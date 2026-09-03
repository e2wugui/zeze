package UnitTest.Zeze.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import harness.Fast;
import Zeze.Net.Binary;
import Zeze.Services.Token;
import Zeze.Util.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND-S3-3 回归：token 状态曾落库（优雅停机 saveDB 或软引用溢写 moveToDB）后，
 * 核销只删内存不删 RocksDB 行——重启后同一 token 可从 DB 旧副本无限次重放核销。
 * 修复后：达到 maxCount 核销成功的同时删除 RocksDB 副本，重启后重放被拒（time=-1）。
 * 端到端复现：签发 → 停服落库（saveDB）→ 重启（内存空、DB 有 {count:0}）→ 核销 → 再核销。
 * 端口用 5013（避开 TestToken 的 5003，防并行车道冲突）。
 */
@Fast
public class TestTokenConsumeDeletesDb {
	@Test
	public void testConsumeDeletesDbCopy(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		System.setProperty("token.rocksdb", tempDir.resolve("token_db").toString());

		var tokenServer = new Token();
		tokenServer.start(null, null, 5013);
		var contextStr = "replay-ctx";
		String token;
		String token2;
		try {
			var tokenClient = new Token.TokenClient(null).start("127.0.0.1", 5013);
			try {
				tokenClient.waitReady();
				token = tokenClient.newToken(new Binary(contextStr), 60_000).get().getToken();

				// 部分消费场景预备：maxCount=2，先消费1次（未核销、不删库）。
				token2 = tokenClient.newToken(new Binary("partial"), 60_000).get().getToken();
				Assertions.assertTrue(tokenClient.getToken(token2, 2).get().getTime() >= 0);
			} finally {
				tokenClient.stop();
			}
		} finally {
			// 优雅停机：saveDB 把 {count:0}/{count:1} 写入 RocksDB 并清空 tokenMap（finding 的前置条件）。
			tokenServer.stop();
			tokenServer.closeDb();
		}

		// 重启同一 rocksdb 目录：内存 tokenMap 为空，token 状态仅存于 DB 旧副本。
		tokenServer.start(null, null, 5013);
		try {
			var tokenClient = new Token.TokenClient(null).start("127.0.0.1", 5013);
			try {
				tokenClient.waitReady();

				// 首次核销：DB 回载 {count:0} → count 1>=1 → 核销成功。
				var res1 = tokenClient.getToken(token, 1).get();
				Assertions.assertEquals(contextStr, res1.getContext().toString(StandardCharsets.UTF_8));
				Assertions.assertTrue(res1.getTime() >= 0);

				// 重放（修复前：DB 副本 {count:0} 再次回载 → 再次核销成功 → 重放通过，红）。
				var res2 = tokenClient.getToken(token, 1).get();
				Assertions.assertTrue(res2.getTime() < 0, "replayed token must be rejected after consume");
				Assertions.assertEquals("", res2.getContext().toString(StandardCharsets.UTF_8));
			} finally {
				tokenClient.stop();
			}
		} finally {
			tokenServer.stop();
			tokenServer.closeDb();
		}

		// 第二轮重启：token2 状态已落库 {count:1}，再核销一次（count 2>=2 核销+删库），之后重放必须被拒。
		tokenServer.start(null, null, 5013);
		try {
			var tokenClient = new Token.TokenClient(null).start("127.0.0.1", 5013);
			try {
				tokenClient.waitReady();
				var res = tokenClient.getToken(token2, 2).get();
				Assertions.assertEquals("partial", res.getContext().toString(StandardCharsets.UTF_8));
				Assertions.assertEquals(2, res.getCount());
				Assertions.assertTrue(res.getTime() >= 0);
				Assertions.assertTrue(tokenClient.getToken(token2, 2).get().getTime() < 0,
						"replayed partial-consumed token must be rejected after final consume");
			} finally {
				tokenClient.stop();
			}
		} finally {
			tokenServer.stop();
			tokenServer.closeDb();
		}
	}
}
