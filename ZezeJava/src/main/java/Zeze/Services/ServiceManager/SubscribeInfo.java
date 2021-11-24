package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;

public final class SubscribeInfo extends Bean {
	public static final int SubscribeTypeSimple = 0;
	public static final int SubscribeTypeReadyCommit = 1;

	private String ServiceName;
	public String getServiceName() {
		return ServiceName;
	}
	public void setServiceName(String value) {
		ServiceName = value;
	}
	private int SubscribeType;
	public int getSubscribeType() {
		return SubscribeType;
	}
	public void setSubscribeType(int value) {
		SubscribeType = value;
	}
	private Object LocalState;
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
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return getServiceName() + ":" + getSubscribeType();
	}
}
