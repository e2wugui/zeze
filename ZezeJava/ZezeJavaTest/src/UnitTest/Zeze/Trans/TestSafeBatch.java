package UnitTest.Zeze.Trans;

import Zeze.Component.SafeBatch;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import demo.App;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NavigableMap;

public class TestSafeBatch {
	@BeforeEach
	public void before() throws Exception {
		App.getInstance().Start();
	}

	@Test
	public void testSafeBatch() throws Exception {
		App.getInstance().getZeze().newProcedure(() -> {
			var table = App.getInstance().demo_Module1.getTable5();
			var value1 = table.getOrAdd(1L);
			value1.setS(1);

			value1.getPlist().clear();
			value1.getPlist().add(1);
			value1.getPlist().add(2);
			value1.getPlist().add(3);

			value1.getPsortedmap().clear();
			value1.getPsortedmap().put(1, 1);
			value1.getPsortedmap().put(2, 2);
			value1.getPsortedmap().put(3, 3);

			table.getOrAdd(2L).setS(2);
			table.getOrAdd(3L).setS(3);
			return 0;
		}, "initSafeBatchData").call();

		App.getInstance().getZeze().checkpointRun(); // walk 需要保存数据。

		App.getInstance().getZeze().newProcedure(() -> {
			App.getInstance().getZeze().getSafeBatch().startWalkTable(
				App.getInstance().demo_Module1.getTable5(),
				(safeBatch, key, value) -> {
					System.out.println("SafeBatch: " + key + ", " + value.getS());
					return 0;
				}, 1000, 1);
			return 0;
		}, "startWalkTable").call();

		App.getInstance().getZeze().newProcedure(() -> {
			App.getInstance().getZeze().getSafeBatch().startWalkList(
				App.getInstance().demo_Module1.getTable5(), 1L, new WalkList(), 1000, 1);
			return 0;
		}, "startWalkList").call();


		App.getInstance().getZeze().newProcedure(() -> {
			App.getInstance().getZeze().getSafeBatch().startWalkSortedMap(
				App.getInstance().demo_Module1.getTable5(), 1L, new WalkSortedMap(), 1000, 1);
			return 0;
		}, "startWalkSortedMap").call();

		System.out.println("startWalkTable ...");
		Thread.sleep(1000);
		System.out.println("startWalkTable ... end.");
	}

	// FND3-31：get*OutTransaction 返回 null（记录不存在/已删）= 无工作可推进，批处理必须停止并清理
	// （对齐 TableBatchWorker 遍历尽与 checkBatch 表不存在的停批先例）。
	// 修复前：worker 只 break 不 stopBatch，看门狗每 tick 重建 worker 再拿 null 再退出——
	// Timer 与 Timer 表 BBatch 记录永不清理（僵尸批处理），getBatch 恒非空。
	@Test
	public void testRecordMissingStopsBatch() throws Exception {
		var table = App.getInstance().demo_Module1.getTable5();
		var safeBatch = App.getInstance().getZeze().getSafeBatch();

		var jobIdList = new String[1];
		var jobIdMap = new String[1];
		App.getInstance().getZeze().newProcedure(() -> {
			// 记录 99 不存在：WalkList/WalkSortedMap 的 get*OutTransaction 均返回 null
			jobIdList[0] = safeBatch.startWalkList(table, 99L, new WalkList(), 200, 1);
			jobIdMap[0] = safeBatch.startWalkSortedMap(table, 99L, new WalkSortedMap(), 200, 1);
			return 0;
		}, "startMissingRecordBatches").call();

		var deadline = System.currentTimeMillis() + 10_000;
		while (null != queryBatch(jobIdList[0]) || null != queryBatch(jobIdMap[0])) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail("missing-record batch not stopped (zombie): listDone="
						+ (null == queryBatch(jobIdList[0])) + " mapDone=" + (null == queryBatch(jobIdMap[0])));
			//noinspection BusyWait
			Thread.sleep(50);
		}
	}

	// getBatch 走 _tIndexs.get，TableX.get 断言必须在事务内调用。
	private @Nullable Object queryBatch(String jobId) {
		var out = new Object[1];
		App.getInstance().getZeze().newProcedure(() -> {
			out[0] = App.getInstance().getZeze().getSafeBatch().getBatch(jobId);
			return 0;
		}, "queryBatch_" + jobId).call();
		return out[0];
	}

	public static class WalkSortedMap implements SafeBatch.WalkSortedMapJobHandle<Integer, Integer> {
		@Override
		public long runJob(SafeBatch safeBatch, Integer key, Integer value) {
			System.out.println("SafeBatch_SortedMap: " + key + ", " + value);
			return 0;
		}

		@Override
		public @Nullable ByteBuffer encodeMapKey(@NotNull TableX<?, ?> table, @NotNull ByteBuffer tableKey,
		                                        @NotNull Integer mapKey) {
			var tt = (demo.Module1.Table5) table;
			var value = tt.selectDirty(tt.decodeKey(tableKey));
			if (null == value)
				return null;
			return value.getPsortedmap().encodeKey(mapKey);
		}

		@Override
		public @NotNull Integer decodeMapKey(@NotNull TableX<?, ?> table, @NotNull ByteBuffer tableKey, @NotNull ByteBuffer bb) {
			var tt = (demo.Module1.Table5) table;
			var value = tt.selectDirty(tt.decodeKey(tableKey));
			if (null == value)
				throw new RuntimeException("record not found: " + table.getName() + " " + tableKey);
			return value.getPsortedmap().decodeKey(bb);
		}

		@Override
		public @Nullable NavigableMap<Integer, Integer> getSortedMapOutTransaction(
			@NotNull TableX<?, ?> table, @NotNull ByteBuffer tableKey) {
			var tt = (demo.Module1.Table5) table;
			var value = tt.selectDirty(tt.decodeKey(tableKey));
			if (null == value) {
				return null;
			}
			return value.getPsortedmap();
		}
	}

	public static class WalkList implements SafeBatch.WalkListJobHandle<Integer> {
		@Override
		public long runJob(SafeBatch safeBatch, int index, Integer value) {
			System.out.println("SafeBatch_List: " + index + ", " + value);
			return 0;
		}

		@Override
		public @Nullable java.util.List<Integer> getListOutTransaction(@NotNull TableX<?, ?> table,
		                                                              @NotNull ByteBuffer tableKey) {
			var tt = (demo.Module1.Table5) table;
			var value = tt.selectDirty(tt.decodeKey(tableKey));
			if (null == value) {
				return null;
			}
			return value.getPlist();
		}
	}
}
