package com.example.birdnote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.birdnote.R
import com.example.birdnote.ScoreStore
import com.example.birdnote.domain.ChordConfig
import com.example.birdnote.domain.ClefMode
import com.example.birdnote.domain.IntervalConfig
import com.example.birdnote.domain.KeySignatureConfig
import com.example.birdnote.domain.ALL_MAJOR_KEYS
import com.example.birdnote.domain.MajorKey
import com.example.birdnote.domain.NoteNaming
import com.example.birdnote.domain.PracticeConfig
import com.example.birdnote.domain.PracticeMode
import com.example.birdnote.domain.majorKeyFromRoute
import com.example.birdnote.domain.routeValue
import com.example.birdnote.ui.screens.ChordQuizRoute
import com.example.birdnote.ui.screens.ChordSetupScreen
import com.example.birdnote.ui.screens.HomeScreen
import com.example.birdnote.ui.screens.IntervalQuizRoute
import com.example.birdnote.ui.screens.IntervalSetupScreen
import com.example.birdnote.ui.screens.KeySignatureQuizRoute
import com.example.birdnote.ui.screens.KeySignatureSetupScreen
import com.example.birdnote.ui.screens.NoteSetupScreen
import com.example.birdnote.ui.screens.QuizRoute
import com.example.birdnote.ui.screens.ResultScreen
import com.example.birdnote.ui.screens.ScoresScreen
import com.example.birdnote.ui.screens.SettingsScreen
import com.example.birdnote.ui.screens.TrainingMenuScreen

object Routes {
    const val HOME = "home"
    const val SCORES = "scores"
    const val TRAINING = "training"
    const val SETUP = "setup"
    const val INTERVAL_SETUP = "interval-setup"
    const val CHORD_SETUP = "chord-setup"
    const val KEY_SIGNATURE_SETUP = "key-signature-setup"
    const val SETTINGS = "settings"
    const val QUIZ = "quiz/{difficulty}/{clefMode}"
    const val INTERVAL_QUIZ = "interval-quiz/{difficulty}"
    const val CHORD_QUIZ = "chord-quiz/{difficulty}/{clefMode}"
    const val KEY_SIGNATURE_QUIZ = "key-signature-quiz/{difficulty}/{clefMode}/{key}"
    const val RESULT = "result/{difficulty}/{clefMode}/{score}"
    const val INTERVAL_RESULT = "interval-result/{difficulty}/{score}"
    const val CHORD_RESULT = "chord-result/{difficulty}/{clefMode}/{score}"
    const val KEY_SIGNATURE_RESULT = "key-signature-result/{difficulty}/{clefMode}/{key}/{score}"

    fun quiz(difficulty: Int, clefMode: ClefMode): String =
        "quiz/$difficulty/${clefMode.name}"

    fun intervalQuiz(difficulty: Int): String = "interval-quiz/$difficulty"

    fun chordQuiz(difficulty: Int, clefMode: ClefMode): String =
        "chord-quiz/$difficulty/${clefMode.name}"

    fun keySignatureQuiz(difficulty: Int, clefMode: ClefMode, key: MajorKey?): String =
        "key-signature-quiz/$difficulty/${clefMode.name}/${routeValue(key)}"

    fun result(difficulty: Int, clefMode: ClefMode, score: Int): String =
        "result/$difficulty/${clefMode.name}/$score"

    fun intervalResult(difficulty: Int, score: Int): String =
        "interval-result/$difficulty/$score"

    fun chordResult(difficulty: Int, clefMode: ClefMode, score: Int): String =
        "chord-result/$difficulty/${clefMode.name}/$score"

    fun keySignatureResult(
        difficulty: Int,
        clefMode: ClefMode,
        key: MajorKey?,
        score: Int,
    ): String = "key-signature-result/$difficulty/${clefMode.name}/${routeValue(key)}/$score"
}

