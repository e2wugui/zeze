package Zeze.Services.ServiceManager;

import java.util.HashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

public class BSubscribeResult extends Bean {
	public final HashMap<String, BServiceInfos> map = new HashMap<>();

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteUInt(map.size());
		for (var e : map.values()) {
			e.encode(bb);
		}
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		for (var i = bb.ReadUInt(); i > 0; --i) {
			var infos = new BServiceInfos();
			infos.decode(bb);
			map.put(infos.getServiceName(), infos);
		}
	}

	@Override
	public String toString() {
		return map.toString();
	}

	private static int _PRE_ALLOC_SIZE_ = 100 * 1024;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}
}
