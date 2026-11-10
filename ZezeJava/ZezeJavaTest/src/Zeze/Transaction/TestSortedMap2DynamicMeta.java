package Zeze.Transaction;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Map2Meta;
import Zeze.Transaction.Collections.PSortedMap2;
import Zeze.Transaction.Collections.SortedMap2Meta;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND3-04 回归：sortedmap&lt;K,dynamic&gt; 的 meta 必须落在 sortedMap2 家族头哈希上，
 * 读写端的 typeId 都使用 LogSortedMap2 家族。
 * 修复前动态构造器用 map2 家族哈希：回放端 Log.create 查不到（unknown typeId），
 * 或借道同 keyClass 的 map&lt;K,dynamic&gt; 注册解出 LogMap2，followerApply 强转抛 CCE；
 * 且与 map&lt;K,dynamic&gt; 共享 typeId（FND-T4-2 的 map/sortedmap 碰撞对），修复后此对消除。
 */
@Fast
public class TestSortedMap2DynamicMeta {
	private abstract static class Fields extends Bean {
		private static final long TYPE_ID = Bean.hash64(Fields.class.getName());
		private static final DynamicField SORTED =
				DynamicField.declare(Fields.class, TYPE_ID, 1, b -> 1L, id -> null);
		private static final DynamicField MAP =
				DynamicField.declare(Fields.class, TYPE_ID, 2, b -> 1L, id -> null);
	}

	@Test
	public void testDynamicSortedMapUsesSortedMap2Family() {
		var smeta = new PSortedMap2<>(SortedMap2Meta.createDynamic(Long.class, Fields.SORTED)).getMeta();
		Assertions.assertEquals(Bean.hashLog(Bean.hash64("Zeze.Transaction.Collections.LogSortedMap2<"),
				Long.class, DynamicBean.class), smeta.logTypeId);

		// 不再与同 keyClass 的 map<K,dynamic> 碰撞（logTypeId 与 name 都分家）
		var mapMeta = Map2Meta.createDynamic(Long.class, Fields.MAP);
		Assertions.assertNotEquals(mapMeta.logTypeId, smeta.logTypeId);
		Assertions.assertNotEquals(mapMeta.name, smeta.name);
	}
}
