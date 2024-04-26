package Zeze.Services.ServiceManager;

import java.util.ArrayList;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

public class BUnSubscribeArgument extends Bean {
	public final ArrayList<String> serviceNames = new ArrayList<>();

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteUInt(serviceNames.size());
		for (var serviceName : serviceNames)
			bb.WriteString(serviceName);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		for (var i = bb.ReadUInt(); i > 0; --i)
			serviceNames.add(bb.ReadString());
	}

	@Override
	public String toString() {
		return serviceNames.toString();
	}

	private static int _PRE_ALLOC_SIZE_ = 128;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}
}
