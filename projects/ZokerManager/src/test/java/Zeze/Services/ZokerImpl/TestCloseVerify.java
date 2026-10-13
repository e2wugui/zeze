package Zeze.Services.ZokerImpl;

import harness.Extra;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND19 GE-D04：closeAndVerify 三态契约（拍板方案A）。
 * 直构 DistributeManager（包内构造器，不依赖 Zoker 网络服务），覆盖：
 * 未 open 的 close→eNotOpened（不再谎报校验成功）；md5 失败→物理文件被删（暂存区损坏
 * 中间产物无保留价值）+eMd5Mismatch+下次 OpenFile 从 0 续传（状态机闭合）；
 * md5 成功→文件保留；已收尾的重复 close→eNotOpened（幂等路径的精确三态）。
 */
@Fast
@Extra
public class TestCloseVerify {
	private static final long NOT_OPENED = IModule.errorCode(Zoker.ModuleId, Zoker.eNotOpened);
	private static final long MD5_MISMATCH = IModule.errorCode(Zoker.ModuleId, Zoker.eMd5Mismatch);

	private static DistributeManager newManager(File distributeDir, File servicesDir) {
		return new DistributeManager(distributeDir, servicesDir);
	}

	private static byte[] md5Of(byte[] data) throws Exception {
		var md5 = MessageDigest.getInstance("MD5");
		md5.update(data);
		return md5.digest();
	}

	/** 未 open 就 Close（未传过/重复close/断链回收后补发——C06回收后agent补发的close必走此路径）。 */
	@Test
	public void testCloseNotOpened(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var dm = newManager(distributeDir, tempDir.resolve("services").toFile());
		assertEquals(NOT_OPENED, dm.closeAndVerify("svc", "x.jar", new Binary(md5Of(new byte[0])), null));
		// 路径守卫语义不变：eNotOpened 不落盘、不建目录
		assertFalse(new File(distributeDir, "svc").exists());
	}

	/**
	 * md5 失败：损坏中间产物被删除 + eMd5Mismatch；状态机闭合——下次 OpenFile 拿到 offset=0，
	 * 坏起点不再占位（修复前残留坏长度作为续传起点，把断点续传打回人工清理）。
	 * 嵌套路径（server/lib/x.jar）同形态：删除只动文件本身。
	 */
	@Test
	public void testMd5MismatchDeletesFile(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var dm = newManager(distributeDir, tempDir.resolve("services").toFile());
		var fileBin = dm.open("svc", "lib/x.jar", null);
		fileBin.append(0, new Binary("corrupted-partial".getBytes()));
		// 客户端本地完整文件与暂存区坏起点内容不一致（常态故障而非边角，见design事实链）
		var wrong = md5Of("totally-different".getBytes());
		assertEquals(MD5_MISMATCH, dm.closeAndVerify("svc", "lib/x.jar", new Binary(wrong), null));

		var file = new File(distributeDir, "svc/lib/x.jar");
		assertFalse(file.exists(), "md5失败后损坏文件必须被删除");
		// 状态机闭合：重开从 0 续传
		var reopened = dm.open("svc", "lib/x.jar", null);
		try {
			assertEquals(0, reopened.getLength(), "坏文件删除后OpenFile必须从0开始");
		} finally {
			reopened.close();
		}
	}

	/** md5 成功：文件保留（等待commit消费）+rc 0；已收尾的重复 close→eNotOpened 且不动磁盘现场。 */
	@Test
	public void testMd5MatchKeepsFileAndIdempotentReclose(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var dm = newManager(distributeDir, tempDir.resolve("services").toFile());
		var fileBin = dm.open("svc", "x.jar", null);
		var data = "hello-verify".getBytes();
		fileBin.append(0, new Binary(data));

		assertEquals(0, dm.closeAndVerify("svc", "x.jar", new Binary(md5Of(data)), null));
		var file = new File(distributeDir, "svc/x.jar");
		assertTrue(file.exists(), "校验一致的文件必须保留");
		assertArrayEquals(data, Files.readAllBytes(file.toPath()));

		// 重复 close（已收尾）：不在传输中→eNotOpened，不再谎报"校验通过"
		assertEquals(NOT_OPENED, dm.closeAndVerify("svc", "x.jar", new Binary(md5Of(data)), null));
		assertTrue(file.exists(), "eNotOpened路径不动磁盘现场");
	}
}
