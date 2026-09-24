package UnitTest.Zeze.Transaction;

import Zeze.History.Helper;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.CollOne;
import Zeze.Transaction.Collections.LogOne;
import Zeze.Transaction.Collections.LogOneMeta;
import Zeze.Transaction.Database;
import Zeze.Transaction.IGlobalAgent;
import Zeze.Transaction.Log;
import Zeze.Transaction.Record;
import Zeze.Transaction.RecordAccessed;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableKey;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-80：LogOne写端用value.getClass()（运行时类）建meta计算typeId，而读端工厂按
 * 声明类注册（History.Helper.dependsBean→registerLogOne遍历表值bean的声明依赖集）。
 * CollOne装入声明类型的子类实例时typeId=hash(LogOne<子类>)，follower Log.create抛
 * UnsupportedOperationException，复制/回放中断（fail-fast非静默）。修复：CollOne保存
 * 原被忽略的valueClass参数，LogOne建meta优先用宿主CollOne携带的声明类；精确类型
 * typeId不变。
 */
@Fast
public class TestFnd780LogOneDeclaredTypeMeta {

	/** 声明类型：含一个int变量，encode/decode可往返。 */
	public static class BeanBase extends Bean {
		public int x;

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteInt(x);
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			x = bb.ReadInt();
		}

		@Override
		public @NotNull Bean copy() {
			var c = new BeanBase();
			c.x = x;
			return c;
		}
	}

	/** 声明类型的子类：装入CollOne<BeanBase>即触发typeId不对称。 */
	public static class BeanSub extends BeanBase {
	}

	// 纯单元：手工构造managed状态（正常由表记录初始化），仿TestCollOneFollowerApply。
	private static Record.RootInfo newRootInfo() {
		var record = new Record(null) {
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
		return new Record.RootInfo(record, new TableKey(1, "TestFnd780"));
	}

	@Test
	public void testSubclassValueUsesDeclaredType() {
		var parent = new BeanBase();
		var sub = new BeanSub();
		sub.x = 42;
		var collOne = new CollOne<>((BeanBase)sub, BeanBase.class);
		collOne.initRootInfo(newRootInfo(), parent);
		Assertions.assertTrue(collOne.isManaged());

		@SuppressWarnings("unchecked")
		var log = (LogOne<BeanBase>)collOne.createLogBean();
		Assertions.assertEquals(LogOneMeta.get(BeanBase.class).logTypeId, log.getTypeId(),
				"子类实例必须按声明类计算typeId（读端工厂按声明类注册）");

		// 机制锁定：保存点副本不得回退到运行时类。
		Assertions.assertEquals(log.getTypeId(), log.beginSavepoint().getTypeId(),
				"beginSavepoint副本必须保持同一typeId");
	}

	@Test
	public void testEncodeCreateRoundTrip() {
		// 模拟读端注册：按声明类注册LogOne工厂（History.Helper.registerLogOne(beanClass)）。
		Helper.registerLogOne(BeanBase.class);

		var parent = new BeanBase();
		var sub = new BeanSub();
		sub.x = 42;
		var collOne = new CollOne<>((BeanBase)sub, BeanBase.class);
		collOne.initRootInfo(newRootInfo(), parent);

		// 写端编码，模拟复制通道：follower按typeId经Log.create重建再decode。
		@SuppressWarnings("unchecked")
		var log = (LogOne<BeanBase>)collOne.createLogBean();
		var bb = ByteBuffer.Allocate();
		log.encode(bb);
		@SuppressWarnings("unchecked")
		var decoded = (LogOne<BeanBase>)Log.create(log.getTypeId(), 0);
		decoded.decode(bb);
		collOne.followerApply(decoded);

		Assertions.assertEquals(42, collOne.getValue().x, "声明类重建的值必须完成数据往返");
		Assertions.assertSame(BeanBase.class, collOne.getValue().getClass(),
				"decode按声明类工厂重建值实例");
	}

	@Test
	public void testExactTypeUnchanged() {
		// 兼容红线：精确类型（运行时类==声明类）行为不变。
		Assertions.assertNotEquals(LogOneMeta.get(BeanBase.class).logTypeId,
				LogOneMeta.get(BeanSub.class).logTypeId, "测试自检：两类typeId必须可区分");

		var parent = new BeanBase();
		var collOne = new CollOne<>(new BeanBase(), BeanBase.class);
		collOne.initRootInfo(newRootInfo(), parent);
		@SuppressWarnings("unchecked")
		var log = (LogOne<BeanBase>)collOne.createLogBean();
		Assertions.assertEquals(LogOneMeta.get(BeanBase.class).logTypeId, log.getTypeId(),
				"精确类型typeId与修复前一致");

		// 未提供声明类（copy路径）：退回运行时类，与修复前行为一致。
		var copied = collOne.copy();
		@SuppressWarnings("unchecked")
		var copiedLog = (LogOne<BeanBase>)copied.createLogBean();
		Assertions.assertEquals(LogOneMeta.get(BeanBase.class).logTypeId, copiedLog.getTypeId());
	}
}