@Composable
fun BirdNoteNavHost(
    navController: NavHostController,
    noteNaming: NoteNaming,
    onNoteNamingChange: (NoteNaming) -> Unit,
    scoreStore: ScoreStore,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onTrainClick = { navController.navigate(Routes.TRAINING) },
                onScoresClick = { navController.navigate(Routes.SCORES) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SCORES) {
            val scores by scoreStore.scores.collectAsStateWithLifecycle()
            ScoresScreen(
                scores = scores,
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                noteNaming = noteNaming,
                onNoteNamingChange = onNoteNamingChange,
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Routes.TRAINING) {
            TrainingMenuScreen(
                onNotesClick = { navController.navigate(Routes.SETUP) },
                onIntervalsClick = { navController.navigate(Routes.INTERVAL_SETUP) },
                onChordsClick = { navController.navigate(Routes.CHORD_SETUP) },
                onKeySignaturesClick = { navController.navigate(Routes.KEY_SIGNATURE_SETUP) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Routes.SETUP) {
            NoteSetupScreen(
                onStartClick = { difficulty, clefMode ->
                    navController.navigate(Routes.quiz(difficulty, clefMode))
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Routes.INTERVAL_SETUP) {
            IntervalSetupScreen(
                onStartClick = { difficulty ->
                    navController.navigate(Routes.intervalQuiz(difficulty))
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Routes.CHORD_SETUP) {
            ChordSetupScreen(
                onStartClick = { difficulty, clefMode ->
                    navController.navigate(Routes.chordQuiz(difficulty, clefMode))
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Routes.KEY_SIGNATURE_SETUP) {
            KeySignatureSetupScreen(
                noteNaming = noteNaming,
                onStartClick = { difficulty, clefMode, key ->
                    navController.navigate(Routes.keySignatureQuiz(difficulty, clefMode, key))
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.QUIZ,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.IntType },
                navArgument("clefMode") { type = NavType.StringType },
            ),
        ) { entry ->
            val difficulty = entry.arguments?.getInt("difficulty") ?: 1
            val clefMode = ClefMode.valueOf(entry.arguments?.getString("clefMode") ?: ClefMode.SOL.name)
            QuizRoute(
                config = PracticeConfig(difficulty, clefMode),
                noteNaming = noteNaming,
                onFinished = { score ->
                    scoreStore.record(PracticeMode.NOTES, difficulty, clefMode, score)
                    navController.navigate(Routes.result(difficulty, clefMode, score)) {
                        popUpTo(Routes.QUIZ) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.INTERVAL_QUIZ,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.IntType },
            ),
        ) { entry ->
            val difficulty = entry.arguments?.getInt("difficulty") ?: 1
            IntervalQuizRoute(
                config = IntervalConfig(difficulty),
                onFinished = { score ->
                    scoreStore.record(PracticeMode.INTERVALS, difficulty, null, score)
                    navController.navigate(Routes.intervalResult(difficulty, score)) {
                        popUpTo(Routes.INTERVAL_QUIZ) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.CHORD_QUIZ,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.IntType },
                navArgument("clefMode") { type = NavType.StringType },
            ),
        ) { entry ->
            val difficulty = entry.arguments?.getInt("difficulty") ?: 1
            val clefMode = ClefMode.valueOf(
                entry.arguments?.getString("clefMode") ?: ClefMode.SOL.name,
            )
            ChordQuizRoute(
                config = ChordConfig(difficulty, clefMode),
                onFinished = { score ->
                    scoreStore.record(PracticeMode.CHORDS, difficulty, clefMode, score)
                    navController.navigate(Routes.chordResult(difficulty, clefMode, score)) {
                        popUpTo(Routes.CHORD_QUIZ) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.KEY_SIGNATURE_QUIZ,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.IntType },
                navArgument("clefMode") { type = NavType.StringType },
                navArgument("key") { type = NavType.StringType },
            ),
        ) { entry ->
            val difficulty = entry.arguments?.getInt("difficulty") ?: 1
            val clefMode = ClefMode.valueOf(
                entry.arguments?.getString("clefMode") ?: ClefMode.SOL.name,
            )
            val key = majorKeyFromRoute(entry.arguments?.getString("key") ?: ALL_MAJOR_KEYS)
            KeySignatureQuizRoute(
                config = KeySignatureConfig(difficulty, clefMode, key),
                noteNaming = noteNaming,
                onFinished = { score ->
                    scoreStore.record(PracticeMode.KEY_SIGNATURES, difficulty, clefMode, score)
                    navController.navigate(
                        Routes.keySignatureResult(difficulty, clefMode, key, score),
                    ) {
                        popUpTo(Routes.KEY_SIGNATURE_QUIZ) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.IntType },
                navArgument("clefMode") { type = NavType.StringType },
                navArgument("score") { type = NavType.IntType },
            ),
        ) { entry ->
            val difficulty = entry.arguments?.getInt("difficulty") ?: 1
            val clefMode = ClefMode.valueOf(entry.arguments?.getString("clefMode") ?: ClefMode.SOL.name)
            val score = entry.arguments?.getInt("score") ?: 0
            ResultScreen(
                score = score,
                onRestartClick = {
                    navController.navigate(Routes.quiz(difficulty, clefMode)) {
                        popUpTo(Routes.RESULT) { inclusive = true }
                    }
                },
                onMenuClick = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
        composable(
            route = Routes.INTERVAL_RESULT,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.IntType },
                navArgument("score") { type = NavType.IntType },
            ),
        ) { entry ->
            val difficulty = entry.arguments?.getInt("difficulty") ?: 1
            val score = entry.arguments?.getInt("score") ?: 0
            ResultScreen(
                score = score,
                scorePluralRes = R.plurals.score_intervals,
                onRestartClick = {
                    navController.navigate(Routes.intervalQuiz(difficulty)) {
                        popUpTo(Routes.INTERVAL_RESULT) { inclusive = true }
                    }
                },
                onMenuClick = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
        composable(
            route = Routes.CHORD_RESULT,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.IntType },
                navArgument("clefMode") { type = NavType.StringType },
                navArgument("score") { type = NavType.IntType },
            ),
        ) { entry ->
            val difficulty = entry.arguments?.getInt("difficulty") ?: 1
            val clefMode = ClefMode.valueOf(
                entry.arguments?.getString("clefMode") ?: ClefMode.SOL.name,
            )
            val score = entry.arguments?.getInt("score") ?: 0
            ResultScreen(
                score = score,
                scorePluralRes = R.plurals.score_chords,
                onRestartClick = {
                    navController.navigate(Routes.chordQuiz(difficulty, clefMode)) {
                        popUpTo(Routes.CHORD_RESULT) { inclusive = true }
                    }
                },
                onMenuClick = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
        composable(
            route = Routes.KEY_SIGNATURE_RESULT,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.IntType },
                navArgument("clefMode") { type = NavType.StringType },
                navArgument("key") { type = NavType.StringType },
                navArgument("score") { type = NavType.IntType },
            ),
        ) { entry ->
            val difficulty = entry.arguments?.getInt("difficulty") ?: 1
            val clefMode = ClefMode.valueOf(
                entry.arguments?.getString("clefMode") ?: ClefMode.SOL.name,
            )
            val key = majorKeyFromRoute(entry.arguments?.getString("key") ?: ALL_MAJOR_KEYS)
            val score = entry.arguments?.getInt("score") ?: 0
            ResultScreen(
                score = score,
                scorePluralRes = R.plurals.score_key_signatures,
                onRestartClick = {
                    navController.navigate(Routes.keySignatureQuiz(difficulty, clefMode, key)) {
                        popUpTo(Routes.KEY_SIGNATURE_RESULT) { inclusive = true }
                    }
                },
                onMenuClick = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
    }
}
