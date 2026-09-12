package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import harness.Fast;

/**
 * FND4-01：脏标记与脏期间强引用合并为单一volatile事实源（dirtyRef）。
 * 原dirty+strongDirtyValue两字段写读非原子，TableX.load判脏后取值的间隙被清脏
 * （Checkpoint.flush成功后不持fairLock）打断会拿到null当作"记录不存在"，
 * 已提交未读出的内存脏数据被静默丢弃。本测试锁定合并字段的语义契约：
 * 置脏=单次写（脏值即强引用）；脏删除用哨兵维持脏事实（否则误判干净会从库读回
 * 已删除旧值）；清脏=单次写null。
 * 注：TOCTOU交错本身需checkpoint线程与load微秒级精确竞态，不可确定性构造，
 * 以结构消除（单volatile读写）+语义锁定覆盖；包内直调package-private方法
 * （与TestThreadingRWLockDowngrade同款测试缝）。
 */
@Fast
public class TestRecordDirtyRef {
	private static final class StubRecord extends Record {
		StubRecord(@Nullable Bean value) {
			super(value);
		}

		@Override
		public @NotNull Table getTable() {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull Object getObjectKey() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setDirty() {
			setDirty(true);
		}

		@Override
		public @Nullable IGlobalAgent.AcquireResult acquire(int state, boolean fresh, boolean noWait) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void encode0() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void flush(@Nullable Database.Transaction t, @NotNull Database.Transaction lct) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void commit(@NotNull RecordAccessed accessed) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void cleanup() {
			throw new UnsupportedOperationException();
		}
	}

	@Test
	public void testDirtyValueIsStrongRefSnapshot() {
		var bean = new EmptyBean();
		var r = new StubRecord(bean);
		Assertions.assertFalse(r.getDirty());
		Assertions.assertNull(r.getDirtyValue()); // 干净：脏值null，调用方走storage

		r.setSoftValue(bean);
		r.setDirty(true);
		Assertions.assertTrue(r.getDirty());
		Assertions.assertSame(bean, r.getDirtyValue()); // 脏值快照=置脏时softValue的强引用

		r.setDirty(false); // 清脏：单次volatile写null
		Assertions.assertFalse(r.getDirty());
		Assertions.assertNull(r.getDirtyValue());
	}

	@Test
	public void testDirtyRemoveKeepsDirtyFact() {
		// 脏删除（commit的PutLog.getValue()==null，softValue为null）：哨兵必须维持"脏"事实，
		// 否则getDirty()误判干净→load从storage读回已删除的旧值（删除丢失）
		var r = new StubRecord(null);
		r.setSoftValue(null);
		r.setDirty(true);
		Assertions.assertTrue(r.getDirty(), "脏删除必须保持脏事实（哨兵）");
		Assertions.assertNull(r.getDirtyValue(), "脏删除的值快照为null（记录不存在语义）");
	}
}
