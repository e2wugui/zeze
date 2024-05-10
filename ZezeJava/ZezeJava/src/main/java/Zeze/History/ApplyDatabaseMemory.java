package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Transaction.TableWalkHandleRaw;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ApplyDatabaseMemory implements IApplyDatabase {
	private final ConcurrentHashMap<String, ApplyTableMemory> tables = new ConcurrentHashMap<>();

	@Override
	public @NotNull IApplyTable open(@NotNull String tableName) {
		return tables.computeIfAbsent(tableName, ApplyTableMemory::new);
	}

	private static class ApplyTableMemory implements IApplyTable {
		private final @NotNull String tableName;
		private final ConcurrentHashMap<Binary, Binary> dataMap = new ConcurrentHashMap<>();

		public ApplyTableMemory(@NotNull String tableName) {
			this.tableName = tableName;
		}

		@Override
		public @NotNull String getTableName() {
			return tableName;
		}

		@Override
		public @Nullable Binary get(byte @NotNull [] key, int offset, int length) {
			return dataMap.get(new Binary(key, offset, length));
		}

		@Override
		public void put(byte @NotNull [] key, int keyOffset, int keyLength,
						byte @NotNull [] value, int valueOffset, int valueLength) {
			dataMap.put(new Binary(key, keyOffset, keyLength), new Binary(value, valueOffset, valueLength));
		}

		@Override
		public void remove(byte @NotNull [] key, int offset, int length) {
			dataMap.remove(new Binary(key, offset, length));
		}

		@Override
		public boolean isEmpty() {
			return dataMap.isEmpty();
		}

		@Override
		public void walk(@NotNull TableWalkHandleRaw walker) throws Exception {
			for (var e : dataMap.entrySet()) {
				if (!walker.handle(e.getKey().copyIf(), e.getValue().copyIf()))
					break;
			}
		}
	}
}
