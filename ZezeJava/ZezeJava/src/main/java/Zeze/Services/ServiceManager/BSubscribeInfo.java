package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class BSubscribeInfo extends Bean {
	public static final int SubscribeTypeSimple = 0;
	public static final int SubscribeTypeReadyCommit = 1;

	private String ServiceName;
	private int SubscribeType;
	private Object LocalState;

	public String getServiceName() {
		return ServiceName;
	}

	public void setServiceName(String value) {
		ServiceName = value;
	}

	public int getSubscribeType() {
		return SubscribeType;
	}

	public void setSubscribeType(int value) {
		SubscribeType = value;
	}

	public Object getLocalState() {
		return LocalState;
	}

	public void setLocalState(Object value) {
		LocalState = value;
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setServiceName(bb.ReadString());
		setSubscribeType(bb.ReadInt());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteString(getServiceName());
		bb.WriteInt(getSubscribeType());
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void ResetChildrenRootInfo() {
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
