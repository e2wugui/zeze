package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class BServiceListVersion extends Bean {
	public String ServiceName;
	public long SerialId;

	@Override
	public void decode(ByteBuffer bb) {
		ServiceName = bb.ReadString();
		SerialId = bb.ReadLong();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(ServiceName);
		bb.WriteLong(SerialId);
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
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
		return "BServiceListVersion{" + "ServiceName='" + ServiceName + '\'' + ", SerialId=" + SerialId + '}';
	}
}
