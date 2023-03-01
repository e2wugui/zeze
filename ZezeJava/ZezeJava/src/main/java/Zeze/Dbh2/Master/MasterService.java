package Zeze.Dbh2.Master;

/**
 * 根据key查询桶(bucket)
 */
public class MasterService {
	public void locate(String tableName, byte[] key) {
		/*
		var table = getTable(tableName);
		var it = table.iterator();
		var lowerBound = it.seek(key);
		if (lowerBound.isValid()) {
			var bucket = decode(lowerBound.value());
			...
		} else {
			// is empty；分配第一个桶。
			var bucket = allocate();
			table.put(key, bucket);
		}
		// 其他：分桶策略在哪里实现。
		*/
	}
}
