package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.CollOne;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogOne;
import Zeze.Transaction.Collections.LogOneMeta;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSortedMap2;
import Zeze.Transaction.Logs.LogLong;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-29回归：map2/dynamic 值日志缺身份校验。
 * put覆盖（或setBean换bean）不解除旧bean归属：旧bean仍managed、mapKey不清，
 * 用户持有陈旧引用继续修改是合法调用序列；旧实现只有 key 级过滤
 * （replaced/removed/存在性），仍可能放行错误的值身份，
 * follower 侧被应用到覆盖后的新值上，主从静默分歧。
 * 修复：收集阶段要求 child.getThis()==source.map.get(k)（LogMap2/LogSortedMap2），
 * LogDynamic.addChild 要求 logBean.getThis()==当前内部bean。
 * 对照组：LogList2 历来就有身份过滤（v==bean 逐位搜索），本修复对齐家族语义。
 * <p>
 * 纯单元：不起应用/网络，为 Map2 构造合法源关系并真实 collect 陈旧字段日志，
 * Dynamic 同样经 addChild 收集，再走 encode→decode→followerApply
 * 全路径断言。修复前 changedWithKey 携带陈旧日志、follower 的当前值被污染。
 */
@Fast
public class TestStaleBeanChangedFilter {
	static {
		// decodeLogBean -> LogRegistry.decode(typeId) 需要 log 工厂（生产路径由模块装载注册）。
		Zeze.Transaction.LogRegistry.register(LogLong.TYPE_ID, "LogLong", LogLong::decode);
	}

	// varId=2 是 BValue.long2；伪造"更早事务装入的bean的字段日志"。
	private static LogBean newBeanLog(BValue bean, long long2) {
		var logBean = new LogBean(0, bean);
		var varLog = Map2TestSupport.longLog(2, long2);
		logBean.getVariablesOrNew().put(2, varLog);
		return logBean;
	}

	private static ByteBuffer encodeThenWrap(Log log) {
		var bb = ByteBuffer.Allocate(256);
		log.encode(bb);
		return ByteBuffer.Wrap(bb.Bytes, bb.ReadIndex, bb.size());
	}

	@Test
	public void testLogMap2StaleChangedFiltered() {
		// master侧：put覆盖后 map.get("k")==current，stale是被顶替的旧bean。
		var stale = new BValue();
		stale.mapKey("k");
		var current = new BValue();
		var map = new PMap2<String, BValue>(String.class, BValue.class);
		map.put("k", current);
		@SuppressWarnings("unchecked")
		var log = (LogMap2<String, BValue>)Map2TestSupport.collect(map, newBeanLog(stale, 9));

		var followerValue = new BValue();
		var follower = new PMap2<String, BValue>(String.class, BValue.class);
		follower.put("k", followerValue);
		@SuppressWarnings("unchecked")
		var decoded = Map2TestSupport.wireLog(follower);
		decoded = Zeze.Transaction.Map2TestSupport.decode(decoded, encodeThenWrap(log));
		assertTrue(decoded.getChangedWithKey().isEmpty(), "陈旧引用的changed必须被身份校验过滤");
		follower.followerApply(decoded);
		assertEquals(0, followerValue.getLong2(), "follower当前值不得被陈旧字段日志污染");

		// 正控：当前值自身的changed保留并正常应用。
		@SuppressWarnings("unchecked")
		var live = (LogMap2<String, BValue>)Map2TestSupport.collect(map, newBeanLog(current, 7));
		@SuppressWarnings("unchecked")
		var decodedLive = Map2TestSupport.wireLog(follower);
		decodedLive = Zeze.Transaction.Map2TestSupport.decode(decodedLive, encodeThenWrap(live));
		assertTrue(decodedLive.getChangedWithKey().containsKey("k"), "当前值的changed不得误杀");
		follower.followerApply(decodedLive);
		assertEquals(7, followerValue.getLong2());
	}

	@Test
	public void testLogSortedMap2StaleChangedFiltered() {
		var stale = new BValue();
		stale.mapKey("k");
		var current = new BValue();
		var map = new PSortedMap2<String, BValue>(String.class, BValue.class);
		map.put("k", current);
		@SuppressWarnings("unchecked")
		var log = (LogSortedMap2<String, BValue>)Map2TestSupport.collect(map, newBeanLog(stale, 9));

		var followerValue = new BValue();
		var follower = new PSortedMap2<String, BValue>(String.class, BValue.class);
		follower.put("k", followerValue);
		@SuppressWarnings("unchecked")
		var decoded = Map2TestSupport.wireLog(follower);
		decoded = Zeze.Transaction.Map2TestSupport.decode(decoded, encodeThenWrap(log));
		assertTrue(decoded.getChangedWithKey().isEmpty(), "陈旧引用的changed必须被身份校验过滤");
		follower.followerApply(decoded);
		assertEquals(0, followerValue.getLong2(), "follower当前值不得被陈旧字段日志污染");

		@SuppressWarnings("unchecked")
		var live = (LogSortedMap2<String, BValue>)Map2TestSupport.collect(map, newBeanLog(current, 7));
		@SuppressWarnings("unchecked")
		var decodedLive = Map2TestSupport.wireLog(follower);
		decodedLive = Zeze.Transaction.Map2TestSupport.decode(decodedLive, encodeThenWrap(live));
		assertTrue(decodedLive.getChangedWithKey().containsKey("k"), "当前值的changed不得误杀");
		follower.followerApply(decodedLive);
		assertEquals(7, followerValue.getLong2());
	}

