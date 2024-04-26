package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class BSubscribeArgument extends Bean {
	public final List<BSubscribeInfo> subs = new ArrayList<>();

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

	@Override
	public @NotNull String toString() {
		return subs.toString();
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
}
