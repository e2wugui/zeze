package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

public final class BSubscribeInfo extends Bean {
	private String serviceName;
	private long version;
	private Object localState;

	public BSubscribeInfo() {
	}

	public BSubscribeInfo(String name) {
		serviceName = name;
	}

	public BSubscribeInfo(String name, long version) {
		serviceName = name;
		this.version = version;
	}

	public BSubscribeInfo(String name, long version, Object state) {
		serviceName = name;
		this.version = version;
		localState = state;
	}

	public long getVersion() {
		return version;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String value) {
		serviceName = value;
	}

	public Object getLocalState() {
		return localState;
	}

	public void setLocalState(Object value) {
		localState = value;
	}

	@Override
	public void decode(IByteBuffer bb) {
		setServiceName(bb.ReadString());
		this.version = bb.ReadLong();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(getServiceName());
		bb.WriteLong(version);
	}

	@Override
	public String toString() {
		return getServiceName();
	}

	private static int _PRE_ALLOC_SIZE_ = 32;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}
}
