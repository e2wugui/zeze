package UnitTest.Zeze.Services;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import Zeze.Services.Daemon;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-37：Register 的 mmap 白名单校验。mmap 由子进程 createTempFile("zeze",".mmap") +
 * setLength(8*globalCount) 创建——Daemon 侧限定临时目录、前缀后缀、尺寸吻合、globalCount
 * 上界，非信任报文不得以任意路径/任意尺寸注册（曾直接 new AtomicReferenceArray(globalCount)
 * 与 RandomAccessFile("rw")，坏值即 OOM/任意建删文件，且异常触发 fatalExit halt 看门狗）。
 */
@Fast
public class TestDaemonRegisterValidation {

	@Test
	public void testRegisterMmapValidation() throws Exception {
		var m = Daemon.class.getDeclaredMethod("isValidRegister", Daemon.Register.class);
		m.setAccessible(true);

		var dir = Path.of(System.getProperty("java.io.tmpdir"));
		var good = Files.createTempFile(dir, "zeze", ".mmap");
		var evil = (Path)null;
		var wrongSize = (Path)null;
		try (var ch = FileChannel.open(good, StandardOpenOption.WRITE)) {
			ch.write(ByteBuffer.allocate(16), 0); // globalCount=2 -> 8*2 字节
		}
		try {
			Assertions.assertTrue((boolean)m.invoke(null, new Daemon.Register(1, 2, good.toString())),
					"临时目录内zeze*.mmap且尺寸==globalCount*8必须通过");

			Assertions.assertFalse((boolean)m.invoke(null, new Daemon.Register(1, 0, good.toString())),
					"globalCount=0拒绝");
			Assertions.assertFalse((boolean)m.invoke(null, new Daemon.Register(1, 2000, good.toString())),
					"globalCount超上界拒绝（伪造巨值OOM防护）");

			evil = Files.createTempFile(dir, "evil", ".mmap");
			Assertions.assertFalse((boolean)m.invoke(null, new Daemon.Register(1, 0, evil.toString())),
					"非zeze前缀拒绝（任意路径打开/删除防护）");

			wrongSize = Files.createTempFile(dir, "zeze", ".mmap"); // 尺寸0 != 8*globalCount
			Assertions.assertFalse((boolean)m.invoke(null, new Daemon.Register(1, 2, wrongSize.toString())),
					"尺寸不符拒绝");

			Assertions.assertFalse((boolean)m.invoke(null,
							new Daemon.Register(1, 2, good.resolveSibling("zezeNotExist.mmap").toString())),
					"不存在的文件拒绝（rw打开会创建，必须前置拒）");
			Assertions.assertFalse((boolean)m.invoke(null, new Daemon.Register(1, 2, "zeze.mmap")),
					"非临时目录拒绝");
		} finally {
			Files.deleteIfExists(good);
			if (evil != null)
				Files.deleteIfExists(evil);
			if (wrongSize != null)
				Files.deleteIfExists(wrongSize);
		}
	}
}
