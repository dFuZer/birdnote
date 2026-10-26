package com.dfuzer.birdnote.ui.staff

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.view.Choreographer
import android.view.Surface
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.AndroidExternalSurfaceZOrder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.svg.SvgDecoder
import coil3.toBitmap
import com.dfuzer.birdnote.domain.Accidental
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.ui.LayoutTuning
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking

/**
 * Quiz staff drawn on a [Surface] whose buffers are posted from a render thread.
 *
 * The composable publishes ordered note and configuration updates. The render thread draws them
 * with its own GL context ([StaffGl]), so neither a busy main thread nor HWUI's RenderThread, which
 * Compose frames hold, delays the next staff frame. The surface sits in the card's inner padding,
 * which matches the card corner radius, so the rectangular buffer stays inside the rounded card.
 *
 * The plate (staff lines and clefs) and each distinct chord are painted on the CPU once and
 * uploaded as textures. An answer paints only the chord that enters the belt, and a sliding frame
 * is a few GPU quads. The first
 * buffer is the card color, posted before any sprite work. An opaque surface with no buffer is
 * black, and a recycled buffer can still hold the previous exercise.
 */
@Composable
fun <T> StaffSurface(
    notes: List<T>,
    toChords: (List<T>) -> List<StaffChord>,
    clefMode: ClefMode,
    difficulty: Int,
    visibleCount: Int,
    followTimeMillis: Int,
    minSpeedSlotsPerSecond: Float,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    val mailbox = remember { StaffMailbox<T>() }
    SideEffect {
        mailbox.submit(
            StaffUpdate(
                notes = notes,
                config = StaffRenderConfig(
                    clefMode, difficulty, visibleCount, followTimeMillis, minSpeedSlotsPerSecond,
                ),
            ),
        )
    }
    AndroidExternalSurface(
        modifier = modifier,
        isOpaque = true,
        zOrder = AndroidExternalSurfaceZOrder.Behind,
        onInit = {
            onSurface { surface, width, height ->
                // The priority HWUI gives its RenderThread, so a busy core does not delay a frame.
                val thread = HandlerThread("StaffRender", Process.THREAD_PRIORITY_URGENT_DISPLAY)
                thread.start()
                val handler = Handler(thread.looper)
                val renderer = StaffRenderer(appContext, surface, mailbox, toChords, handler, thread)
                renderer.resize(width, height)
                handler.post { renderer.loop() }
                surface.onChanged { changedWidth, changedHeight ->
                    renderer.resize(changedWidth, changedHeight)
                }
                // SurfaceHolder.Callback.surfaceDestroyed contract: nothing may touch the surface
                // after this callback returns, because the SurfaceView then disconnects its buffer
                // queue. stop() blocks until the render thread has destroyed its GL surface.
                surface.onDestroyed { renderer.stop() }
                kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                    continuation.invokeOnCancellation { renderer.stop() }
                }
            }
        },
    )
}

