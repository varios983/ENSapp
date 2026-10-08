package com.ensapp.app

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfflineManifestTest {
    @Test
    fun `application manifests declare no internet permission or cloud clients`() {
        val repositoryRoot = Path.of(System.getProperty("repositoryRoot"))
            .toAbsolutePath()
            .normalize()
        val mergedManifest = repositoryRoot.resolve(
            "app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml",
        )
        assertTrue(Files.exists(mergedManifest), "The merged APK manifest must be available")
        val mergedXml = Files.newBufferedReader(mergedManifest).use { it.readText() }
        val internetPermission = mergedXml.contains("android.permission.INTERNET")
        assertFalse(internetPermission, "An app/library manifest declares INTERNET")

        val clientLibraries = listOf("app", "data", "presentation").any { module ->
            val buildFile = repositoryRoot.resolve(module).resolve("build.gradle.kts")
            Files.newBufferedReader(buildFile).use { it.readText() }.contains(
                Regex("retrofit|okhttp|firebase|ktor-client|play-services"),
            )
        }
        assertFalse(clientLibraries, "An online/cloud client dependency was introduced")
    }
}
