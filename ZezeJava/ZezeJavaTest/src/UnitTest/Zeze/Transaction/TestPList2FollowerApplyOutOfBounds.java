package UnitTest.Zeze.Transaction;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import Zeze.Builtin.Provider.BLoad;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogList2;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.PList2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Util.OutInt;

@Fast
public class TestPList2FollowerApplyOutOfBounds {
	@Test
	public void testListOutOfBoundsChangedThrows() {
		// 纯单元：unmanaged容器直接构造，不需要应用环境。
		var plist = new PList2<>(BLoad.class);
		plist.add(new BLoad());
		plist.add(new BLoad());
		assertEquals(2, plist.size());

		// 正常encode侧LogList2.encode只保留最终列表中存在的bean并按最终列表计算index，
		// follower侧越界只可能来自先行分歧（日志丢失/重复/交错应用）：直接get抛
		// IndexOutOfBoundsException，由驱动方裁决（raft路径Rocks.followerApply统一catch
		// +fatalKill；History回放路径批中断）。抛出时list未提交，容器保持原状。
		@SuppressWarnings("unchecked")
		var oobLog = (LogList2<BLoad>)plist.createLogBean();
		oobLog.getChanged().put(new LogBean(null, 0, null), new OutInt(plist.size())); // ==size，越上界
		assertThrows(IndexOutOfBoundsException.class, () -> plist.followerApply(oobLog));
		assertEquals(2, plist.size());

		// 同一条日志内opLogs先于changed应用，changed抛出时整个应用中止：tmp不提交回list
		//（列表不变），不存在"越界changed只跳过自己、opLogs仍生效"的部分应用。
		@SuppressWarnings("unchecked")
		var mixedLog = (LogList2<BLoad>)plist.createLogBean();
		mixedLog.clear();
		mixedLog.getChanged().put(new LogBean(null, 0, null), new OutInt(99));
		assertThrows(IndexOutOfBoundsException.class, () -> plist.followerApply(mixedLog));
		assertEquals(2, plist.size());
	}

	@Test
	public void testMapChangedKeyMissingThrows() {
		var pmap = new PMap2<>(Long.class, BLoad.class);

		// 正常encode侧LogMap2.buildChangedWithKey只保留最终map存在的key，
		// follower侧取不到key只可能来自先行分歧：直接递归抛NPE。
		// （replaced正常安装路径需要map-capable bean，由TestMap2FollowerApplyMapKey等覆盖。）
		@SuppressWarnings("unchecked")
		var missLog = (LogMap2<Long, BLoad>)pmap.createLogBean();
		missLog.getChangedWithKey().put(12345L, new LogBean(null, 0, null));
		assertThrows(NullPointerException.class, () -> pmap.followerApply(missLog));
		assertEquals(0, pmap.size());
	}
}
