import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.0"
}

group = "ao.cmc.fincrestsdvm"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val ktorVersion = "2.3.12"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // Reads the .xlsx "mapa auxiliar" template for the Excel-upload import path.
    implementation("org.apache.poi:poi-ooxml:5.3.0")

    // Renders the brand logo SVGs (no native Compose SVG support on desktop).
    implementation("com.formdev:svgSalamander:1.1.4")
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "ao.cmc.fincrestsdvm.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.AppImage, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "Fincrest Reportes"
            packageVersion = "1.0.0"
            description = "Portal de Reportes SIRA / CMC"
            vendor = "Fincrest"

            linux {
                iconFile.set(project.file("src/main/resources/icons/icon.png"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icons/icon.ico"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icons/icon.icns"))
            }
        }
    }
}
