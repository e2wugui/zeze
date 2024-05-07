package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Transaction.TableWalkHandleRaw;

public class ApplyDatabaseMemory implements IApplyDatabase {
	private final ConcurrentHashMap<String, ApplyTableMemory> tables = new ConcurrentHashMap<>();

	@Override
	public IApplyTable open(String tableName) {
		return tables.computeIfAbsent(tableName, ApplyTableMemory::new);
	}

	private static class ApplyTableMemory implements IApplyTable {
		private final String tableName;
		private final ConcurrentHashMap<Binary, Binary> dataMap = new ConcurrentHashMap<>();

		public ApplyTableMemory(String tableName) {
			this.tableName = tableName;
		}

		@Override
		public String getTableName() {
			return tableName;
		}

		@Override
		public Binary get(byte[] key, int offset, int length) {
			return dataMap.get(new Binary(key, offset, length));
		}

		@Override
		public void put(byte[] key, int keyOffset, int keyLength, byte[] value, int valueOffset, int valueLength) {
			dataMap.put(new Binary(key, keyOffset, keyLength), new Binary(value, valueOffset, valueLength));
		}

		@Override
		public void remove(byte[] key, int offset, int length) {
			dataMap.remove(new Binary(key, offset, length));
		}

		@Override
		public boolean isEmpty() {
			return dataMap.isEmpty();
		}

		@Override
		public void walk(TableWalkHandleRaw walker) {
			for (var e : dataMap.entrySet()) {
				if (!walker.handle(e.getKey().copyIf(), e.getValue().copyIf()))
					break;
			}
		}
	}
}
