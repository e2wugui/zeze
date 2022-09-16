package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class BSubscribeInfo extends Bean {
	public static final int SubscribeTypeSimple = 0;
	public static final int SubscribeTypeReadyCommit = 1;

	private String serviceName;
	private int subscribeType;
	private Object localState;

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String value) {
		serviceName = value;
	}

	public int getSubscribeType() {
		return subscribeType;
	}

	public void setSubscribeType(int value) {
		subscribeType = value;
	}

	public Object getLocalState() {
		return localState;
	}

	public void setLocalState(Object value) {
		localState = value;
	}

	@Override
	public void decode(ByteBuffer bb) {
		setServiceName(bb.ReadString());
		setSubscribeType(bb.ReadInt());
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(getServiceName());
		bb.WriteInt(getSubscribeType());
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return getServiceName() + ":" + getSubscribeType();
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
