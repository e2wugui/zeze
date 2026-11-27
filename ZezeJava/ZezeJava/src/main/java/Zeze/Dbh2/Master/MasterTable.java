package Zeze.Dbh2.Master;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

/**
 * Master 侧表元数据：桶区间 TreeMap（嵌套 Data）及其序列化编解码。
 */
public class MasterTable {
	public static class Data extends ReentrantLock implements Serializable {
		final TreeMap<Binary, BBucketMeta.Data> buckets = new TreeMap<>(); // key 为 meta.keyFirst
		volatile boolean created = false;

		public Collection<BBucketMeta.Data> buckets() {
			return buckets.values();
		}

		public TreeMap<Binary, BBucketMeta.Data> getBuckets() {
			return buckets;
		}

		// 持锁深拷贝快照，供 Master 读路径安全遍历或序列化，避免与持锁写并发
		public Data snapshot() {
			lock();
			try {
				var copy = new Data();
				copy.created = created;
				for (var e : buckets.entrySet())
					copy.buckets.put(e.getKey(), e.getValue().copy());
				return copy;
			} finally {
				unlock();
			}
		}

		// 持锁 floor 查找（与 raft apply 写互斥）；空表或小于首桶起始键时返回 null
		public BBucketMeta.@Nullable Data locate(Binary key) {
			lock();
			try {
				var lower = buckets.floorEntry(key);
				return null != lower ? lower.getValue() : null;
			} finally {
				unlock();
			}
		}

		// 返回子视图（仅用于只读快照拷贝）；无前驱桶时从 key 起向后覆盖全表
		public SortedMap<Binary, BBucketMeta.Data> tailMap(Binary key) {
			var bucket = locate(key);
			lock();
			try {
				return buckets.tailMap(null != bucket ? bucket.getKeyFirst() : key);
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
