package com.example.birdnote.domain

/** Correct answers in a row before the quiet streak reward shows. */
const val STREAK_REWARD_LENGTH = 3

/** Soonest another sparkle may pop while a streak still holds. */
const val SPARKLE_COOLDOWN_MILLIS = 3_000L

data class StreakReward(
    val correctInARow: Int = 0,
    val sparklePopMillis: Long? = null,
)

fun streakActive(correctInARow: Int): Boolean = correctInARow >= STREAK_REWARD_LENGTH

/**
 * Next streak after one answer. A miss clears the run and the sparkle.
 * A hit at or above [STREAK_REWARD_LENGTH] pops a sparkle only when
 * [SPARKLE_COOLDOWN_MILLIS] has passed since the previous pop.
 */
fun streakAfterAnswer(
    correctInARow: Int,
    sparklePopMillis: Long?,
    correct: Boolean,
    nowMillis: Long,
): StreakReward {
    if (!correct) return StreakReward()
    val next = correctInARow + 1
    if (!streakActive(next)) return StreakReward(correctInARow = next)
    val lastPop = sparklePopMillis
    val pop = if (lastPop == null || nowMillis - lastPop >= SPARKLE_COOLDOWN_MILLIS) {
        nowMillis
    } else {
        lastPop
    }
    return StreakReward(correctInARow = next, sparklePopMillis = pop)
}
