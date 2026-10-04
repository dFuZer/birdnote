package com.dfuzer.birdnote.domain

import kotlin.random.Random
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Test

class SeededPracticeTest {
    // Captured from the original source at 5784988, seed 582, initial queue plus 100 advances.
    @Test
    fun everyModeDifficultyAndClefRetainsItsOriginalRandomSequence() {
        for (difficulty in 1..4) {
            for (clef in ClefMode.entries) {
                val notes = PracticeConfig(difficulty, clef)
                val nr = Random(582)
                assertEquals(expected.getValue("NOTES_${difficulty}_$clef"), fingerprint(generateQueue(notes, random = nr)) { advanceQueue(it, notes, nr) })
                val chords = ChordConfig(difficulty, clef)
                val cr = Random(582)
                assertEquals(expected.getValue("CHORDS_${difficulty}_$clef"), fingerprint(generateChordQueue(chords, random = cr)) { advanceChordQueue(it, chords, cr) })
            }
            val intervals = IntervalConfig(difficulty)
            val ir = Random(582)
            assertEquals(expected.getValue("INTERVALS_$difficulty"), fingerprint(generateIntervalQueue(intervals, random = ir)) { advanceIntervalQueue(it, intervals, ir) })
        }
    }

    companion object {
        private val expected = mapOf(
            "NOTES_1_SOL" to "f873470ea5dc4400a685dbc9a0aa1fa07de1c63bd1d4a29d9f0844fc9fc9ec1e",
            "CHORDS_1_SOL" to "cc01a55f14369fabd9d6a0dd79c8dbb737c6ce8853a6041114bdc670c28fa2e4",
            "NOTES_1_FA" to "89fa36c1c5787ebea54450072c23ed09977a06eb16ff0ff15c966382bb1bf34e",
            "CHORDS_1_FA" to "7faeb10ddd5f77227f225f25227e6c2afe46e170b9e8d03dd1b060b5c3916d0b",
            "NOTES_1_SOL_FA" to "bc481f936dea8d1adeb706e6607b61d170010c14982ee2794d6e81bffc59c8e5",
            "CHORDS_1_SOL_FA" to "7b31e15c23cd70249fc479da3144d05c22506e2057144838ba7935aa83f35562",
            "NOTES_1_ALTO" to "2a099d3692a7ce752a9753abccca7f9ef91bf251999e892b7e1b8685865bd069",
            "CHORDS_1_ALTO" to "b084eb5db5092da694177750121078fad663473f841b22392ac22e48e27b3a29",
            "NOTES_1_TENOR" to "00ad3abb94c6d17229bf0cda5ac6c112b95494bb339ae0d0511832b6a14e266c",
            "CHORDS_1_TENOR" to "77dc9da11b0387c509405a8328cab90940228ee5bef9df0ef89ff68e66e736bb",
            "INTERVALS_1" to "e09adf03bf2378d1c167f1d7b80d660886d1d0039b6fd701695c5a7fbf61fde0",
            "NOTES_2_SOL" to "f74794ae4958c0c979394b894d77cf9a6c4b574d67e07ccdc971aef61d6a2a78",
            "CHORDS_2_SOL" to "c2c69127f76a861b29aa9e0250103a3593b308b824e639ba1b9a9a290219af70",
            "NOTES_2_FA" to "28937f53d0124c04a858a52997dab33542c4adcd766b88dda4f736f3e9ad9382",
            "CHORDS_2_FA" to "4f435fda85c97428f0c0db5c4d8f8c16634a6b988bad5f5780880fc77a36d0a0",
            "NOTES_2_SOL_FA" to "98d2e64b55813350cbeaacf439ce774e2600971ac94a6ac675d05d095cb2b071",
            "CHORDS_2_SOL_FA" to "62c825cee8b1337b6d0b26065d319ec9ffcb746a156eb99a1e7ceec2a561673c",
            "NOTES_2_ALTO" to "855f9e3f40138a217537c93f9f8288ba48a28d3a88ecd17fa4679717241f063d",
            "CHORDS_2_ALTO" to "616111fe8322cb613a4164ab250497774b9095d11c2be443615113289d0c05a4",
            "NOTES_2_TENOR" to "f77c964e39b5b6d9d9d4d8c3ed8ee38e174f7ff641d381b946a84c7b3379e26d",
            "CHORDS_2_TENOR" to "dac32d6de539276fa7a27f0af16b9f00636e07af3a01d484f4c6559d27c95ee1",
            "INTERVALS_2" to "86d2a1e24a4c4a37be383a9978d2e6cf73e7b92d45808dff6931d6c6292d3bdc",
            "NOTES_3_SOL" to "443f988da3fed42d4a99b2be8891e291abee456ce299cd7ca9e44ef2b22b10e4",
            "CHORDS_3_SOL" to "10ac291d7788b9cab00b4b75731460444bcd6716f6d954978f62b329f225c9ba",
            "NOTES_3_FA" to "1398c4103f5557a620115c53791f99df3930497fc9254ab03cca6ba9d2b80601",
            "CHORDS_3_FA" to "7e46fc2dbf401abb6ccafd3fe91e7af2221858f072bff52687dd0117e45dd6a8",
            "NOTES_3_SOL_FA" to "528099b23c66a1075292fba7d2783f29f49dca31d64bf364b13e1d3fc9b67d4d",
            "CHORDS_3_SOL_FA" to "132838cf68be8dffd5ea326bb88260f74280537dfaa0412dbae488173c9b9fdb",
            "NOTES_3_ALTO" to "454473d2f74ae2d357ea117e3df13dd9f003c9b8a126019fba757dea5106b231",
            "CHORDS_3_ALTO" to "94bab090d54484e5a6ddc24c8bbb248ef497979bb112ae9663cbe642af1b916b",
            "NOTES_3_TENOR" to "aec6ce5e03f0b67cb18007d7f24b1e161a953af941322eb7e2690228a6bd310f",
            "CHORDS_3_TENOR" to "a7af2ed32a55608a32c296afd542822a34f3dd0837d93949abcad0b608165d6b",
            "INTERVALS_3" to "244de0b8e10629e6642102b79a8da6f4520d025cfd26c1f281010ee1044efa7c",
            "NOTES_4_SOL" to "594f8112f8e53ea45ad3642d3efac3e4d3933c53cb1dc499ecbd21f5eb31d9a4",
            "CHORDS_4_SOL" to "bb840c887d15062306c8955b0fbbbb9505124b037955b65589b4b348fb6943e6",
            "NOTES_4_FA" to "0becb66271e180c7e486b5eb814ae179c012aa95cd50c76ae1704592f7e9e487",
            "CHORDS_4_FA" to "79a44056a15b7e2281c043a62fcfcd1e074037cc72e6a7803ccd81e8496a5c5b",
            "NOTES_4_SOL_FA" to "e564d1f4061e393ee2cdfb64673220c4baf5d1d6ed460fe08c575f36c36c7c0a",
            "CHORDS_4_SOL_FA" to "06285cb56f50968297de42703db5fe03482228da54dbb1f9219bad53f3d83191",
            "NOTES_4_ALTO" to "84c765f6db4ba9722f503cd72d5a2faa65b7c80ec79915b33efcd7b02df2fcb9",
            "CHORDS_4_ALTO" to "ecb8a842d6d4f465177c87231c3547cb4bc3b13a10fbee7a41962c51852d8dbe",
            "NOTES_4_TENOR" to "2088ba6779e09db57954bf1379ba61fe1b4fe02882556772542e927a9335dbff",
            "CHORDS_4_TENOR" to "7c645fffdbc83e66b0db8dec4d13e78eddac1161c2562129befcd5db6442e974",
            "INTERVALS_4" to "ca925dfcc05a72fe48b6dc7127a6d757ea7d99609ad656dd5da418df2da2220c",
        )
    }
}

private fun <T> fingerprint(initial: List<T>, advance: (List<T>) -> List<T>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    var queue = initial
    repeat(101) {
        digest.update(queue.toString().toByteArray(Charsets.UTF_8))
        queue = advance(queue)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
