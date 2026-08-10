package UnitTest.Zeze.Trans;

import Zeze.Component.SafeBatch;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import demo.App;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.util.NavigableMap;

public class TestSafeBatch {
	@Before
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
		}, "initsafebatchdata").call();

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
		public @Nullable NavigableMap<Integer, Integer> tailMapExclusiveOutTransaction(
			@NotNull TableX<?, ?> table, @NotNull ByteBuffer tableKey, @Nullable Integer mapKey) throws Exception {
			var tt = (demo.Module1.Table5) table;
			var value = tt.selectDirty(tt.decodeKey(tableKey));
			System.out.println("tailMapExclusiveOutTransaction: " + mapKey);
			if (null == value) {
				return null;
			}

			if (null == mapKey) {
				return value.getPsortedmap();
			}

			return value.getPsortedmap().tailMap(mapKey, false);
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
		                                                              @NotNull ByteBuffer tableKey) throws Exception {
			var tt = (demo.Module1.Table5) table;
			var value = tt.selectDirty(tt.decodeKey(tableKey));
			if (null == value) {
				return null;
			}
			return value.getPlist();
		}
	}
}
