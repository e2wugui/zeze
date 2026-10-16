package Zeze.Services.ServiceManager;

import java.util.HashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

/** Subscribe应答bean：serviceName→订阅时刻的版本分桶地址快照。 */
public class BSubscribeResult implements Serializable {
	public final HashMap<String, BServiceInfosVersion> map = new HashMap<>(); // key:serviceName

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteUInt(map.size());
		for (var e : map.entrySet()) {
			bb.WriteString(e.getKey());
			e.getValue().encode(bb);
		}
	}

	@Override
	public void decode(IByteBuffer bb) {
		for (var i = bb.ReadUInt(); i > 0; --i) {
			var serviceName = bb.ReadString();
			map.put(serviceName, new BServiceInfosVersion(bb));
		}
	}

	private static int _PRE_ALLOC_SIZE_ = 256;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@Override
	public String toString() {
		return "BSubscribeResult" + map;
	}
}
