package UnitTest.Zeze.Transaction;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Zeze.Builtin.AccountOnline.BAccountLink;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogList2;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.PList2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Util.OutInt;

@Fast
public class TestPList2FollowerApplyOutOfBounds {
	@Test
	public void testListOutOfBoundsChangedSkipped() {
		// 纯单元：unmanaged容器直接构造，不需要应用环境。
		var plist = new PList2<>(BAccountLink.class);
		plist.add(new BAccountLink());
		plist.add(new BAccountLink());
		assertEquals(2, plist.size());

		// 正常encode侧LogList2.encode只保留最终列表中存在的bean并按最终列表计算index，
		// follower侧越界只可能来自先行分歧（日志丢失/重复/交错应用）。
		// 防御行为：warn+skip，不抛异常、列表不变。
		@SuppressWarnings("unchecked")
		var oobLog = (LogList2<BAccountLink>)plist.createLogBean();
		oobLog.getChanged().put(new LogBean(null, 0, null), new OutInt(plist.size())); // ==size，越上界
		oobLog.getChanged().put(new LogBean(null, 0, null), new OutInt(-1)); // 负index
		plist.followerApply(oobLog);
		assertEquals(2, plist.size());

		// 同一条日志内opLogs仍正常应用：OP_CLEAR生效，越界changed只跳过自己。
		@SuppressWarnings("unchecked")
		var mixedLog = (LogList2<BAccountLink>)plist.createLogBean();
		mixedLog.clear();
		mixedLog.getChanged().put(new LogBean(null, 0, null), new OutInt(99));
		plist.followerApply(mixedLog);
		assertTrue(plist.isEmpty());
	}

	@Test
	public void testMapChangedKeyMissingSkipped() {
		var pmap = new PMap2<>(Long.class, BAccountLink.class);

		// 正常encode侧LogMap2.buildChangedWithKey只保留最终map存在的key，
		// follower侧取不到key只可能来自先行分歧。防御行为：warn+skip，不抛异常。
		// （replaced正常安装路径需要map-capable bean，由TestMap2FollowerApplyMapKey等覆盖。）
		@SuppressWarnings("unchecked")
		var missLog = (LogMap2<Long, BAccountLink>)pmap.createLogBean();
		missLog.getChangedWithKey().put(12345L, new LogBean(null, 0, null));
		pmap.followerApply(missLog);
		assertEquals(0, pmap.size());
	}
}
