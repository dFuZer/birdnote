package com.dfuzer.birdnote.ui.staff

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
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
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Quiz staff drawn on a [Surface] whose buffers are posted from a render thread.
 *
 * The composable only publishes the latest note list. [android.view.Surface.lockHardwareCanvas]
 * and [android.view.Surface.unlockCanvasAndPost] run on that thread, so a busy main thread does
 * not hold the next staff frame. The surface sits in the card's inner padding, which matches the
 * card corner radius, so the rectangular buffer stays inside the rounded card.
 *
 * The first buffer is the card color, posted before any sprite work. An opaque surface with no
 * buffer is black, and a recycled buffer can still hold the previous exercise.
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
    val session = remember {
        StaffSession(
            context = appContext,
            toChords = toChords,
            clefMode = clefMode,
            difficulty = difficulty,
            visibleCount = visibleCount,
            followTimeMillis = followTimeMillis,
            minSpeedSlotsPerSecond = minSpeedSlotsPerSecond,
        )
    }
    SideEffect {
        session.publish(
            notes = notes,
            clefMode = clefMode,
            difficulty = difficulty,
            visibleCount = visibleCount,
            followTimeMillis = followTimeMillis,
            minSpeedSlotsPerSecond = minSpeedSlotsPerSecond,
        )
    }
    AndroidExternalSurface(
        modifier = modifier,
        isOpaque = true,
        zOrder = AndroidExternalSurfaceZOrder.Behind,
        onInit = {
            onSurface { surface, width, height ->
                fillSurface(surface)
                val thread = HandlerThread("StaffRender")
                thread.start()
                val handler = Handler(thread.looper)
                val renderer = StaffRenderer(session, handler, thread)
                renderer.resize(width, height)
                handler.post { renderer.loop(surface) }
                surface.onChanged { changedWidth, changedHeight ->
                    renderer.resize(changedWidth, changedHeight)
                }
                surface.onDestroyed { renderer.stop() }
                kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                    continuation.invokeOnCancellation { renderer.stop() }
                }
            }
        },
    )
}

private class StaffSession<T>(
    val context: Context,
    val toChords: (List<T>) -> List<StaffChord>,
    clefMode: ClefMode,
    difficulty: Int,
    visibleCount: Int,
    followTimeMillis: Int,
    minSpeedSlotsPerSecond: Float,
) {
    val mailbox = StaffMailbox<T>()

    @Volatile var clefMode: ClefMode = clefMode
    @Volatile var difficulty: Int = difficulty
    @Volatile var visibleCount: Int = visibleCount
    @Volatile var followTimeMillis: Int = followTimeMillis
    @Volatile var minSpeedSlotsPerSecond: Float = minSpeedSlotsPerSecond

    fun publish(
        notes: List<T>,
        clefMode: ClefMode,
        difficulty: Int,
        visibleCount: Int,
        followTimeMillis: Int,
        minSpeedSlotsPerSecond: Float,
    ) {
        this.clefMode = clefMode
        this.difficulty = difficulty
        this.visibleCount = visibleCount
        this.followTimeMillis = followTimeMillis
        this.minSpeedSlotsPerSecond = minSpeedSlotsPerSecond
        mailbox.submit(notes)
    }
}

private class StaffMailbox<T> {
    private val pending = ConcurrentLinkedQueue<List<T>>()

    fun submit(notes: List<T>) {
        pending.add(notes)
    }

    fun drainInto(out: MutableList<List<T>>) {
        out.clear()
        while (true) {
            out.add(pending.poll() ?: return)
        }
    }
}

