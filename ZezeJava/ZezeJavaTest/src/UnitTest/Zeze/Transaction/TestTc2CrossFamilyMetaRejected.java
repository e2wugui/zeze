package UnitTest.Zeze.Transaction;

import Zeze.Transaction.Collections.Meta2;
import Zeze.Transaction.Collections.PMap1;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSortedMap1;
import Zeze.Transaction.Collections.PSortedMap2;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * TC2-F1回归：PMap1/PMap2/PSortedMap1/PSortedMap2 的 Meta2 构造器接受跨家族 meta 无防护。
 * 两边同为 Meta2&lt;K,V&gt;，编译期不拦；该入口整体旁路工厂层 checkNonBeanKey/checkNonBeanValue1
 * 防线，反家族 meta 使写端 typeId 借道对家注册，读端解码出对方家族的 Log，本家 followerApply
 * 强转 ClassCastException（raft 路径 fatalKill）。同族错误 FND3-04 历史上真实发生过。
 * 修复：Meta2 携带家族标记（日志头前缀），四个 Meta2 构造器运行时断言家族匹配。
 */
@Fast
public class TestTc2CrossFamilyMetaRejected {

	@Test
	public void testCrossFamilyRejected() {
		// PMap2 误收 sortedMap2 家族 meta（裁决修订点：与 PSortedMap1/2 逐字同型同洞，同批覆盖）。
		var sortedMeta2 = Meta2.getSortedMap2Meta(Long.class, BValue.class);
		Assertions.assertThrows(IllegalArgumentException.class, () -> new PMap2<>(sortedMeta2),
				"PMap2必须拒绝sortedMap2家族meta");

		// PSortedMap2 误收 map2 家族 meta（工单原始场景）。
		var mapMeta2 = Meta2.getMap2Meta(Long.class, BValue.class);
		Assertions.assertThrows(IllegalArgumentException.class, () -> new PSortedMap2<>(mapMeta2),
				"PSortedMap2必须拒绝map2家族meta");

		// 1系互为镜像的同型洞。
		var sortedMeta1 = Meta2.getSortedMap1Meta(Long.class, String.class);
		Assertions.assertThrows(IllegalArgumentException.class, () -> new PMap1<>(sortedMeta1),
				"PMap1必须拒绝sortedMap1家族meta");

		var mapMeta1 = Meta2.getMap1Meta(Long.class, String.class);
		Assertions.assertThrows(IllegalArgumentException.class, () -> new PSortedMap1<>(mapMeta1),
				"PSortedMap1必须拒绝map1家族meta");

		// 动态 meta 同样按家族判定（FND3-04 历史形态：sortedMap 动态家族头误入 map 家族）。
		var sortedDynMeta = Meta2.createDynamicSortedMapMeta(Long.class, b -> 0L, id -> new BValue());
		Assertions.assertThrows(IllegalArgumentException.class, () -> new PMap2<>(sortedDynMeta),
				"PMap2必须拒绝sortedMap2动态家族meta");

		var mapDynMeta = Meta2.createDynamicMapMeta(Long.class, b -> 0L, id -> new BValue());
		Assertions.assertThrows(IllegalArgumentException.class, () -> new PSortedMap2<>(mapDynMeta),
				"PSortedMap2必须拒绝map2动态家族meta");
	}

	@Test
	public void testSameFamilyStillWork() {
		// 正控：同家族 meta 经 Meta2 构造器（copy()/GTable 等生产路径形态）不受影响。
		Assertions.assertNotNull(new PMap2<>(Meta2.getMap2Meta(Long.class, BValue.class)).copy());
		Assertions.assertNotNull(new PMap2<>(Meta2.createMap2Meta(Long.class, BValue.class, BValue::new)).copy());
		Assertions.assertNotNull(new PMap2<>(Meta2.createDynamicMapMeta(Long.class, b -> 0L, id -> new BValue())).copy());
		Assertions.assertNotNull(new PSortedMap2<>(Meta2.getSortedMap2Meta(Long.class, BValue.class)).copy());
		Assertions.assertNotNull(new PSortedMap2<>(Meta2.createSortedMap2Meta(Long.class, BValue.class, BValue::new)).copy());
		Assertions.assertNotNull(new PSortedMap2<>(Meta2.createDynamicSortedMapMeta(Long.class, b -> 0L, id -> new BValue())).copy());
		Assertions.assertNotNull(new PMap1<>(Meta2.getMap1Meta(Long.class, String.class)).copy());
		Assertions.assertNotNull(new PSortedMap1<>(Meta2.getSortedMap1Meta(Long.class, String.class)).copy());
	}
}
