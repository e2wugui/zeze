package UnitTest.Zeze.Util;

import java.nio.file.Path;
import Zeze.Util.Cache;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Cache.close() 必须幂等：二次调用不得在 db.close() 处 NPE。
 * close 后 db 置 null，重入时裸调用 db.close() 即 NPE——停机拆卸（Application.stop 各步
 * 异常隔离后仍可能重走）与使用方清理路径重复关缓存时直接崩，掩盖真正的停机错误。
 * 修复：null 快照判定已关闭即返回。
 */
@Fast
public class TestCacheCloseIdempotent {

	@Test
	public void testCloseTwiceIsIdempotent(@TempDir Path tempDir) throws Exception {
		var dir = tempDir.resolve("cache-close-idempotent").toString();
		var cache = new Cache(dir, 16,
				id -> new TestCacheAppendTodayResume.Obj(id, "v1"),
				(id, bb) -> {
					var o = new TestCacheAppendTodayResume.Obj();
					o.decode(bb);
					return o;
				});
		Assertions.assertNotNull(cache.get("k1"));
		cache.close();
		// 二次close必须幂等：修复前此处 db==null 直接 NPE。
		Assertions.assertDoesNotThrow(cache::close, "close必须可重入（幂等）");
		// close后get仍按既定契约明确失败，而非NPE。
		Assertions.assertThrows(IllegalStateException.class, () -> cache.get("k1"));
	}
}