	@Test
	public void testLogDynamicStaleLogBeanFiltered() {
		var parent = new BValue();
		var dyn = new DynamicBean(14, b -> 1, id -> new BValue());
		// encode 需要parent链：手工搭managed状态（TestCollOneFollowerApply 的桩Record范式）。
		dyn.initRootInfo(new Record.RootInfo(newStubRecord(), new TableKey(1, "StaleBeanDynamic")), parent);

		var staleInner = new BValue(); // 更早事务 setBean 装入的旧内部bean
		var currentInner = new BValue(); // 本事务前 setBean 替换上的新内部bean
		dyn.bean = currentInner;
		var log = new LogDynamic(dyn, 14, dyn); // 本事务未 setBean：value==null，logBean 分支
		log.addChild(dyn, newBeanLog(staleInner, 9)); // txn3：陈旧引用的修改被collect
		assertNull(log.logBean, "陈旧内部bean的字段日志必须在收集阶段过滤");

		var decoded = LogDynamicValue.decode(0, encodeThenWrap(log));
		assertNull(decoded.logBean, "陈旧内部bean的字段日志必须被身份校验过滤");

		var followerInner = new BValue();
		var followerDyn = new DynamicBean(14, b -> 1, id -> new BValue());
		followerDyn.bean = followerInner;
		followerDyn.followerApply(decoded);
		assertEquals(0, followerInner.getLong2(), "follower当前内部bean不得被陈旧字段日志污染");

		// 正控：当前内部bean自身的logBean保留并正常应用。
		var live = new LogDynamic(dyn, 14, dyn);
		live.addChild(dyn, newBeanLog(currentInner, 7));
		var decodedLive = LogDynamicValue.decode(0, encodeThenWrap(live));
		assertNotNull(decodedLive.logBean, "当前内部bean的logBean不得误杀");
		followerDyn.followerApply(decodedLive);
		assertEquals(7, followerInner.getLong2());
	}

	@Test
	public void testLogOneStaleLogBeanFiltered() {
		Zeze.History.Helper.registerLogOne(BValue.class);
		var parent = new BValue();
		var staleInner = new BValue(); // 更早事务 setValue 替换掉的旧值
		var currentInner = new BValue(); // 当前受管值
		var collOne = new CollOne<>(currentInner, LogOneMeta.get(BValue.class));
		collOne.initRootInfo(new Record.RootInfo(newStubRecord(), new TableKey(1, "StaleBeanCollOne")), parent);

		// 仅修改陈旧引用：createLogBean(null) 产生增量节点，addChild 必须过滤非当前实例。
		@SuppressWarnings("unchecked")
		var log = (LogOne<BValue>)collOne.createLogBean(null);
		assertTrue(log.addChild(collOne, newBeanLog(staleInner, 9)));
		var decoded = LogRegistry.decode(log.getTypeId(), 0, encodeThenWrap(log));

		var followerParent = new BValue();
		var followerInner = new BValue();
		var followerCollOne = new CollOne<>(followerInner, LogOneMeta.get(BValue.class));
		followerCollOne.initRootInfo(new Record.RootInfo(newStubRecord(), new TableKey(1, "StaleBeanCollOneFollower")), followerParent);
		followerCollOne.followerApply(decoded);
		assertSame(followerInner, followerCollOne.getValue(), "陈旧增量不得替换follower当前bean");
		assertEquals(0, followerInner.getLong2(), "follower当前内部bean不得被陈旧字段日志污染");

		// 正控（且同节点先遇陈旧引用后遇当前引用）：陈旧引用不得占坑，当前内部bean的logBean必须保留并原位应用。
		assertTrue(log.addChild(collOne, newBeanLog(currentInner, 7)));
		var decodedLive = LogRegistry.decode(log.getTypeId(), 0, encodeThenWrap(log));
		followerCollOne.followerApply(decodedLive);
		assertSame(followerInner, followerCollOne.getValue(), "原位增量应保持follower当前bean身份");
		assertEquals(7, followerInner.getLong2());
	}

	private static Record newStubRecord() {
		return new Record(null) {
			@Override
			public Table getTable() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Object getObjectKey() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setDirty() {
			}

			@Override
			public IGlobalAgent.AcquireResult acquire(int state, boolean fresh, boolean noWait) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void commit(RecordAccessed accessed) {
			}
		};
	}
}
