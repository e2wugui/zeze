package Zeze.Dbh2.Master;

import java.util.TreeMap;
import Zeze.Builtin.Dbh2.BBucketMetaData;
import Zeze.Net.Binary;

public class MasterTable {
	private final String name;
	private final TreeMap<Binary, BBucketMetaData> buckets = new TreeMap<>(); // key is meta.first

	public MasterTable(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public BBucketMetaData open(Binary key) {
		// todo 算法实现和测试
		var lower = buckets.lowerEntry(key);
		return lower.getValue();
	}
}
