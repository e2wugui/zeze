package UnitTest.Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSortedMap2;
import Zeze.Transaction.Log;
import Zeze.Transaction.Logs.LogLong;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC1-F1/F2回归：LogMap2/LogSortedMap2 的 mergeChangedToReplaced 复用 encode 的 built 门闩。
 * History 开启时 finalCommit 的 collect 阶段先 encode（置 built=true、构建 changedWithKey），
 * 随后 notifyListener 的监听器调用 mergeChangedToReplaced 被 buildChangedWithKey 短路成 no-op，
 * getReplaced() 缺失本事务的原位修改条目，增量通知静默丢失（History=false 时同一监听器正常）。
 * 修复：引入独立 merged 标志，merge 不再依赖 built。
 * <p>
 * 纯单元（FND8-29 判例范式）：不起应用/History，直接按真实时序（encode 先行 → merge）驱动
 * 日志对象，近似覆盖 History 链路的调用顺序。
 */
@Fast
public class TestTc1MergeChangedAfterEncode {
	static {
		// decodeLogBean -> Log.create(typeId) 需要 log 工厂（生产路径由模块装载注册）。
		Log.register(LogLong::new);
	}

	// varId=2 是 BValue.long2；模拟监听器范式（TestChangeListener.CLMap11）：原位修改已存在的 value。
	private static LogBean newBeanLog(BValue bean, long long2) {
		var logBean = new LogBean(null, 0, bean);
		var varLog = new LogLong(2);
		varLog.value = long2;
		logBean.getVariablesOrNew().put(2, varLog);
		return logBean;
	}

	private static ByteBuffer encodeThenWrap(Log log) {
		var bb = ByteBuffer.Allocate(256);
		log.encode(bb);
		return ByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
	}

	@Test
	public void testMap2MergeAfterEncode() {
		// 模拟 History=true 时序：collect 阶段 encode 先行（built=true），监听器阶段才 merge。
		var value = new BValue();
		var map = new PMap2<String, BValue>(String.class, BValue.class);
		map.put("k", value); // put 设置 mapKey("k")，buildChangedWithKey 依赖
		value.setLong2(7); // 原位字段修改：bean 自身已持新值，changed 只是对它的日志
		var log = (LogMap2<String, BValue>)map.createLogBean();
		log.getChanged().add(newBeanLog(value, 7));

		var bb = ByteBuffer.Allocate(256);
		log.encode(bb); // collect 阶段：encode 先行置 built=true

		log.mergeChangedToReplaced(); // 监听器阶段：修复前被 built 短路成 no-op
		assertTrue(log.getReplaced().containsKey("k"), "encode 先行后 merge 仍须把原位修复合入 replaced");
		assertSame(value, log.getReplaced().get("k"));
		assertEquals(7, log.getReplaced().get("k").getLong2());
	}

	@Test
	public void testSortedMap2MergeAfterEncode() {
		var value = new BValue();
		var map = new PSortedMap2<String, BValue>(String.class, BValue.class);
		map.put("k", value);
		value.setLong2(7);
		var log = (LogSortedMap2<String, BValue>)map.createLogBean();
		log.getChanged().add(newBeanLog(value, 7));

		var bb = ByteBuffer.Allocate(256);
		log.encode(bb); // collect 阶段：encode 先行置 built=true

		log.mergeChangedToReplaced(); // 监听器阶段：修复前被 built 短路成 no-op
		assertTrue(log.getReplaced().containsKey("k"), "encode 先行后 merge 仍须把原位修复合入 replaced");
		assertSame(value, log.getReplaced().get("k"));
		assertEquals(7, log.getReplaced().get("k").getLong2());
	}

	// 反方向守卫（缺陷是单向的）：先 merge 后 encode 时 replaced 已含条目、重建过滤后等价表示，
	// follower 重放不得丢数据。
	@Test
	public void testMap2MergeThenEncodeReplayEquivalent() {
		var value = new BValue();
		var map = new PMap2<String, BValue>(String.class, BValue.class);
		map.put("k", value);
		value.setLong2(7);
		var log = (LogMap2<String, BValue>)map.createLogBean();
		log.getChanged().add(newBeanLog(value, 7));

		log.mergeChangedToReplaced();
		assertTrue(log.getReplaced().containsKey("k"));

		var followerValue = new BValue();
		var follower = new PMap2<String, BValue>(String.class, BValue.class);
		follower.put("k", followerValue);
		var decoded = (LogMap2<String, BValue>)follower.createLogBean();
		decoded.decode(encodeThenWrap(log)); // merge 后再 encode：changed 并入 replaced，等价表示
		assertTrue(decoded.getChangedWithKey().isEmpty(), "已并入 replaced 的 changed 不得重复编码");
		follower.followerApply(decoded);
		assertEquals(7, follower.get("k").getLong2(), "merge 后 encode 的重放不得丢原位修改");
	}

	@Test
	public void testSortedMap2MergeThenEncodeReplayEquivalent() {
		var value = new BValue();
		var map = new PSortedMap2<String, BValue>(String.class, BValue.class);
		map.put("k", value);
		value.setLong2(7);
		var log = (LogSortedMap2<String, BValue>)map.createLogBean();
		log.getChanged().add(newBeanLog(value, 7));

		log.mergeChangedToReplaced();
		assertTrue(log.getReplaced().containsKey("k"));

		var followerValue = new BValue();
		var follower = new PSortedMap2<String, BValue>(String.class, BValue.class);
		follower.put("k", followerValue);
		var decoded = (LogSortedMap2<String, BValue>)follower.createLogBean();
		decoded.decode(encodeThenWrap(log));
		assertTrue(decoded.getChangedWithKey().isEmpty(), "已并入 replaced 的 changed 不得重复编码");
		follower.followerApply(decoded);
		assertEquals(7, follower.get("k").getLong2(), "merge 后 encode 的重放不得丢原位修改");
	}
}
