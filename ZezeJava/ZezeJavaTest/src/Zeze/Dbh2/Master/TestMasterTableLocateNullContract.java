package Zeze.Dbh2.Master;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import harness.Fast;

/**
 * FND28 dbh2-F1 回归：MasterTable.Data.locate 的 null 契约。
 * 修复前空表/小于首桶first的越界键在locate内对floorEntry==null的lower.getValue()直接NPE，
 * Dbh2侧（ProcessPrepareBatchRequest的拒绝路径）的null检查是死代码——异常面目替代
 * 可判定错误码eBucketNotFound（超64代裁剪后的链式重定向可使floor落空/落到新桶）。
 * tailMap同治：越界键回落key自身的tail（小于全部桶first=整表视图），不再NPE。
 */
@Fast
public class TestMasterTableLocateNullContract {

	private static Binary key(String s) {
		return new Binary(s.getBytes(StandardCharsets.UTF_8));
	}

	private static BBucketMeta.Data meta(String first, String raftConfig) {
		var m = new BBucketMeta.Data();
		m.setKeyFirst(key(first));
		m.setRaftConfig(raftConfig);
		return m;
	}

	@Test
	public void testLocateEmptyTableReturnsNull() {
		var data = new MasterTable.Data();
		assertNull(data.locate(key("any")), "空表locate必须返回null（修复前NPE）");
	}

	@Test
	public void testLocateBelowFirstBucketReturnsNull() {
		var data = new MasterTable.Data();
		data.getBuckets().put(key("m"), meta("m", "raft-1"));
		assertNull(data.locate(key("a")), "小于首桶first的越界键返回null（修复前NPE）");
		assertEquals("raft-1", data.locate(key("m")).getRaftConfig(), "命中首桶first");
		assertEquals("raft-1", data.locate(key("z")).getRaftConfig(), "大于全部桶first命中末桶");
	}

	@Test
	public void testTailMapEmptyAndBelowFirst() {
		var data = new MasterTable.Data();
		assertTrue(data.tailMap(key("a")).isEmpty(), "空表tailMap为空视图（修复前NPE）");

		var data2 = new MasterTable.Data();
		data2.getBuckets().put(key("m"), meta("m", "raft-1"));
		data2.getBuckets().put(key("x"), meta("x", "raft-2"));
		assertEquals(2, data2.tailMap(key("a")).size(), "越界键（无floor）回落key自身tail=整表视图");
		// tailMap含起点（inclusive）：floor命中m桶即从m起（m与x两桶）
		assertEquals(2, data2.tailMap(key("n")).size(), "floor命中m桶：从m起tail（含m）");
		assertEquals("raft-1", data2.tailMap(key("n")).values().iterator().next().getRaftConfig());
		assertEquals(1, data2.tailMap(key("y")).size(), "floor命中x桶：从x起tail（仅x）");
		assertEquals("raft-2", data2.tailMap(key("y")).values().iterator().next().getRaftConfig(),
				"floor命中x桶：从x起tail");
	}
}
