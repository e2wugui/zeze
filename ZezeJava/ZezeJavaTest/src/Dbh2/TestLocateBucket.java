package Dbh2;

import java.util.TreeMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Net.Binary;
import org.junit.Assert;
import org.junit.Test;

public class TestLocateBucket {
	public static BBucketMeta.Data locate(TreeMap<Binary, BBucketMeta.Data> buckets, Binary key) {
		var lower = buckets.floorEntry(key);
		return lower.getValue();
	}

	@Test
	public void testLocate() {
		TreeMap<Binary, BBucketMeta.Data> buckets = new TreeMap<>(); // key is meta.first

		var keyEmpty = new Binary(new byte[]{ });
		var key00 = new Binary(new byte[]{ 0, 0, 0, 0 });
		var key10 = new Binary(new byte[]{ 1, 0, 0, 0 });
		var key11 = new Binary(new byte[]{ 1, 1, 0, 0 });
		var key20 = new Binary(new byte[]{ 2, 0, 0, 0 });
		var key22 = new Binary(new byte[]{ 2, 2, 0, 0 });
		var key30 = new Binary(new byte[]{ 3, 0, 0, 0 });
		var key33 = new Binary(new byte[]{ 3, 3, 0, 0 });
		{
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("database");
			meta.setTableName("table");
			meta.setRaftConfig("raft config");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			buckets.put(meta.getKeyFirst(), meta);
			Assert.assertSame(locate(buckets, keyEmpty), meta);
			Assert.assertSame(locate(buckets, key00), meta);
			Assert.assertSame(locate(buckets, key10), meta);
			Assert.assertSame(locate(buckets, key11), meta);
			Assert.assertSame(locate(buckets, key20), meta);
			Assert.assertSame(locate(buckets, key22), meta);
			Assert.assertSame(locate(buckets, key30), meta);
			Assert.assertSame(locate(buckets, key33), meta);
		}
		buckets.clear();

		var metaEmpty = new BBucketMeta.Data();
		metaEmpty.setDatabaseName("database");
		metaEmpty.setTableName("table");
		metaEmpty.setRaftConfig("raft config");
		metaEmpty.setKeyFirst(Binary.Empty);
		metaEmpty.setKeyLast(key10);
		buckets.put(metaEmpty.getKeyFirst(), metaEmpty);

		var metaKey10 = new BBucketMeta.Data();
		metaKey10.setDatabaseName("database");
		metaKey10.setTableName("table");
		metaKey10.setRaftConfig("raft config");
		metaKey10.setKeyFirst(key10);
		metaKey10.setKeyLast(key20);
		buckets.put(metaKey10.getKeyFirst(), metaKey10);

		var metaKey20 = new BBucketMeta.Data();
		metaKey20.setDatabaseName("database");
		metaKey20.setTableName("table");
		metaKey20.setRaftConfig("raft config");
		metaKey20.setKeyFirst(key20);
		metaKey20.setKeyLast(key30);
		buckets.put(metaKey20.getKeyFirst(), metaKey20);

		var metaKey30 = new BBucketMeta.Data();
		metaKey30.setDatabaseName("database");
		metaKey30.setTableName("table");
		metaKey30.setRaftConfig("raft config");
		metaKey30.setKeyFirst(key30);
		metaKey30.setKeyLast(Binary.Empty);
		buckets.put(metaKey30.getKeyFirst(), metaKey30);

		Assert.assertSame(locate(buckets, keyEmpty), metaEmpty);
		Assert.assertSame(locate(buckets, key00), metaEmpty);
		Assert.assertSame(locate(buckets, key10), metaKey10);
		Assert.assertSame(locate(buckets, key11), metaKey10);
		Assert.assertSame(locate(buckets, key20), metaKey20);
		Assert.assertSame(locate(buckets, key22), metaKey20);
		Assert.assertSame(locate(buckets, key30), metaKey30);
		Assert.assertSame(locate(buckets, key33), metaKey30);
	}
}
