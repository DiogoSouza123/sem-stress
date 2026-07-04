package com.semstress.mobile.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.semstress.mobile"
private const val UI_TIMEOUT_MS = 5_000L

/**
 * PF-01: generates `app/src/main/baseline-prof.txt`, ahead-of-time-compiling the classes touched
 * by the critical path below (cold start, opening a stage, a few moves) instead of relying on
 * JIT warm-up on every install. No `androidx.baselineprofile` Gradle plugin wired up (that also
 * needs Gradle Managed Devices) — run manually with
 * `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest --tests BaselineProfileGenerator` and
 * copy the profile printed under `Benchmark results` (`...-baseline-prof.txt` in the module's
 * `build/outputs/managed_device_android_test_additional_output` / connected-test output folder)
 * into `app/src/main/baseline-prof.txt`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Jogar fase selecionada")), UI_TIMEOUT_MS)
        device.findObject(By.text("Jogar fase selecionada")).click()
        device.wait(Until.hasObject(By.text("Voltar ao menu")), UI_TIMEOUT_MS)
    }
}
