package Zeze.Raft;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@Fast
@Timeout(30)
public class TestRaftDirectoryDeletion {
	@TempDir
	Path temp;

	private void createLink(Path link, Path target) throws Exception {
		assertTrue(link.toAbsolutePath().normalize().startsWith(temp.toAbsolutePath().normalize()));
		assertTrue(target.toAbsolutePath().normalize().startsWith(temp.toAbsolutePath().normalize()));
		if (File.separatorChar != '\\') {
			Files.createSymbolicLink(link, target);
			return;
		}
		var builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
				"$ErrorActionPreference='Stop'; New-Item -ItemType Junction -Path $env:ZEZE_TEST_LINK "
						+ "-Target $env:ZEZE_TEST_TARGET | Out-Null").redirectErrorStream(true);
		builder.environment().put("ZEZE_TEST_LINK", link.toAbsolutePath().toString());
		builder.environment().put("ZEZE_TEST_TARGET", target.toAbsolutePath().toString());
		var child = builder.start();
		try {
			assertTrue(child.waitFor(10, TimeUnit.SECONDS), "创建目录链接超时");
			assertEquals(0, child.exitValue(), () -> {
				try {
					return new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
				} catch (Exception e) {
					return e.toString();
				}
			});
		} finally {
			if (child.isAlive()) {
				child.destroyForcibly();
				assertTrue(child.waitFor(5, TimeUnit.SECONDS));
			}
		}
	}

	private void assertTargetSurvives(Path root, Path link) throws Exception {
		var target = Files.createDirectory(temp.resolve("target"));
		var marker = Files.writeString(target.resolve("keep"), "keep");
		createLink(link, target);
		LogSequence.deleteDirectory(root.toFile());
		assertTrue(Files.exists(marker), "删除链接不得遍历并删除目标内容");
		assertFalse(Files.exists(root, LinkOption.NOFOLLOW_LINKS), "应删除链接本身及其父目录");
	}

	@Test
	void rootDirectoryLinkKeepsTarget() throws Exception {
		var link = temp.resolve("root-link");
		assertTargetSurvives(link, link);
	}

	@Test
	void nestedDirectoryLinkKeepsTarget() throws Exception {
		var root = Files.createDirectory(temp.resolve("root"));
		assertTargetSurvives(root, root.resolve("link"));
	}

	@Test
	void brokenDirectoryLinkIsRemoved() throws Exception {
		var target = Files.createDirectory(temp.resolve("target"));
		var link = temp.resolve("broken-link");
		createLink(link, target);
		Files.delete(target);
		LogSequence.deleteDirectory(link.toFile());
		assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
	}

	@Test
	void removesOrdinaryTreeFileAndMissingPath() throws Exception {
		var root = Files.createDirectories(temp.resolve("root/child"));
		Files.writeString(root.resolve("data"), "data");
		LogSequence.deletedDirectoryAndCheck(root.getParent().toFile(), 1);
		assertFalse(Files.exists(root.getParent()));
		var file = Files.writeString(temp.resolve("file"), "data");
		LogSequence.deleteDirectory(file.toFile());
		assertFalse(Files.exists(file));
		LogSequence.deletedDirectoryAndCheck(temp.resolve("absent").toFile(), 1);
	}
}
