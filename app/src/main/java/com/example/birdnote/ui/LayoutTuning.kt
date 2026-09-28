package com.example.birdnote.ui

import androidx.compose.ui.unit.dp

/**
 * Visual tuning values for matching the Figma mockups.
 *
 * Fractions are relative to the available screen/canvas dimension. Values named
 * `...InLineSpaces` are relative to the distance between two staff lines.
 */
object LayoutTuning {
    object Background {
        const val pianoHomeSize = 0.50f
        const val pianoStandardSize = 0.42f
        const val pianoHomeX = 0.0f
        const val pianoStandardX = -0.28f
        const val pianoY = -0.34f

        const val scoreHomeWidth = 0.35f
        const val scoreStandardWidth = 0.30f
        const val scoreHomeHeight = 0.72f
        const val scoreStandardHeight = 0.62f
        const val scoreHomeX = 0.0f
        const val scoreStandardX = 0.28f

        const val noteHomeSize = 0.36f
        const val noteStandardSize = 0.30f
        const val noteHomeX = 0.12f
        const val noteStandardX = -0.20f
        const val noteY = 0.12f
    }

    object Common {
        val screenHorizontalPadding = 24.dp
        val screenVerticalPadding = 12.dp
        val backButtonMargin = 12.dp
        val backButtonSize = 48.dp
        val backArrowSize = 24.dp
        val backArrowStrokeWidth = 2.dp
        const val backArrowTipXFraction = 0.18f
        const val backArrowTailXFraction = 0.82f
        const val backArrowWingXFraction = 0.42f
        const val backArrowWingOffsetFraction = 0.24f
        val buttonMinWidth = 220.dp
        val buttonMaxWidth = 300.dp
        val buttonHeight = 48.dp
        val buttonCornerRadius = 5.dp
        val buttonElevation = 2.dp
        val buttonHorizontalPadding = 16.dp
        val buttonVerticalPadding = 3.dp
        val menuButtonGap = 7.dp
        val titleBottomSpacing = 20.dp
    }

    object Setup {
        val contentStartPadding = 64.dp
        val contentEndPadding = 40.dp
        val contentTopPadding = 58.dp
        val contentBottomPadding = 68.dp
        val columnsGap = 28.dp
        const val controlsWeight = 0.40f
        const val previewWeight = 0.60f
        val controlsMaxWidth = 330.dp
        val controlsGap = 10.dp
        val difficultyGap = 8.dp
        val difficultyButtonWidth = 52.dp
        val difficultyButtonHeight = 42.dp
        val clefButtonMinWidth = 190.dp
        val clefButtonMaxWidth = 230.dp
        val previewCornerRadius = 8.dp
        val previewInnerPadding = 10.dp
        const val previewNoteAreaExtraLeftPaddingInLineSpaces = 1.0f
        const val chordPreviewMaxRows = 2
        val chordPreviewLabelGap = 2.dp
        val startButtonBottomMargin = 12.dp
    }

    object Quiz {
        val horizontalPadding = 12.dp
        val verticalPadding = 8.dp
        val stopButtonHeight = 38.dp
        val stopButtonHorizontalPadding = 16.dp
        val progressBarGap = 16.dp
        val progressBarHeight = 10.dp
        val staffCornerRadius = 8.dp
        val staffInnerPadding = 8.dp
        val staffVerticalGap = 8.dp
        val answerHorizontalMargin = 2.dp
        val answerContentHorizontalPadding = 2.dp
        val answerButtonHeight = 48.dp
        val answerRowGap = 6.dp
        const val intervalAnswerColumns = 4
        const val chordAnswerMaxRows = 2
        const val answerDisabledAlpha = 0.55f
        const val noteFollowTimeMillis = 1500
        const val noteFollowMinSpeedSlotsPerSecond = 0.05f
    }

    object Result {
        val contentPadding = 20.dp
        val scoreBottomSpacing = 22.dp
        val buttonGap = 7.dp
    }

    object Staff {
        // Canvas painters do not expose a measured output size to Coil.
        const val canvasSvgRasterScale = 2

        const val horizontalPadding = 0.1f
        const val verticalPadding = 0.08f
        const val doubleStaffGap = 0.08f
        const val doubleStaffScale = 0.60f
        const val doubleStaffDifficulty4Scale = 0.50f
        const val singleStaffToDoubleRatio = 1.20f
        const val clefReservedWidthInLineSpaces = 2.8f
        const val minimumNoteSlots = 1

        const val staffLineWidthPx = 2.0f
        const val singleStaffLineWidthPx = 4.0f
        const val rangeCornerRadiusInLineSpaces = 0.25f
        const val currentNoteHighlightPaddingInLineSpaces = 0.40f
        const val currentNoteHighlightCornerRadiusInLineSpaces = 0.30f
        const val ledgerHalfWidthInLineSpaces = 0.90f * 1.05f
        const val ledgerLineWidthInLineSpaces = 0.08f*1.05f
        const val noteWidthInLineSpaces = 1.15f * 1.05f
        const val noteHeightInLineSpaces = noteWidthInLineSpaces * 3.2f
        const val noteHeadCenterXFraction = 0.50f
        const val noteHeadCenterYFraction = 0.862f
        const val stemDownFromStaffStep = 4

        const val noteHeadSvgWidth = 8
        const val noteHeadSvgHeight = 7
        const val noteHeadEllipseRx = 4.35f
        const val noteHeadEllipseRy = 2.9f
        const val noteHeadRotationDegrees = -33.33f
        const val stemThicknessInNoteWidths = 2.25f / 20f
        const val compactVerticalPadding = 0.03f
        const val compactLedgerStepsAbove = 3
        const val compactLedgerStepsBelow = 3

        const val sharpAsset = "diese.svg"
        const val flatAsset = "bemol.svg"
        const val naturalAsset = "becarre.svg"
        const val accidentalRasterWidth = 7
        const val accidentalRasterHeight = 19
        const val sharpWidthInLineSpaces = 0.82f
        const val sharpHeightInLineSpaces = 2.25f
        const val flatWidthInLineSpaces = 0.70f
        const val flatHeightInLineSpaces = 1.95f
        const val flatCenterYOffsetInLineSpaces = -0.38f
        const val naturalWidthInLineSpaces = 0.62f
        const val naturalHeightInLineSpaces = 2.60f
        const val accidentalMaxWidthInLineSpaces = sharpWidthInLineSpaces
        const val accidentalMaxHeightInLineSpaces = naturalHeightInLineSpaces
        const val accidentalColumnGapInLineSpaces = 0.12f
        const val accidentalToHeadGapInLineSpaces = 0.18f
        const val accidentalSameColumnMinSteps = 5
        const val accidentalFlatExtraSteps = 1

        // Horizontal smear while a belt is sliding. Sigma is this fraction of the
        // distance traveled in the last frame; slower motion stays sharp.
        const val motionBlurMinTravelPx = 1f
        const val motionBlurSigmaPerTravelPx = 0.625f
        const val motionBlurPadSlots = 1.5f
        const val motionBlurTailSigmas = 3f

        // SVG clef bounds, relative to one staff-line spacing.
        const val trebleClefXInLineSpaces = 0.15f
        const val trebleClefTopInLineSpaces = -5.5f
        const val trebleClefWidthInLineSpaces = 2.52f
        const val trebleClefHeightInLineSpaces = 7.20f

        const val bassClefXInLineSpaces = 0.10f
        const val bassClefTopInLineSpaces = -4.08f
        const val bassClefWidthInLineSpaces = 2.60f*1.15f
        const val bassClefHeightInLineSpaces = 3.00f*1.15f
    }
}
