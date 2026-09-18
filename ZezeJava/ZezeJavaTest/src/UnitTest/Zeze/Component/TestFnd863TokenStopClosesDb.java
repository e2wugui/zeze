package UnitTest.Zeze.Component;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.file.Path;

import Zeze.Services.Token;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * FND8-63 回归：stop() 只 saveDB 不关库，rocksdb/tokenMapTable 句柄悬挂——restart 的
 * start 对同目录二次 open：Windows 下 LOCK 互斥每次必抛（重试环还先空转约10秒），
 * Linux 下双实例双 WAL/memtable 写同目录。
 * 修复后 stop 在 saveDB 后调 closeDb()（记录 saveDB 失败），start 的 getOrAddTable
 * 移入回滚 try 块。断言 stop 后句柄清空、同目录 restart 真实成功。
 */
@Fast
@ResourceLock("token.rocksdb") // Token经全局System property定位DB目录，与同族测试并行互相覆盖路径
public class TestFnd863TokenStopClosesDb {
	private static final Field ROCKSDB_FIELD;

	static {
		try {
			ROCKSDB_FIELD = Token.class.getDeclaredField("rocksdb");
			ROCKSDB_FIELD.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static int probePort() throws IOException {
		for (int port = 28600; port < 28639; port++) {
			try (var ignore = new ServerSocket(port)) {
				return port;
			} catch (IOException e) {
				// 端口被占，探测下一个
			}
		}
		throw new IOException("no free port in [28600,28639)");
	}

	@Test
	public void testStopClosesDbAndRestartSucceeds(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		System.setProperty("token.rocksdb", tempDir.resolve("a5_token_db").toString());
		var token = new Token();
		try {
			token.start(null, "127.0.0.1", probePort());
			Assertions.assertNotNull(token.getService());
			Assertions.assertNotNull(ROCKSDB_FIELD.get(token));

			token.stop();
			Assertions.assertNull(ROCKSDB_FIELD.get(token), "stop必须关库并清引用（修复前句柄悬挂）");

			// 同目录restart：修复前Windows下LOCK互斥必抛（重试环先空转约10秒）
			token.start(null, "127.0.0.1", probePort());
			Assertions.assertNotNull(token.getService(), "stop→start重启必须真实成功");
			Assertions.assertNotNull(ROCKSDB_FIELD.get(token), "重启必须重新打开DB");
		} finally {
			try {
				token.stop();
			} catch (Exception ignored) {
			}
			token.closeDb(); // Windows下释放rocksdb文件锁，TempDir才能清理
			System.clearProperty("token.rocksdb");
		}
	}
}