private class StaffRenderer<T>(
    private val session: StaffSession<T>,
    private val handler: Handler,
    private val thread: HandlerThread,
) {
    private val alive = AtomicBoolean(true)
    private val stopped = AtomicBoolean(false)
    private val width = AtomicInteger(0)
    private val height = AtomicInteger(0)
    private val belt = StaffBelt<T>()
    private val pendingNotes = ArrayList<List<T>>()
    private val sprites = StaffSprites(session.context)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val highlightRect = RectF()
    private val destRect = RectF()

    private var lastFrameNanos = 0L
    private var geometry: StaffGeometry? = null
    private var glyphs: StaffGlyphs? = null
    private var plate: Bitmap? = null
    private var strip: Bitmap? = null
    private var highlights: List<HighlightRect> = emptyList()
    private var builtWidth = 0
    private var builtHeight = 0
    private var builtClef: ClefMode? = null
    private var builtDifficulty = Int.MIN_VALUE
    private var builtVisible = Int.MIN_VALUE
    private var builtNotes: List<T>? = null
    private var released = false

    fun resize(nextWidth: Int, nextHeight: Int) {
        width.set(nextWidth)
        height.set(nextHeight)
    }

    fun loop(surface: Surface) {
        fillSurface(surface)
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
        choreographer.postFrameCallback(callback)
    }

    fun stop() {
        if (!stopped.compareAndSet(false, true)) return
        alive.set(false)
        handler.post { release() }
        thread.quitSafely()
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
        session.mailbox.drainInto(pendingNotes)
        val visibleCount = session.visibleCount
        for (incoming in pendingNotes) {
            belt.offer(incoming, visibleCount, nowSeconds, frameSeconds)
        }
        belt.advance(
            nowSeconds,
            session.followTimeMillis.coerceAtLeast(1) / 1_000f,
            session.minSpeedSlotsPerSecond,
        )
        val surfaceWidth = width.get()
        val surfaceHeight = height.get()
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return
        ensureCaches(surfaceWidth, surfaceHeight)
        val plateBitmap = plate ?: return
        val stripBitmap = strip ?: return
        val staff = geometry ?: return
        val canvas = surface.lockHardwareCanvas()
        try {
            canvas.drawColor(STAFF_BACKGROUND)
            val offset = -slideOffsetSlots(belt.shift, belt.origin) * staff.slotWidth
            val clipLeft = staffClipLeft(staff.clefRight)
            canvas.save()
            canvas.clipRect(clipLeft, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat())
            canvas.translate(offset, 0f)
            fillPaint.color = LIGHT_BLUE
            for (highlight in highlights) {
                highlightRect.set(
                    highlight.topLeft.x,
                    highlight.topLeft.y,
                    highlight.topLeft.x + highlight.size.width,
                    highlight.topLeft.y + highlight.size.height,
                )
                canvas.drawRoundRect(highlightRect, highlight.radius, highlight.radius, fillPaint)
            }
            canvas.restore()
            canvas.drawBitmap(plateBitmap, 0f, 0f, bitmapPaint)
            canvas.save()
            canvas.clipRect(clipLeft, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat())
            canvas.translate(offset, 0f)
            canvas.drawBitmap(stripBitmap, 0f, 0f, bitmapPaint)
            canvas.restore()
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
    }

    private fun ensureCaches(surfaceWidth: Int, surfaceHeight: Int) {
        val clefMode = session.clefMode
        val difficulty = session.difficulty
        val visibleCount = session.visibleCount
        val geometryChanged = geometry == null ||
            builtWidth != surfaceWidth ||
            builtHeight != surfaceHeight ||
            builtClef != clefMode ||
            builtDifficulty != difficulty ||
            builtVisible != visibleCount
        if (geometryChanged) {
            val nextGeometry = staffGeometry(
                size = Size(surfaceWidth.toFloat(), surfaceHeight.toFloat()),
                model = StaffRenderModel(
                    clefMode = clefMode,
                    chords = emptyList(),
                    difficulty = difficulty,
                ),
                visibleSlotCount = visibleCount,
                noteAreaExtraLeftPaddingInLineSpaces = 0f,
                compactVertical = false,
            )
            val nextGlyphs = sprites.rasterize(nextGeometry)
            val nextPlate = drawPlate(nextGeometry, nextGlyphs, surfaceWidth, surfaceHeight)
            geometry = nextGeometry
            recycleGlyphs(glyphs)
            glyphs = nextGlyphs
            plate?.recycle()
            plate = nextPlate
            builtWidth = surfaceWidth
            builtHeight = surfaceHeight
            builtClef = clefMode
            builtDifficulty = difficulty
            builtVisible = visibleCount
            builtNotes = null
        }
        val staff = geometry ?: return
        val drawnGlyphs = glyphs ?: return
        if (builtNotes === belt.notes && strip != null) return
        val chords = session.toChords(belt.notes)
        val chordDraws = cacheChordDraws(
            StaffRenderModel(clefMode, chords, difficulty),
            staff,
        )
        highlights = highlightRects(
            belt.highlightIndex.takeIf { belt.notes.isNotEmpty() },
            chordDraws,
            staff.lineSpacing,
        )
        val nextStrip = drawStrip(
            staff = staff,
            drawnGlyphs = drawnGlyphs,
            chordDraws = chordDraws,
            noteCount = belt.notes.size,
            surfaceWidth = surfaceWidth,
            surfaceHeight = surfaceHeight,
        )
        strip?.recycle()
        strip = nextStrip
        builtNotes = belt.notes
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

    private fun drawStrip(
        staff: StaffGeometry,
        drawnGlyphs: StaffGlyphs,
        chordDraws: List<CachedChordDraw>,
        noteCount: Int,
        surfaceWidth: Int,
        surfaceHeight: Int,
    ): Bitmap {
        val contentWidth = staff.notesStartX + (noteCount + 1) * staff.slotWidth
        val bitmapWidth = ceil(maxOf(surfaceWidth.toFloat(), contentWidth)).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(bitmapWidth, surfaceHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        for (chordDraw in chordDraws) {
            drawChord(canvas, chordDraw, staff.lineSpacing, drawnGlyphs)
        }
        return bitmap
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
            val glyph = when (accidental.accidental) {
                Accidental.SHARP -> AccidentalSprite(
                    bitmap = glyphs.sharp,
                    width = lineSpacing * tuning.sharpWidthInLineSpaces,
                    height = lineSpacing * tuning.sharpHeightInLineSpaces,
                    centerYOffset = 0f,
                )
                Accidental.FLAT -> AccidentalSprite(
                    bitmap = glyphs.flat,
                    width = lineSpacing * tuning.flatWidthInLineSpaces,
                    height = lineSpacing * tuning.flatHeightInLineSpaces,
                    centerYOffset = lineSpacing * tuning.flatCenterYOffsetInLineSpaces,
                )
                Accidental.NATURAL -> AccidentalSprite(
                    bitmap = glyphs.natural,
                    width = lineSpacing * tuning.naturalWidthInLineSpaces,
                    height = lineSpacing * tuning.naturalHeightInLineSpaces,
                    centerYOffset = 0f,
                )
                Accidental.NONE -> continue
            }
            val centerY = yForStep(accidental.step.toFloat(), bottomLineY, lineSpacing) + glyph.centerYOffset
            destRect.set(
                accidental.x - glyph.width / 2f,
                centerY - glyph.height / 2f,
                accidental.x + glyph.width / 2f,
                centerY + glyph.height / 2f,
            )
            canvas.drawBitmap(glyph.bitmap, null, destRect, bitmapPaint)
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
        plate?.recycle()
        strip?.recycle()
        recycleGlyphs(glyphs)
        plate = null
        strip = null
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

private fun fillSurface(surface: Surface) {
    if (!surface.isValid) return
    val canvas = try {
        surface.lockHardwareCanvas()
    } catch (_: RuntimeException) {
        return
    }
    try {
        canvas.drawColor(STAFF_BACKGROUND)
    } finally {
        try {
            surface.unlockCanvasAndPost(canvas)
        } catch (_: RuntimeException) {
            // The surface was destroyed while this buffer was locked.
        }
    }
}

private class AccidentalSprite(
    val bitmap: Bitmap,
    val width: Float,
    val height: Float,
    val centerYOffset: Float,
)

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
            sharp = load(
                tuning.sharpAsset,
                spacing * tuning.sharpWidthInLineSpaces,
                spacing * tuning.sharpHeightInLineSpaces,
            ),
            flat = load(
                tuning.flatAsset,
                spacing * tuning.flatWidthInLineSpaces,
                spacing * tuning.flatHeightInLineSpaces,
            ),
            natural = load(
                tuning.naturalAsset,
                spacing * tuning.naturalWidthInLineSpaces,
                spacing * tuning.naturalHeightInLineSpaces,
            ),
            treble = load(
                "key-sol.svg",
                spacing * tuning.trebleClefWidthInLineSpaces,
                spacing * tuning.trebleClefHeightInLineSpaces,
            ),
            bass = load(
                "key-fa.svg",
                spacing * tuning.bassClefWidthInLineSpaces,
                spacing * tuning.bassClefHeightInLineSpaces,
            ),
            cClef = load(
                "key-ut.svg",
                spacing * tuning.cClefWidthInLineSpaces,
                spacing * tuning.cClefHeightInLineSpaces,
            ),
        )
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
