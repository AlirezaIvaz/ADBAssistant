import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "ir.alirezaivaz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3-desktop:1.5.10")
    implementation(compose.materialIconsExtended)
}

compose.desktop {
    application {
        mainClass = "MainScreenKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ADBAssistant"
            packageVersion = "1.0.0"
            description = "A GUI for common ADB commands"
            copyright = "© 2024 Alireza Ivaz. All rights reserved."
            vendor = "Alireza Ivaz"
            licenseFile.set(project.file("LICENSE.txt"))
            windows {
                iconFile.set(project.file("icon.ico"))
            }
            linux {
                iconFile.set(project.file("icon.png"))
                shortcut = true
                debMaintainer = "ivaz1382@gmail.com"
                appCategory = "utils"
            }
            buildTypes.release {
                proguard {
                    configurationFiles.from("compose.desktop.pro")
                }
            }
        }
    }
}
