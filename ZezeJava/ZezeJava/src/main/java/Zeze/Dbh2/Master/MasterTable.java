package Zeze.Dbh2.Master;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

public class MasterTable {
	public static class Data extends ReentrantLock implements Serializable {
		final TreeMap<Binary, BBucketMeta.Data> buckets = new TreeMap<>(); // key is meta.first
		volatile boolean created = false;

		public Collection<BBucketMeta.Data> buckets() {
			return buckets.values();
		}

		public TreeMap<Binary, BBucketMeta.Data> getBuckets() {
			return buckets;
		}

		// 持锁深拷贝快照：Master读路径（GetBuckets/LocateBucket/Register）返回或遍历快照，
		// 避免rpc序列化遍历TreeMap与endSplit/endMove的持锁写并发（CME/脏结构）。
		public MasterTable.Data snapshot() {
			lock();
			try {
				var copy = new MasterTable.Data();
				copy.created = created;
				for (var e : buckets.entrySet())
					copy.buckets.put(e.getKey(), e.getValue().copy());
				return copy;
			} finally {
				unlock();
			}
		}

		// floorEntry对并发写不是线程安全（红黑树重组中途读）。Dbh2分桶历史（Bucket.splitMetaHistory）
		// 的写在raft apply线程、读在user-task线程，内部持锁保护；Master调用方已持锁，可重入无副作用。
		public BBucketMeta.Data locate(Binary key) {
			lock();
			try {
				var lower = buckets.floorEntry(key);
				return lower.getValue();
			} finally {
				unlock();
			}
		}

		// 返回的是live视图：仅Dbh2AgentManager使用，其操作的实例是rpc返回的快照拷贝，无并发写。
		public SortedMap<Binary, BBucketMeta.Data> tailMap(Binary key) {
			var bucket = locate(key);
			lock();
			try {
				return buckets.tailMap(bucket.getKeyFirst());
			} finally {
				unlock();
			}
		}

		@Override
		public String toString() {
			return buckets.values().toString();
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteBool(created);
			bb.WriteUInt(buckets.size());
			for (var e : buckets.entrySet()) {
				bb.WriteBinary(e.getKey());
				e.getValue().encode(bb);
			}
		}

		@Override
		public void decode(IByteBuffer bb) {
			created = bb.ReadBool();
			buckets.clear();
			for (var size = bb.ReadUInt(); size > 0; --size) {
				var key = bb.ReadBinary();
				var value = new BBucketMeta.Data();
				value.decode(bb);
				buckets.put(key, value);
			}
		}

		public ByteBuffer encode() {
			var bb = ByteBuffer.Allocate();
			encode(bb);
			return bb;
		}
	}
}
