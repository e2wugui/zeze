package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

public final class BServiceListVersion extends Bean {
	public String serviceName;
	public long serialId;

	@Override
	public void decode(IByteBuffer bb) {
		serviceName = bb.ReadString();
		serialId = bb.ReadLong();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(serviceName);
		bb.WriteLong(serialId);
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
	public String toString() {
		return "BServiceListVersion{" + "ServiceName='" + serviceName + '\'' + ", SerialId=" + serialId + '}';
	}
}
