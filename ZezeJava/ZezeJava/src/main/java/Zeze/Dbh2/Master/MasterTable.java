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

/**
 * Master 侧表元数据：桶区间 TreeMap（嵌套 Data）及其序列化编解码。
 */
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

		// floorEntry对并发写不是线程安全（红黑树重组中途读）。Dbh2分桶历史（Bucket.splitMetaHistory）
		// 的写在raft apply线程、读在user-task线程，内部持锁保护；Master调用方已持锁，可重入无副作用。
		// 空表/小于首桶first的越界键返回null（FND28 F1）：调用方（Dbh2.ProcessPrepareBatchRequest
		// 的拒绝路径、tailMap）以null判"无覆盖桶"——此前floorEntry==null时对lower.getValue()直接
		// NPE，Dbh2侧的null检查是死代码，异常面目替代可判定错误码eBucketNotFound。
		public BBucketMeta.Data locate(Binary key) {
			lock();
			try {
				var lower = buckets.floorEntry(key);
				return null != lower ? lower.getValue() : null;
			} finally {
				unlock();
			}
		}

		// 返回的是live视图：仅Dbh2AgentManager使用，其操作的实例是rpc返回的快照拷贝，无并发写。
		// 越界键（无floor，FND28 F1）：从key自身起tail——小于全部桶first的键等价于整表视图，
		// 与"定位到覆盖-or-前驱桶再tail"的窗口语义一致（此前locate对空表NPE）。
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
