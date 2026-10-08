package com.dfuzer.birdnote

import androidx.test.platform.app.InstrumentationRegistry
import com.dfuzer.birdnote.audio.decodeVorbisPcm
import com.dfuzer.birdnote.domain.Pitch
import com.dfuzer.birdnote.domain.pianoAssetPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioDecoderTest {
    @Test fun platformDecoderReadsEveryShippedPianoSample() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val pianoFiles = assets.list("notes")!!.filter { it.endsWith(".ogg") }
        assertEquals("Expected C2–C6 natural-note pack", 29, pianoFiles.size)
        val totalBytes = pianoFiles.sumOf { name ->
            assets.openFd("notes/$name").use { it.length }
        }
        assertTrue("Piano pack exceeds 1 MB: $totalBytes bytes", totalBytes <= 1_000_000)
        for (step in 14..42) {
            val path = Pitch(step).pianoAssetPath()
            val pcm = assets.openFd(path).use(::decodeVorbisPcm)
            assertEquals(path, 44_100, pcm.sampleRate)
            assertTrue(path, pcm.samples.size > 44_100 / 4)
            assertTrue(path, pcm.samples.any { it != 0.toShort() })
        }
    }
}
