package com.semstress.mobile.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.semstress.mobile"
private const val UI_TIMEOUT_MS = 5_000L

/**
 * PF-01, scenario 1: cold start -> interactive menu. Compares against `performance.md` §6's
 * target of < 1.5s (P50) to the menu becoming interactive. Run with:
 * `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartToMenu() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.DEFAULT
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Selecione sua fase")), UI_TIMEOUT_MS)
    }
}
