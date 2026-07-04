package com.semstress.mobile.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.semstress.mobile"
private const val UI_TIMEOUT_MS = 5_000L
private const val SWAP_ATTEMPTS = 12
private const val SWAP_STEPS = 8

/**
 * PF-01, scenarios 2 and 3: opening a stage from the menu, then a burst of swaps meant to trigger
 * matches/cascades. `board` is a single Canvas without per-cell semantics yet (UX-11 is still
 * pending), so cells can't be targeted by content description — this taps a fixed diagonal
 * pattern of screen-relative coordinates instead. That reliably opens the stage and exercises
 * real gameplay frames, but can't *guarantee* a 3x cascade every run the way a seeded board would;
 * revisit with a fixed seed once CQ-03's debug panel exists.
 */
@RunWith(AndroidJUnit4::class)
class GameplayBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun openStage() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.DEFAULT
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Jogar fase selecionada")), UI_TIMEOUT_MS)
        device.findObject(By.text("Jogar fase selecionada")).click()
        device.wait(Until.hasObject(By.text("Voltar ao menu")), UI_TIMEOUT_MS)
    }

    @Test
    fun playMovesWithCascades() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.DEFAULT,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.text("Jogar fase selecionada")), UI_TIMEOUT_MS)
            device.findObject(By.text("Jogar fase selecionada")).click()
            device.wait(Until.hasObject(By.text("Voltar ao menu")), UI_TIMEOUT_MS)
        }
    ) {
        val bounds = device.displayWidth to device.displayHeight
        val boardTop = (bounds.second * BOARD_TOP_FRACTION).toInt()
        val boardBottom = (bounds.second * BOARD_BOTTOM_FRACTION).toInt()
        val boardLeft = (bounds.first * BOARD_SIDE_FRACTION).toInt()
        val boardRight = (bounds.first * (1 - BOARD_SIDE_FRACTION)).toInt()
        val stepX = (boardRight - boardLeft) / SWAP_STEPS
        val stepY = (boardBottom - boardTop) / SWAP_STEPS

        repeat(SWAP_ATTEMPTS) { attempt ->
            val col = attempt % SWAP_STEPS
            val row = (attempt / SWAP_STEPS) % SWAP_STEPS
            val x = boardLeft + stepX * col + stepX / 2
            val y = boardTop + stepY * row + stepY / 2
            device.click(x, y)
            device.click(x + stepX, y)
        }
    }

    private companion object {
        const val BOARD_TOP_FRACTION = 0.35
        const val BOARD_BOTTOM_FRACTION = 0.85
        const val BOARD_SIDE_FRACTION = 0.08
    }
}
