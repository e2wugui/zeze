package UnitTest.Zeze.Netty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import Zeze.Netty.HttpFileUploadHandle;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND4-70：客户端可控上传文件名的路径穿越净化。原实现 new File(uploadDir, filename) 直接
 * delete+renameTo，"..%2F..%2F"类相对名以JVM工作目录为基准越权删除/覆盖任意文件。
 * sanitizeDestFile 契约：首道防线剥掉全部目录成分（穿越名退化为目录内basename，安全落地）；
 * 二道防线canonical前缀校验兜底，剥后仍出界（".."、"."、空等退化名）抛
 * IllegalArgumentException由端点应答400。
 */
@Fast
public class TestUploadFilenameSanitize {

	private static File dest(Path uploadDir, String name) throws IOException {
		return HttpFileUploadHandle.sanitizeDestFile(uploadDir.toFile(), name);
	}

	@Test
	public void testTraversalNeutralized(@TempDir Path uploadDir) throws IOException {
		// 穿越成分被剥掉：目标必须落在uploadDir内的basename上，绝不指向目录外（原实现
		// 会解析到 uploadDir/../evil.zip 并先 delete 触碰外部文件）
		Assertions.assertEquals(uploadDir.resolve("evil.zip").toFile(),
				dest(uploadDir, "../evil.zip").getCanonicalFile());
		Assertions.assertEquals(uploadDir.resolve("evil.zip").toFile(),
				dest(uploadDir, "..\\..\\evil.zip").getCanonicalFile());
		Assertions.assertEquals(uploadDir.resolve("normal.zip").toFile(),
				dest(uploadDir, "sub/normal.zip").getCanonicalFile());
		// 剥离后的canonical必须仍在目录内（二道防线本身）
		var sanitized = dest(uploadDir, "a/b/c/../../x.zip");
		Assertions.assertTrue(sanitized.getCanonicalPath()
				.startsWith(uploadDir.toFile().getCanonicalPath() + File.separator));
	}

	@Test
	public void testDegenerateNamesRejected(@TempDir Path uploadDir) {
		// 剥后无法构成目录内文件的退化名必须400（canonical兜底校验抛IllegalArgumentException）
		for (var name : new String[]{"..", ".", "", "/", "\\", "../"})
			Assertions.assertThrows(IllegalArgumentException.class, () -> dest(uploadDir, name),
					"必须拒绝退化文件名: '" + name + "'");
	}

	@Test
	public void testLegalNamesKept(@TempDir Path uploadDir) throws IOException {
		Assertions.assertEquals(uploadDir.resolve("patch_v1.zip").toFile(),
				dest(uploadDir, "patch_v1.zip").getCanonicalFile());
		// 目录尚不存在时同样正确判定（canonical解析不要求目录存在）；穿越名照样剥成目录内basename
		var notYet = uploadDir.resolve("a/b/c");
		Assertions.assertEquals(notYet.resolve("x.zip").toFile(), dest(notYet, "../x.zip").getCanonicalFile());
		Assertions.assertEquals(notYet.resolve("x.zip").toFile(), dest(notYet, "x.zip").getCanonicalFile());
		Files.createDirectories(notYet);
	}
}
