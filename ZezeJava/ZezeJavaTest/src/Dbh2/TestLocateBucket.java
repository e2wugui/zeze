package Dbh2;

import java.util.TreeMap;
import Zeze.Builtin.Dbh2.BBucketMetaData;
import Zeze.Net.Binary;
import org.junit.Assert;
import org.junit.Test;

public class TestLocateBucket {
	TreeMap<Binary, BBucketMetaData> buckets = new TreeMap<>(); // key is meta.first
	public BBucketMetaData locate(Binary key) {
		var lower = buckets.lowerEntry(key);
		return lower.getValue();
	}

	@Test
	public void test() {

		var keyEmpty = new Binary(new byte[]{ });
		var key00 = new Binary(new byte[]{ 0, 0, 0, 0 });
		var key10 = new Binary(new byte[]{ 1, 0, 0, 0 });
		var key11 = new Binary(new byte[]{ 1, 1, 0, 0 });
		var key20 = new Binary(new byte[]{ 2, 0, 0, 0 });
		var key22 = new Binary(new byte[]{ 2, 2, 0, 0 });
		var key30 = new Binary(new byte[]{ 3, 0, 0, 0 });
		var key33 = new Binary(new byte[]{ 3, 3, 0, 0 });
		{
			var meta = new BBucketMetaData();
			meta.setDatabaseName("database");
			meta.setTableName("table");
			meta.setRaftConfig("raft config");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			buckets.put(meta.getKeyFirst(), meta);
			Assert.assertEquals(locate(keyEmpty), meta);
			Assert.assertEquals(locate(key00), meta);
			Assert.assertEquals(locate(key10), meta);
			Assert.assertEquals(locate(key11), meta);
			Assert.assertEquals(locate(key20), meta);
			Assert.assertEquals(locate(key22), meta);
			Assert.assertEquals(locate(key30), meta);
			Assert.assertEquals(locate(key33), meta);
		}
		buckets.clear();

		var metaEmpty = new BBucketMetaData();
		metaEmpty.setDatabaseName("database");
		metaEmpty.setTableName("table");
		metaEmpty.setRaftConfig("raft config");
		metaEmpty.setKeyFirst(Binary.Empty);
		metaEmpty.setKeyLast(key10);
		buckets.put(metaEmpty.getKeyFirst(), metaEmpty);

		var metaKey10 = new BBucketMetaData();
		metaKey10.setDatabaseName("database");
		metaKey10.setTableName("table");
		metaKey10.setRaftConfig("raft config");
		metaKey10.setKeyFirst(key10);
		metaKey10.setKeyLast(key20);
		buckets.put(metaKey10.getKeyFirst(), metaKey10);

		var metaKey20 = new BBucketMetaData();
		metaKey20.setDatabaseName("database");
		metaKey20.setTableName("table");
		metaKey20.setRaftConfig("raft config");
		metaKey20.setKeyFirst(key20);
		metaKey20.setKeyLast(key30);
		buckets.put(metaKey20.getKeyFirst(), metaKey20);

		var metaKey30 = new BBucketMetaData();
		metaKey30.setDatabaseName("database");
		metaKey30.setTableName("table");
		metaKey30.setRaftConfig("raft config");
		metaKey30.setKeyFirst(key30);
		metaKey30.setKeyLast(Binary.Empty);
		buckets.put(metaKey30.getKeyFirst(), metaKey30);

		Assert.assertEquals(locate(keyEmpty), metaEmpty);
		Assert.assertEquals(locate(key00), metaEmpty);
		Assert.assertEquals(locate(key10), metaKey10);
		Assert.assertEquals(locate(key11), metaKey10);
		Assert.assertEquals(locate(key20), metaKey20);
		Assert.assertEquals(locate(key22), metaKey20);
		Assert.assertEquals(locate(key30), metaKey30);
		Assert.assertEquals(locate(key33), metaKey30);
	}
}
