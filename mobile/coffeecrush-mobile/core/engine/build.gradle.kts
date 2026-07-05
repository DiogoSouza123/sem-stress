plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

kover {
    reports {
        filters {
            includes {
                packages("com.semstress.mobile.engine")
            }
        }
        verify {
            rule("Engine coverage") {
                minBound(90)
            }
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.javax.inject)

    testFixturesImplementation(project(":core:model"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    detektPlugins(libs.detekt.formatting)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
