package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;

public class BSubscribeArgument implements Serializable {
	public final ArrayList<BSubscribeInfo> subs = new ArrayList<>(); // 每个serviceName只能订阅一次,覆盖之前的

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteUInt(subs.size());
		for (var sub : subs)
			sub.encode(bb);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		for (var i = bb.ReadUInt(); i > 0; --i)
			subs.add(new BSubscribeInfo(bb));
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@Override
	public @NotNull String toString() {
		return "BSubscribeArgument" + subs;
	}
}
