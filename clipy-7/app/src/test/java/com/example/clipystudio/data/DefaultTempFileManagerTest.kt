package com.example.clipystudio.data

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DefaultTempFileManagerTest {
  @Test
  fun createShareableOutput_writesCompletedExportOutsideWorkspace() {
    val root = Files.createTempDirectory("clipy-temp-root").toFile()
    try {
      val manager = DefaultTempFileManager(root)
      val workspace = manager.createWorkspace("project-a")

      val output = manager.createShareableOutput(workspace, "final.mp4")

      assertTrue(output.exists())
      assertTrue(output.parentFile!!.name == "completed")
      assertTrue(output.canonicalPath.startsWith(root.canonicalPath))
      assertFalse(output.canonicalPath.startsWith(File(workspace.directoryPath).canonicalPath))
    } finally {
      root.deleteRecursively()
    }
  }

  @Test
  fun cleanup_removesWorkspaceButKeepsCompletedExport() {
    val root = Files.createTempDirectory("clipy-temp-root").toFile()
    try {
      val manager = DefaultTempFileManager(root)
      val workspace = manager.createWorkspace("project-b")
      val output = manager.createShareableOutput(workspace, "saved.mp4")

      val cleaned = manager.cleanup(workspace.copy(finalOutputPath = output.canonicalPath))

      assertTrue(cleaned.isCleaned)
      assertFalse(File(workspace.directoryPath).exists())
      assertTrue(output.exists())
      assertEquals("completed", output.parentFile!!.name)
    } finally {
      root.deleteRecursively()
    }
  }

  @Test
  fun cleanupStale_withImmediateAgePreservesCompletedExports() {
    val root = Files.createTempDirectory("clipy-temp-root").toFile()
    try {
      val manager = DefaultTempFileManager(root)
      val workspace = manager.createWorkspace("project-c")
      val output = manager.createShareableOutput(workspace, "stale.mp4")
      File(workspace.directoryPath).setLastModified(System.currentTimeMillis() - 172_800_000L)

      val cleared = manager.cleanupStale(0L)

      assertEquals(1, cleared)
      assertTrue(File(root, "completed").exists())
      assertTrue(output.exists())
    } finally {
      root.deleteRecursively()
    }
  }

  @Test
  fun createShareableOutput_sanitizesUnsafeDisplayNameInsideCompletedDirectory() {
    val root = Files.createTempDirectory("clipy-temp-root").toFile()
    try {
      val manager = DefaultTempFileManager(root)
      val workspace = manager.createWorkspace("project-d")

      val output = manager.createShareableOutput(workspace, "../bad/name.mp4")

      assertTrue(output.exists())
      assertEquals("completed", output.parentFile!!.name)
      assertTrue(output.canonicalPath.startsWith(root.canonicalPath))
      assertFalse(output.name.contains("/"))
      assertFalse(output.name.contains(".."))
    } finally {
      root.deleteRecursively()
    }
  }
}
