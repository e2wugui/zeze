package UnitTest.Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Raft.LogSequence;
import Zeze.Util.DumpRocksDb;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * R2-U2回归（backlog U2①）：DumpRocksDb各分支的rocksjava native对象——open(...,outHandles)
 * 返回的ColumnFamilyHandle、DBOptions/ColumnFamilyOptions、compact1的CompactionOptions、
 * listColumnFamilies的Options——原先从不close，native内存泄漏（一次性CLI工具，量级小但
 * 属同族句柄生命期缺陷）。修复：句柄在db.close前逐个close（异常安全），options走
 * try-with-resources。
 * 本测试用真实临时库驱动全部五个分支（listing/meta/dump/compact1/compact），主要钉住
 * 关闭路径无重复释放/崩溃（native误用多表现为进程级崩溃或异常），并保留工具行为可用。
 */
@Fast
public class TestDumpRocksDbNativeCloseSmoke {

	// main按系统属性分派且属性跨调用残留，每次调用前清干净保证分支确定性
	private static void runMain(String... args) throws Exception {
		System.clearProperty("meta");
		System.clearProperty("compact");
		System.clearProperty("compact1");
		System.clearProperty("key");
		System.clearProperty("value");
		DumpRocksDb.main(args);
	}

	@Test
	public void testAllBranchesCloseNativeAndWork() throws Exception {
		var dbPath = Path.of("TestDumpRocksDbNativeCloseSmoke.tmp");
		LogSequence.deleteDirectory(dbPath.toFile());
		try {
			try (var rdb = new RocksDatabase(dbPath.toString())) {
				var table = rdb.getOrAddTable("default");
				table.put("smokeKey1".getBytes(StandardCharsets.UTF_8),
						"smokeValue1".getBytes(StandardCharsets.UTF_8));
				table.put("smokeKey2".getBytes(StandardCharsets.UTF_8),
						"smokeValue2".getBytes(StandardCharsets.UTF_8));
			}

			// listing分支（列族列表）
			Assertions.assertDoesNotThrow(() -> runMain(dbPath.toString()));
			// meta分支
			Assertions.assertDoesNotThrow(() -> runMain(dbPath.toString(), "-meta"));
			// dump分支（导出到临时文件）
			var out = dbPath.resolveSibling("TestDumpRocksDbNativeCloseSmoke.out");
			Assertions.assertDoesNotThrow(() -> runMain(dbPath.toString(), "default", out.toString()));
			var dumped = Files.readString(out);
			Assertions.assertTrue(dumped.contains("smokeKey1") && dumped.contains("smokeValue2"),
					"dump输出必须包含写入的记录:\n" + dumped);
			// compact1分支（level-0合并，写模式打开）
			Assertions.assertDoesNotThrow(() -> runMain(dbPath.toString(), "-compact1"));
			// compact分支
			Assertions.assertDoesNotThrow(() -> runMain(dbPath.toString(), "-compact"));
			// compact后再dump一次：数据仍在，关闭路径反复执行无副作用
			Assertions.assertDoesNotThrow(() -> runMain(dbPath.toString(), "default", out.toString()));
			Assertions.assertTrue(Files.readString(out).contains("smokeKey2"));
		} finally {
			LogSequence.deleteDirectory(dbPath.toFile());
			Files.deleteIfExists(dbPath.resolveSibling("TestDumpRocksDbNativeCloseSmoke.out"));
		}
	}
}
