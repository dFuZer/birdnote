package com.dfuzer.birdnote.ui.staff

import com.dfuzer.birdnote.domain.Clef
import org.junit.Assert.assertEquals
import org.junit.Test

class ClefGlyphTest {
    @Test
    fun altoAndTenorShareOneClefCenteredOnTheirCLine() {
        val alto = Clef.ALTO.glyphBox()
        val tenor = Clef.TENOR.glyphBox()
        assertEquals(alto.widthInLineSpaces, tenor.widthInLineSpaces)
        assertEquals(alto.heightInLineSpaces, tenor.heightInLineSpaces)
        assertEquals(alto.xInLineSpaces, tenor.xInLineSpaces)
        // Staff lines sit 1 line-space apart. Middle line is 2 above the bottom; the fourth line is 3.
        assertEquals(-2f, alto.topInLineSpaces + alto.heightInLineSpaces / 2f)
        assertEquals(-3f, tenor.topInLineSpaces + tenor.heightInLineSpaces / 2f)
    }
}
