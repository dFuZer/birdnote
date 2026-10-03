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
import com.example.birdnote.domain.Clef
import com.example.birdnote.domain.ClefMode
import com.example.birdnote.domain.IntervalConfig
import com.example.birdnote.domain.clefModesFor
import com.example.birdnote.domain.NoteNaming
import com.example.birdnote.domain.PracticeConfig
import com.example.birdnote.domain.PracticeMode
import com.example.birdnote.ui.screens.ChordQuizRoute
import com.example.birdnote.ui.screens.ChordSetupScreen
import com.example.birdnote.ui.screens.HomeScreen
import com.example.birdnote.ui.screens.IntervalQuizRoute
import com.example.birdnote.ui.screens.IntervalSetupScreen
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
    const val SETTINGS = "settings"
    const val QUIZ = "quiz/{difficulty}/{clefMode}"
    const val INTERVAL_QUIZ = "interval-quiz/{difficulty}"
    const val CHORD_QUIZ = "chord-quiz/{difficulty}/{clefMode}"
    const val RESULT = "result/{difficulty}/{clefMode}/{score}"
    const val INTERVAL_RESULT = "interval-result/{difficulty}/{score}"
    const val CHORD_RESULT = "chord-result/{difficulty}/{clefMode}/{score}"

    fun quiz(difficulty: Int, clefMode: ClefMode): String =
        "quiz/$difficulty/${clefMode.name}"

    fun intervalQuiz(difficulty: Int): String = "interval-quiz/$difficulty"

    fun chordQuiz(difficulty: Int, clefMode: ClefMode): String =
        "chord-quiz/$difficulty/${clefMode.name}"

    fun result(difficulty: Int, clefMode: ClefMode, score: Int): String =
        "result/$difficulty/${clefMode.name}/$score"

    fun intervalResult(difficulty: Int, score: Int): String =
        "interval-result/$difficulty/$score"

    fun chordResult(difficulty: Int, clefMode: ClefMode, score: Int): String =
        "chord-result/$difficulty/${clefMode.name}/$score"
}

@Composable
fun BirdNoteNavHost(
    navController: NavHostController,
    noteNaming: NoteNaming,
    onNoteNamingChange: (NoteNaming) -> Unit,
    preferredClefs: Set<Clef>,
    onPreferredClefToggle: (Clef) -> Unit,
    scoreStore: ScoreStore,
    modifier: Modifier = Modifier,
) {
    val clefModes = clefModesFor(preferredClefs)
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
                clefModes = clefModes,
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                noteNaming = noteNaming,
                onNoteNamingChange = onNoteNamingChange,
                preferredClefs = preferredClefs,
                onPreferredClefToggle = onPreferredClefToggle,
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Routes.TRAINING) {
            TrainingMenuScreen(
                onNotesClick = { navController.navigate(Routes.SETUP) },
                onIntervalsClick = { navController.navigate(Routes.INTERVAL_SETUP) },
                onChordsClick = { navController.navigate(Routes.CHORD_SETUP) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Routes.SETUP) {
            NoteSetupScreen(
                clefModes = clefModes,
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
                clefModes = clefModes,
                onStartClick = { difficulty, clefMode ->
                    navController.navigate(Routes.chordQuiz(difficulty, clefMode))
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
    }
}
