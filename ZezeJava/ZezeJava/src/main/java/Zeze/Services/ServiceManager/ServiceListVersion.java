package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class ServiceListVersion extends Bean {
	public String ServiceName;
	public long SerialId;

	@Override
	public void Decode(ByteBuffer bb) {
		ServiceName = bb.ReadString();
		SerialId = bb.ReadLong();
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteString(ServiceName);
		bb.WriteLong(SerialId);
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int getPreAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void setPreAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}
}
