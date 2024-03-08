package io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.IOException
import java.nio.file.Paths


class DefaultFileSystemAccessTest extends AnyWordSpec with Matchers {

  "The Default FS" should {

    "resolve absolute paths relative to root" in new Fixture {

      if (sys.props.get("os.name").exists(_.toLowerCase contains "win")) {
        // On Windows the default paths look a little different and absolute paths always need a drive letter (default: "C:") at the front
        fs.resolveRealPath(fs.resolveAbsolute(Seq.empty)) shouldBe Paths.get("C:\\")
        fs.resolveRealPath(fs.resolveAbsolute("E:/", "foo", "bar", "BAZ")) shouldBe Paths.get("E:/foo/bar/BAZ")
        fs.resolveRealPath(fs.resolveAbsolute("C:/foo", "..", "bar", ".")) shouldBe Paths.get("C:/bar")
        fs.resolveRealPath(fs.resolveAbsolute("C:\\Program Files", "simplifier", "storage")) shouldBe Paths.get("C:/Program Files/simplifier/storage")
        fs.resolveRealPath(fs.resolveAbsolute("..")) shouldBe Paths.get("C:/")
      } else {
        // Unix
        fs.resolveRealPath(fs.resolveAbsolute(Seq.empty)) shouldBe Paths.get("/")
        fs.resolveRealPath(fs.resolveAbsolute("/", "foo", "bar", "BAZ")) shouldBe Paths.get("/foo/bar/BAZ")
        fs.resolveRealPath(fs.resolveAbsolute("/foo", "..", "bar", ".")) shouldBe Paths.get("/bar")
        fs.resolveRealPath(fs.resolveAbsolute("opt", "simplifier", "storage")) shouldBe Paths.get("/opt/simplifier/storage")
        fs.resolveRealPath(fs.resolveAbsolute("..")) shouldBe Paths.get("/")
      }
    }

    "resolve storage paths relative to storage base" in new Fixture {
      fs.resolveRealPath(fs.resolveStorage(Seq.empty)) shouldBe Paths.get(storageSettings.storageDir)
      fs.resolveRealPath(fs.resolveStorage(".")) shouldBe Paths.get(storageSettings.storageDir)
      fs.resolveRealPath(fs.resolveStorage("test")) shouldBe Paths.get(storageSettings.storageDir, "test")
      fs.resolveRealPath(fs.resolveStorage("test/../test2")) shouldBe Paths.get(storageSettings.storageDir, "test2")
      fs.resolveRealPath(fs.resolveStorage("test", "file.txt")) shouldBe Paths.get(storageSettings.storageDir, "test/file.txt")
    }

    "resolve temp paths relative to temp base" in new Fixture {
      fs.resolveRealPath(fs.resolveTemp(Seq.empty)) shouldBe Paths.get(storageSettings.tempDir)
      fs.resolveRealPath(fs.resolveTemp(".")) shouldBe Paths.get(storageSettings.tempDir)
      fs.resolveRealPath(fs.resolveTemp("test")) shouldBe Paths.get(storageSettings.tempDir, "test")
      fs.resolveRealPath(fs.resolveTemp("test/../test2")) shouldBe Paths.get(storageSettings.tempDir, "test2")
      fs.resolveRealPath(fs.resolveTemp("test", "file.txt")) shouldBe Paths.get(storageSettings.tempDir, "test/file.txt")
      fs.resolveRealPath(fs.resolveTemp("/test")) shouldBe Paths.get(storageSettings.tempDir, "test")
    }

    "ensure containment of logic storage paths" in new Fixture {
      a [IOException] shouldBe thrownBy {
        fs.resolveRealPath(fs.resolveStorage(".."))
      }
      a [IOException] shouldBe thrownBy {
        fs.resolveRealPath(fs.resolveStorage("test/test2/../../../."))
      }
    }

    "ensure containment of logic temp paths" in new Fixture {
      a [IOException] shouldBe thrownBy {
        fs.resolveRealPath(fs.resolveTemp(".."))
      }
      a [IOException] shouldBe thrownBy {
        fs.resolveRealPath(fs.resolveTemp("test/test2/../../../."))
      }
    }

  }

  trait Fixture {

    // No actual IO is produced on this folders, so it is not required to have them existing on the testing system
    val storageSettings = new StorageSettingsLike {
      override def tempDir = "/temp"
      override def storageDir = "/storage"
    }

    val fs = new DefaultFileSystemAccess(storageSettings)

  }

}
