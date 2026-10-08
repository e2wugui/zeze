package Zeze.Services.ZokerImpl;

import harness.Extra;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import Zeze.Net.Binary;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND30 zoker-01：真实流量形态（ServiceName=空串）的 open/append/closeAndVerify 全链路。
 * ZokerAgent 的三个文件 RPC 从不设置 ServiceName（bean 默认空串），文件相对路径
 * （localServiceHome.relativize，带服务名首段，形如 "svc/lib/x.jar"）整体放在 FileName 里。
 * 修复前：服务端 new File("", fileName) 的空父目录被 JDK 替换为默认父（Windows "\"、
 * Linux "/"），合成根相对/绝对路径（\svc\lib\x.jar / /svc/lib/x.jar），checkInsideDir 的
 * base.resolve 对带根成分的路径不再拼接到 base 之下，判为越界逃逸拒绝——真实流量的
 * OpenFile 100% 失败（eOpenError）。现存测试只覆盖 open("svc", "lib/x.jar") 的非真实
 * 形态，本用例补齐缺口。
 */
@Fast
@Extra
public class TestEmptyServiceNameRealTrafficForm {

	private static byte[] md5Of(byte[] data) throws Exception {
		var md5 = MessageDigest.getInstance("MD5");
		md5.update(data);
		return md5.digest();
	}

	/** ZokerAgent 真实流量三 RPC 同链路：空 ServiceName + 带服务名首段的 FileName，落盘 distributes/svc/lib/x.jar。 */
	@Test
	public void testOpenAppendCloseWithEmptyServiceName(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var dm = new DistributeManager(distributeDir, tempDir.resolve("services").toFile());
		// 真实形态：ServiceName 不设置（空串），FileName="svc/lib/x.jar"
		assertEquals(0, dm.open("", "svc/lib/x.jar", null).getLength(),
				"真实流量的 OpenFile 必须成功（全新文件断点=0）");
		var data = "real-traffic-empty-servicename".getBytes();
		dm.append("", "svc/lib/x.jar", 0, new Binary(data));
		assertEquals(0, dm.closeAndVerify("", "svc/lib/x.jar", new Binary(md5Of(data)), null),
				"close 校验一致（三处路径/键合成须同点）");
		var file = new File(distributeDir, "svc/lib/x.jar");
		assertTrue(file.exists(), "文件必须落盘到 distributes/svc/lib/x.jar");
		assertArrayEquals(data, Files.readAllBytes(file.toPath()));
	}
}
