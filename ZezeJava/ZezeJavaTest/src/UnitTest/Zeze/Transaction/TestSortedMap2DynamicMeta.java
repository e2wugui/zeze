package UnitTest.Zeze.Transaction;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Meta2;
import Zeze.Transaction.Collections.PSortedMap2;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND3-04 回归：sortedmap&lt;K,dynamic&gt; 的 meta 必须落在 sortedMap2 家族头哈希上，
 * 写端 typeId 与读端 Helper.registerLogSortedMap2Dynamic 的注册键对称。
 * 修复前动态构造器用 map2 家族哈希：回放端 Log.create 查不到（unknown typeId），
 * 或借道同 keyClass 的 map&lt;K,dynamic&gt; 注册解出 LogMap2，followerApply 强转抛 CCE；
 * 且与 map&lt;K,dynamic&gt; 共享 typeId（FND-T4-2 的 map/sortedmap 碰撞对），修复后此对消除。
 */
@Fast
public class TestSortedMap2DynamicMeta {
	private static final ToLongFunction<Bean> GET = b -> 1L;
	private static final LongFunction<Bean> CREATE = t -> null;

	@Test
	public void testDynamicSortedMapUsesSortedMap2Family() {
		var smeta = new PSortedMap2<Long, Bean>(Long.class, GET, CREATE).getMeta();
		var expected = Meta2.createDynamicSortedMapMeta(Long.class, GET, CREATE);
		Assertions.assertEquals(expected.logTypeId, smeta.logTypeId);
		Assertions.assertEquals(expected.name, smeta.name);

		// 不再与同 keyClass 的 map<K,dynamic> 碰撞（logTypeId 与 name 都分家）
		var mapMeta = Meta2.createDynamicMapMeta(Long.class, GET, CREATE);
		Assertions.assertNotEquals(mapMeta.logTypeId, smeta.logTypeId);
		Assertions.assertNotEquals(mapMeta.name, smeta.name);
	}
}
