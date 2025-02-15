package dev.panuszewski.gradle

import dev.panuszewski.gradle.util.BaseGradleSpec
import dev.panuszewski.gradle.util.BuildOutcome.BUILD_SUCCESSFUL
import dev.panuszewski.gradle.util.IncludedBuildConfigurator
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class IncludedBuildSpec : BaseGradleSpec() {

    @ParameterizedTest
    @MethodSource("includedBuildConfigurators")
    fun `should allow to use catalog accessors in included build`(
        includedBuildForConventionPlugins: IncludedBuildConfigurator
    ) {
        // given
        val someLibrary = "org.apache.commons:commons-lang3:3.17.0"

        customProjectFile("gradle/libs.versions.toml") {
            """
            [libraries]
            some-library = "$someLibrary"
            """
        }

        buildGradleKts {
            """
            plugins {
                java
            }
            """
        }

        includedBuildForConventionPlugins {
            buildGradleKts {
                """
                plugins {
                    `kotlin-dsl`
                } 
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation(libs.some.library)
                }
                """
            }

            settingsGradleKts {
                """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                        mavenLocal()
                    }
                }
                    
                plugins {
                    id("dev.panuszewski.typesafe-conventions") version "${System.getenv("PROJECT_VERSION")}"
                }
                """
            }
        }

        // when
        val result = runGradle("assemble")

        // then
        result.buildOutcome shouldBe BUILD_SUCCESSFUL
    }

    @ParameterizedTest
    @MethodSource("includedBuildConfigurators")
    fun `should allow to use catalog accessors in included build when running task from subproject`(
        includedBuildForConventionPlugins: IncludedBuildConfigurator
    ) {
        // given
        val someLibrary = "org.apache.commons:commons-lang3:3.17.0"

        customProjectFile("gradle/libs.versions.toml") {
            """
            [libraries]
            some-library = "$someLibrary"
            """
        }

        subprojectBuildGradleKts("subproject") {
            """
            plugins {
                java
            }
            """
        }

        includedBuildForConventionPlugins {
            buildGradleKts {
                """
                plugins {
                    `kotlin-dsl`
                }
                    
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation(libs.some.library)
                }
                """
            }

            settingsGradleKts {
                """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                        mavenLocal()
                    }
                }
                    
                plugins {
                    id("dev.panuszewski.typesafe-conventions") version "${System.getenv("PROJECT_VERSION")}"
                }
                """
            }
        }

        // when
        val result = runGradle(":subproject:assemble")

        // then
        result.buildOutcome shouldBe BUILD_SUCCESSFUL
    }

    @ParameterizedTest
    @MethodSource("includedBuildConfigurators")
    fun `should provide pluginMarker helper method`(
        includedBuildForConventionPlugins: IncludedBuildConfigurator
    ) {
        // given
        val somePlugin = "pl.allegro.tech.build.axion-release"
        val somePluginVersion = "1.18.16"
        val taskRegisteredBySomePlugin = "verifyRelease"

        customProjectFile("gradle/libs.versions.toml") {
            """
            [plugins]
            some-plugin = { id = "$somePlugin", version = "$somePluginVersion" }
            """
        }

        buildGradleKts {
            """
            plugins {
                id("some-convention")
            }
            
            repositories {
                mavenCentral()
            }
            """
        }

        includedBuildForConventionPlugins {
            buildGradleKts {
                """
                import dev.panuszewski.gradle.pluginMarker
                    
                plugins {
                    `kotlin-dsl`
                } 
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation(pluginMarker(libs.plugins.some.plugin))
                }
                """
            }

            settingsGradleKts {
                """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                        mavenLocal()
                    }
                }
                    
                plugins {
                    id("dev.panuszewski.typesafe-conventions") version "${System.getenv("PROJECT_VERSION")}"
                }
                """
            }

            customProjectFile("src/main/kotlin/some-convention.gradle.kts") {
                """
                plugins {
                    id("$somePlugin")
                }
                """
            }
        }

        // when
        val result = runGradle("tasks")

        // then
        result.buildOutcome shouldBe BUILD_SUCCESSFUL
        result.output shouldContain taskRegisteredBySomePlugin
    }
}