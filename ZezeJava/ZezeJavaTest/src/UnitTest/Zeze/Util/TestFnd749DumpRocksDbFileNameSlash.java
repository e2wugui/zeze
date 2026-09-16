package UnitTest.Zeze.Util;

import Zeze.Util.DumpRocksDb;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-49回归：部分RocksDB版本的getLiveFilesMetaData().fileName带前导'/'，
 * compactFiles要求相对db目录的文件名——meta分支已剥离而compact1分支漏剥，
 * 带斜杠版本上执行-compact1时fileList全部miss、工具报错退出。
 * 修复：抽出stripLeadingSlash，两个分支统一使用。
 * 说明：fileName是否带'/'由rocksdb native版本决定，无法在单测内构造带斜杠的
 * LiveFileMetaData；此处直接钉住剥离契约（meta分支原有行为）。
 */
@Fast
public class TestFnd749DumpRocksDbFileNameSlash {

	@Test
	public void testStripLeadingSlash() throws Exception {
		var m = DumpRocksDb.class.getDeclaredMethod("stripLeadingSlash", String.class);
		m.setAccessible(true);

		Assertions.assertEquals("000123.sst", m.invoke(null, "/000123.sst"), "带前导斜杠必须剥离");
		Assertions.assertEquals("000123.sst", m.invoke(null, "000123.sst"), "无斜杠保持不变");
		Assertions.assertEquals("", m.invoke(null, "/"), "单独斜杠剥离为空");
		Assertions.assertEquals("a/b.sst", m.invoke(null, "/a/b.sst"), "只剥前导，内部路径分隔保留");
	}
}
