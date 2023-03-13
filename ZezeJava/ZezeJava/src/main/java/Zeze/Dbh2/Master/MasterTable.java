package Zeze.Dbh2.Master;

import java.util.Collection;
import java.util.TreeMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

public class MasterTable {
	public static class Data implements Serializable {
		final TreeMap<Binary, BBucketMeta.Data> buckets = new TreeMap<>(); // key is meta.first
		volatile boolean created = false;

		public Collection<BBucketMeta.Data> buckets() {
			return buckets.values();
		}

		public BBucketMeta.Data locate(Binary key) {
			var lower = buckets.floorEntry(key);
			return lower.getValue();
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteBool(created);
			bb.WriteInt(buckets.size());
			for (var e : buckets.entrySet()) {
				bb.WriteBinary(e.getKey());
				e.getValue().encode(bb);
			}
		}

		@Override
		public void decode(ByteBuffer bb) {
			created = bb.ReadBool();
			buckets.clear();
			for (var size = bb.ReadInt(); size > 0; --size) {
				var key = bb.ReadBinary();
				var value = new BBucketMeta.Data();
				value.decode(bb);
				buckets.put(key, value);
			}
		}
	}
}