private class StaffRenderer<T>(
    context: Context,
    private val surface: Surface,
    private val mailbox: StaffMailbox<T>,
    private val toChords: (List<T>) -> List<StaffChord>,
    private val handler: Handler,
    private val thread: HandlerThread,
) {
    private val alive = AtomicBoolean(true)
    private val stopped = CountDownLatch(1)
    private var frameCallback: Choreographer.FrameCallback? = null
    @Volatile private var size = IntSize.Zero
    private val belt = StaffBelt<T>()
    private val pendingUpdates = ArrayList<StaffUpdate<T>>()
    private var config: StaffRenderConfig? = null
    private val sprites = StaffSprites(context)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val destRect = RectF()

    private var gl: StaffGl? = null
    private var lastFrameNanos = 0L
    private var geometry: StaffGeometry? = null
    private var glyphs: StaffGlyphs? = null
    private var plate: StaffTexture? = null
    private val chordSprites = HashMap<StaffChord, ChordSprite>()
    private var placements: List<ChordPlacement> = emptyList()
    private var highlights: List<HighlightRect> = emptyList()
    private var geometryKey: StaffGeometryKey? = null
    private var contentKey: StaffContentKey<T>? = null
    private var released = false
    private var drawnSize = IntSize.Zero

    fun resize(nextWidth: Int, nextHeight: Int) {
        size = IntSize(nextWidth, nextHeight)
    }

    fun loop() {
        if (!alive.get()) return
        val output = StaffGl.create(surface) ?: return
        gl = output
        output.begin(size.width, size.height, STAFF_BACKGROUND)
        output.swap()
        val choreographer = Choreographer.getInstance()
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!alive.get()) return
                try {
                    draw(surface, frameTimeNanos)
                } catch (_: RuntimeException) {
                    if (!surface.isValid || !alive.get()) return
                }
                if (alive.get()) choreographer.postFrameCallback(this)
            }
        }
        frameCallback = callback
        choreographer.postFrameCallback(callback)
    }

    /**
     * Stops rendering and returns only when the render thread can no longer touch the surface.
     * Called on the main thread from `surfaceDestroyed` and from cancellation.
     *
     * If the SurfaceView disconnects its BLAST queue while a producer still holds a dequeued
     * buffer, tearing that producer down later crashes (the SIGSEGV in `Surface::hook_cancelBuffer`
     * seen with HWUI). So the stop message, which the handler runs only after the current frame and
     * its swap, destroys the GL surface and context on the render thread while the queue is still
     * connected, then releases the [Surface]. Bitmaps are recycled after that.
     */
    fun stop() {
        if (alive.compareAndSet(true, false)) {
            val posted = handler.post {
                try {
                    frameCallback?.let(Choreographer.getInstance()::removeFrameCallback)
                    gl?.release()
                    gl = null
                    surface.release()
                    release()
                } finally {
                    thread.quit()
                    stopped.countDown()
                }
            }
            if (!posted) {
                surface.release()
                stopped.countDown()
            }
        }
        awaitStopped()
    }

    private fun awaitStopped() {
        var interrupted = false
        while (true) {
            try {
                stopped.await()
                break
            } catch (_: InterruptedException) {
                interrupted = true
            }
        }
        if (interrupted) Thread.currentThread().interrupt()
    }

    private fun draw(surface: Surface, frameTimeNanos: Long) {
        if (!alive.get() || !surface.isValid) return
        val frameSeconds = if (lastFrameNanos == 0L) {
            0.0
        } else {
            (frameTimeNanos - lastFrameNanos).coerceIn(0L, MAX_FIRST_STEP_NANOS) / NANOS_PER_SECOND
        }
        val nowSeconds = frameTimeNanos / NANOS_PER_SECOND
        lastFrameNanos = frameTimeNanos
        mailbox.drainInto(pendingUpdates)
        if (config == null && pendingUpdates.isEmpty()) mailbox.latest?.let(pendingUpdates::add)
        for (incoming in pendingUpdates) {
            config = incoming.config
            belt.offer(incoming.notes, incoming.config.visibleCount, nowSeconds, frameSeconds)
        }
        val config = config ?: return
        // An idle belt shows the same frame, which the surface keeps without a new buffer.
        val needsFrame = pendingUpdates.isNotEmpty() || belt.isMoving || size != drawnSize
        belt.advance(
            nowSeconds,
            config.followTimeMillis.coerceAtLeast(1) / 1_000f,
            config.minSpeedSlotsPerSecond,
        )
        val surfaceSize = size
        val surfaceWidth = surfaceSize.width
        val surfaceHeight = surfaceSize.height
        if (surfaceWidth <= 0 || surfaceHeight <= 0 || !needsFrame) return
        val output = gl ?: return
        ensureCaches(output, surfaceWidth, surfaceHeight, config)
        val staff = geometry ?: return
        val plateTexture = plate ?: return
        val offset = -slideOffsetSlots(belt.shift, belt.origin) * staff.slotWidth
        val clipLeft = staffClipLeft(staff.clefRight).toInt()
        output.begin(surfaceWidth, surfaceHeight, STAFF_BACKGROUND)
        output.clipLeft(clipLeft)
        for (highlight in highlights) {
            output.drawRoundRect(
                highlight.topLeft.x + offset,
                highlight.topLeft.y,
                highlight.size.width,
                highlight.size.height,
                highlight.radius,
                LIGHT_BLUE,
            )
        }
        output.clipLeft(null)
        output.drawTexture(plateTexture, 0f, 0f)
        output.clipLeft(clipLeft)
        for (placement in placements) {
            val sprite = placement.sprite
            output.drawTexture(sprite.texture, placement.centerX + sprite.leftFromCenter + offset, sprite.top)
        }
        if (output.swap()) drawnSize = surfaceSize
    }

    private fun ensureCaches(output: StaffGl, surfaceWidth: Int, surfaceHeight: Int, config: StaffRenderConfig) {
        val nextGeometryKey = StaffGeometryKey(
            surfaceWidth, surfaceHeight, config.clefMode, config.difficulty, config.visibleCount,
        )
        if (geometryKey != nextGeometryKey) {
            val nextGeometry = staffGeometry(
                size = Size(surfaceWidth.toFloat(), surfaceHeight.toFloat()),
                model = StaffRenderModel(
                    clefMode = config.clefMode,
                    chords = emptyList(),
                    difficulty = config.difficulty,
                ),
                visibleSlotCount = config.visibleCount,
                noteAreaExtraLeftPaddingInLineSpaces = 0f,
                compactVertical = false,
            )
            val nextGlyphs = sprites.rasterize(nextGeometry)
            val plateBitmap = drawPlate(nextGeometry, nextGlyphs, surfaceWidth, surfaceHeight)
            geometry = nextGeometry
            recycleGlyphs(glyphs)
            glyphs = nextGlyphs
            plate?.let(output::deleteTexture)
            plate = output.createTexture(plateBitmap)
            plateBitmap.recycle()
            chordSprites.values.forEach { output.deleteTexture(it.texture) }
            chordSprites.clear()
            placements = emptyList()
            geometryKey = nextGeometryKey
            contentKey = null
        }
        val staff = geometry ?: return
        val drawnGlyphs = glyphs ?: return
        val nextContentKey = StaffContentKey(belt.notes, belt.highlightIndex)
        if (contentKey == nextContentKey) return
        val chords = toChords(belt.notes)
        val chordDraws = cacheChordDraws(
            StaffRenderModel(config.clefMode, chords, config.difficulty),
            staff,
        )
        highlights = highlightRects(
            belt.highlightIndex.takeIf { belt.notes.isNotEmpty() },
            chordDraws,
            staff.lineSpacing,
        )
        val drawsByIndex = chordDraws.groupBy { it.index }
        placements = chords.mapIndexedNotNull { index, chord ->
            val draws = drawsByIndex[index] ?: return@mapIndexedNotNull null
            val centerX = staff.slotCenterX(index)
            val sprite = chordSprites.getOrPut(chord) {
                paintChordSprite(output, staff, drawnGlyphs, draws, centerX, surfaceHeight)
            }
            ChordPlacement(centerX, sprite)
        }
        val shown = chords.toHashSet()
        chordSprites.entries.removeAll { (chord, sprite) ->
            (chord !in shown).also { gone -> if (gone) output.deleteTexture(sprite.texture) }
        }
        contentKey = nextContentKey
    }

    private fun drawPlate(
        staff: StaffGeometry,
        drawnGlyphs: StaffGlyphs,
        surfaceWidth: Int,
        surfaceHeight: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(surfaceWidth, surfaceHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        staff.clefs.forEachIndexed { index, clef ->
            val bottomLineY = staff.bottomLineYs[index]
            drawStaffLines(
                canvas = canvas,
                left = staff.paddingX,
                right = surfaceWidth.toFloat(),
                bottomLineY = bottomLineY,
                lineSpacing = staff.lineSpacing,
                strokeWidth = staff.staffLineWidth,
            )
            drawClef(
                canvas = canvas,
                clef = clef,
                left = staff.paddingX,
                bottomLineY = bottomLineY,
                lineSpacing = staff.lineSpacing,
                glyphs = drawnGlyphs,
            )
        }
        return bitmap
    }

    /**
     * Paints one chord, all of its staves, into a texture cropped to its bounds. A chord's layout
     * only moves with its slot center, so the sprite is reused wherever the chord sits on the belt.
     */
    private fun paintChordSprite(
        output: StaffGl,
        staff: StaffGeometry,
        drawnGlyphs: StaffGlyphs,
        draws: List<CachedChordDraw>,
        centerX: Float,
        surfaceHeight: Int,
    ): ChordSprite {
        val margin = staff.lineSpacing
        val left = floor(draws.minOf { it.layout.bounds.left } - margin)
        val right = ceil(draws.maxOf { it.layout.bounds.right } + margin)
        val top = floor(draws.minOf { it.layout.bounds.top } - margin).coerceAtLeast(0f)
        val bottom = ceil(draws.maxOf { it.layout.bounds.bottom } + margin).coerceAtMost(surfaceHeight.toFloat())
        val bitmap = Bitmap.createBitmap(
            (right - left).toInt().coerceAtLeast(1),
            (bottom - top).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        canvas.translate(-left, -top)
        for (chordDraw in draws) {
            drawChord(canvas, chordDraw, staff.lineSpacing, drawnGlyphs)
        }
        val texture = output.createTexture(bitmap)
        bitmap.recycle()
        return ChordSprite(texture, left - centerX, top)
    }

    private fun drawStaffLines(
        canvas: Canvas,
        left: Float,
        right: Float,
        bottomLineY: Float,
        lineSpacing: Float,
        strokeWidth: Float,
    ) {
        strokePaint.color = BLACK
        strokePaint.strokeWidth = strokeWidth
        strokePaint.strokeCap = Paint.Cap.SQUARE
        repeat(5) { line ->
            val y = bottomLineY - line * lineSpacing
            canvas.drawLine(left, y, right, y, strokePaint)
        }
    }

    private fun drawClef(
        canvas: Canvas,
        clef: Clef,
        left: Float,
        bottomLineY: Float,
        lineSpacing: Float,
        glyphs: StaffGlyphs,
    ) {
        val box = clef.glyphBox()
        val x = left + lineSpacing * box.xInLineSpaces
        val top = bottomLineY + lineSpacing * box.topInLineSpaces
        val glyphWidth = lineSpacing * box.widthInLineSpaces
        val glyphHeight = lineSpacing * box.heightInLineSpaces
        val bitmap = glyphs.bitmapFor(clef)
        destRect.set(x, top, x + glyphWidth, top + glyphHeight)
        canvas.drawBitmap(bitmap, null, destRect, bitmapPaint)
    }

    private fun drawChord(
        canvas: Canvas,
        chordDraw: CachedChordDraw,
        lineSpacing: Float,
        glyphs: StaffGlyphs,
    ) {
        val layout = chordDraw.layout
        val bottomLineY = chordDraw.bottomLineY
        val tuning = LayoutTuning.Staff
        if (layout.heads.isNotEmpty()) {
            val ledgerHalf = lineSpacing * tuning.ledgerHalfWidthInLineSpaces
            val ledgerLeft = layout.heads.minOf { it.x } - ledgerHalf
            val ledgerRight = layout.heads.maxOf { it.x } + ledgerHalf
            strokePaint.color = BLACK
            strokePaint.strokeCap = Paint.Cap.BUTT
            strokePaint.strokeWidth = lineSpacing * tuning.ledgerLineWidthInLineSpaces
            for (ledgerStep in layout.ledgerSteps) {
                val y = yForStep(ledgerStep.toFloat(), bottomLineY, lineSpacing)
                canvas.drawLine(ledgerLeft, y, ledgerRight, y, strokePaint)
            }
        }
        if (layout.useCompleteNote) {
            val head = layout.heads.firstOrNull() ?: return
            drawCompleteNote(
                canvas = canvas,
                x = head.x,
                step = head.step,
                stemDown = head.stemDown,
                bottomLineY = bottomLineY,
                lineSpacing = lineSpacing,
                note = glyphs.note,
            )
        } else {
            val stem = layout.stem
            val headWidth = lineSpacing * tuning.noteWidthInLineSpaces
            if (stem != null) {
                strokePaint.color = BLACK
                strokePaint.strokeCap = Paint.Cap.BUTT
                strokePaint.strokeWidth = headWidth * tuning.stemThicknessInNoteWidths
                canvas.drawLine(stem.x, stem.startY, stem.x, stem.endY, strokePaint)
            }
            fillPaint.color = BLACK
            for (head in layout.heads) {
                drawNoteHeadOval(
                    canvas = canvas,
                    x = head.x,
                    y = yForStep(head.step.toFloat(), bottomLineY, lineSpacing),
                    width = headWidth,
                )
            }
        }
        for (accidental in layout.accidentals) {
            val glyph = accidental.accidental.glyph() ?: continue
            val glyphWidth = lineSpacing * glyph.widthInLineSpaces
            val glyphHeight = lineSpacing * glyph.heightInLineSpaces
            val centerY = yForStep(accidental.step.toFloat(), bottomLineY, lineSpacing) +
                lineSpacing * glyph.centerYOffsetInLineSpaces
            destRect.set(
                accidental.x - glyphWidth / 2f,
                centerY - glyphHeight / 2f,
                accidental.x + glyphWidth / 2f,
                centerY + glyphHeight / 2f,
            )
            canvas.drawBitmap(glyphs.bitmapFor(accidental.accidental), null, destRect, bitmapPaint)
        }
    }

    private fun drawCompleteNote(
        canvas: Canvas,
        x: Float,
        step: Int,
        stemDown: Boolean,
        bottomLineY: Float,
        lineSpacing: Float,
        note: Bitmap,
    ) {
        val tuning = LayoutTuning.Staff
        val y = yForStep(step.toFloat(), bottomLineY, lineSpacing)
        val noteWidth = lineSpacing * tuning.noteWidthInLineSpaces
        val noteHeight = lineSpacing * tuning.noteHeightInLineSpaces
        val noteLeft = x - noteWidth * tuning.noteHeadCenterXFraction
        val noteTop = y - noteHeight * tuning.noteHeadCenterYFraction
        canvas.save()
        if (stemDown) canvas.rotate(180f, x, y)
        destRect.set(noteLeft, noteTop, noteLeft + noteWidth, noteTop + noteHeight)
        canvas.drawBitmap(note, null, destRect, bitmapPaint)
        canvas.restore()
    }

    private fun drawNoteHeadOval(canvas: Canvas, x: Float, y: Float, width: Float) {
        val tuning = LayoutTuning.Staff
        val rx = width * tuning.noteHeadEllipseRx / tuning.noteHeadSvgWidth
        val ry = width * tuning.noteHeadEllipseRy / tuning.noteHeadSvgWidth
        canvas.save()
        canvas.rotate(tuning.noteHeadRotationDegrees, x, y)
        destRect.set(x - rx, y - ry, x + rx, y + ry)
        canvas.drawOval(destRect, fillPaint)
        canvas.restore()
    }

    private fun release() {
        if (released) return
        released = true
        alive.set(false)
        // GL textures went with the context in StaffGl.release().
        recycleGlyphs(glyphs)
        plate = null
        chordSprites.clear()
        placements = emptyList()
        glyphs = null
    }

    private fun recycleGlyphs(glyphs: StaffGlyphs?) {
        if (glyphs == null) return
        glyphs.note.recycle()
        glyphs.sharp.recycle()
        glyphs.flat.recycle()
        glyphs.natural.recycle()
        glyphs.treble.recycle()
        glyphs.bass.recycle()
        glyphs.cClef.recycle()
    }

    private companion object {
        const val LIGHT_BLUE = 0xFF9FDEFD.toInt()
        const val BLACK = 0xFF000000.toInt()
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val MAX_FIRST_STEP_NANOS = 50_000_000L
    }
}

private const val STAFF_BACKGROUND = 0xFFF6F6F6.toInt()

private class ChordSprite(val texture: StaffTexture, val leftFromCenter: Float, val top: Float)

private class ChordPlacement(val centerX: Float, val sprite: ChordSprite)

private class StaffGlyphs(
    val note: Bitmap,
    val sharp: Bitmap,
    val flat: Bitmap,
    val natural: Bitmap,
    val treble: Bitmap,
    val bass: Bitmap,
    val cClef: Bitmap,
) {
    fun bitmapFor(clef: Clef): Bitmap = when (clef) {
        Clef.SOL -> treble
        Clef.FA -> bass
        Clef.ALTO, Clef.TENOR -> cClef
    }

    fun bitmapFor(accidental: Accidental): Bitmap = when (accidental) {
        Accidental.SHARP -> sharp
        Accidental.FLAT -> flat
        Accidental.NATURAL -> natural
        Accidental.NONE -> error("No glyph for an unaltered note")
    }
}

private class StaffSprites(context: Context) {
    private val loader = ImageLoader.Builder(context)
        .components { add(SvgDecoder.Factory()) }
        .allowHardware(false)
        .bitmapConfig(Bitmap.Config.ARGB_8888)
        .build()
    private val appContext = context

    fun rasterize(geometry: StaffGeometry): StaffGlyphs {
        val spacing = geometry.lineSpacing
        val tuning = LayoutTuning.Staff
        return StaffGlyphs(
            note = load(
                "note.svg",
                spacing * tuning.noteWidthInLineSpaces,
                spacing * tuning.noteHeightInLineSpaces,
            ),
            sharp = loadAccidental(Accidental.SHARP, spacing),
            flat = loadAccidental(Accidental.FLAT, spacing),
            natural = loadAccidental(Accidental.NATURAL, spacing),
            treble = loadClef(Clef.SOL, spacing),
            bass = loadClef(Clef.FA, spacing),
            cClef = loadClef(Clef.ALTO, spacing),
        )
    }

    private fun loadAccidental(accidental: Accidental, spacing: Float): Bitmap {
        val glyph = checkNotNull(accidental.glyph())
        return load(glyph.asset, spacing * glyph.widthInLineSpaces, spacing * glyph.heightInLineSpaces)
    }

    private fun loadClef(clef: Clef, spacing: Float): Bitmap {
        val box = clef.glyphBox()
        return load(clef.assetName(), spacing * box.widthInLineSpaces, spacing * box.heightInLineSpaces)
    }

    private fun load(assetName: String, widthPx: Float, heightPx: Float): Bitmap {
        val width = widthPx.roundToInt().coerceAtLeast(1)
        val height = heightPx.roundToInt().coerceAtLeast(1)
        val request = ImageRequest.Builder(appContext)
            .data("file:///android_asset/$assetName")
            .size(width, height)
            .allowHardware(false)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .build()
        val result = runBlocking { loader.execute(request) }
        val owned = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val decoded = (result as? SuccessResult)?.image?.toBitmap(width, height) ?: return owned
        Canvas(owned).drawBitmap(
            decoded,
            null,
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            null,
        )
        return owned
    }
}
