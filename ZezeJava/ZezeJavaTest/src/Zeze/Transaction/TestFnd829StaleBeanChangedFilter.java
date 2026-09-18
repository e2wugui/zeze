package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogMap2;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-29回归：map2/dynamic 值日志缺身份校验。
 * put覆盖（或setBean换bean）不解除旧bean归属：旧bean仍managed、mapKey不清，
 * 用户持有陈旧引用继续修改是合法调用序列，其字段日志会被collect并经
 * buildChangedWithKey/encode 的 key 级过滤（replaced/removed/存在性）放行，
 * follower 侧被应用到覆盖后的新值上，主从静默分歧。
 * 修复：第三条过滤强化为 c.getThis()==getValue().get(k)（LogMap2/LogSortedMap2），
 * LogDynamic.encode 的 logBean 分支补 logBean.getThis()==当前内部bean。
 * 对照组：LogList2 历来就有身份过滤（v==bean 逐位搜索），本修复对齐家族语义。
 * <p>
 * 纯单元：不起应用/网络，直接构造 LogMap2/LogSortedMap2/LogDynamic，
 * 手工注入"陈旧bean的字段日志"到 changed/logBean，走 encode→decode→followerApply
 * 全路径断言。修复前 changedWithKey 携带陈旧日志、follower 的当前值被污染。
 */
@Fast
public class TestFnd829StaleBeanChangedFilter {
	static {
		// decodeLogBean -> Log.create(typeId) 需要 log 工厂（生产路径由模块装载注册）。
		Log.register(LogLong::new);
	}

	// varId=2 是 BValue.long2；伪造"更早事务装入的bean的字段日志"。
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
	public void testLogMap2StaleChangedFiltered() {
		// master侧：put覆盖后 map.get("k")==current，stale是被顶替的旧bean。
		var stale = new BValue();
		stale.mapKey("k");
		var current = new BValue();
		var map = new PMap2<String, BValue>(String.class, BValue.class);
		map.put("k", current);
		var log = (LogMap2<String, BValue>)map.createLogBean();
		log.getChanged().add(newBeanLog(stale, 9)); // txn3：陈旧引用的修改被collect

		var followerValue = new BValue();
		var follower = new PMap2<String, BValue>(String.class, BValue.class);
		follower.put("k", followerValue);
		var decoded = (LogMap2<String, BValue>)follower.createLogBean();
		decoded.decode(encodeThenWrap(log));
		assertTrue(decoded.getChangedWithKey().isEmpty(), "陈旧引用的changed必须被身份校验过滤");
		follower.followerApply(decoded);
		assertEquals(0, followerValue.getLong2(), "follower当前值不得被陈旧字段日志污染");

		// 正控：当前值自身的changed保留并正常应用。
		var live = (LogMap2<String, BValue>)map.createLogBean();
		live.getChanged().add(newBeanLog(current, 7));
		var decodedLive = (LogMap2<String, BValue>)follower.createLogBean();
		decodedLive.decode(encodeThenWrap(live));
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
		var log = (LogSortedMap2<String, BValue>)map.createLogBean();
		log.getChanged().add(newBeanLog(stale, 9));

		var followerValue = new BValue();
		var follower = new PSortedMap2<String, BValue>(String.class, BValue.class);
		follower.put("k", followerValue);
		var decoded = (LogSortedMap2<String, BValue>)follower.createLogBean();
		decoded.decode(encodeThenWrap(log));
		assertTrue(decoded.getChangedWithKey().isEmpty(), "陈旧引用的changed必须被身份校验过滤");
		follower.followerApply(decoded);
		assertEquals(0, followerValue.getLong2(), "follower当前值不得被陈旧字段日志污染");

		var live = (LogSortedMap2<String, BValue>)map.createLogBean();
		live.getChanged().add(newBeanLog(current, 7));
		var decodedLive = (LogSortedMap2<String, BValue>)follower.createLogBean();
		decodedLive.decode(encodeThenWrap(live));
		assertTrue(decodedLive.getChangedWithKey().containsKey("k"), "当前值的changed不得误杀");
		follower.followerApply(decodedLive);
		assertEquals(7, followerValue.getLong2());
	}

	@Test
	public void testLogDynamicStaleLogBeanFiltered() {
		var parent = new BValue();
		var dyn = new DynamicBean(14, b -> 1, id -> new BValue());
		// encode 需要parent链：手工搭managed状态（TestCollOneFollowerApply 的桩Record范式）。
		dyn.initRootInfo(new Record.RootInfo(newStubRecord(), new TableKey(1, "a3Fnd829Dynamic")), parent);

		var staleInner = new BValue(); // 更早事务 setBean 装入的旧内部bean
		var currentInner = new BValue(); // 本事务前 setBean 替换上的新内部bean
		dyn.bean = currentInner;
		var log = new LogDynamic(dyn, 14, dyn); // 本事务未 setBean：value==null，logBean 分支
		log.logBean = newBeanLog(staleInner, 9); // txn3：陈旧引用的修改被collect

		var decoded = new LogDynamic(null, 0, null);
		decoded.decode(encodeThenWrap(log));
		assertNull(decoded.logBean, "陈旧内部bean的字段日志必须被身份校验过滤");

		var followerInner = new BValue();
		var followerDyn = new DynamicBean(14, b -> 1, id -> new BValue());
		followerDyn.bean = followerInner;
		followerDyn.followerApply(decoded);
		assertEquals(0, followerInner.getLong2(), "follower当前内部bean不得被陈旧字段日志污染");

		// 正控：当前内部bean自身的logBean保留并正常应用。
		var live = new LogDynamic(dyn, 14, dyn);
		live.logBean = newBeanLog(currentInner, 7);
		var decodedLive = new LogDynamic(null, 0, null);
		decodedLive.decode(encodeThenWrap(live));
		assertNotNull(decodedLive.logBean, "当前内部bean的logBean不得误杀");
		followerDyn.followerApply(decodedLive);
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
			public void encode0() {
			}

			@Override
			public void flush(Database.Transaction t, Database.Transaction lct) {
			}

			@Override
			public void commit(RecordAccessed accessed) {
			}

			@Override
			public void cleanup() {
			}
		};
	}
}
