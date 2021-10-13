package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;

public final  class AllocateIdResult extends Bean {
	private String Name;
	public String getName() {
		return Name;
	}
	public void setName(String value) {
		Name = value;
	}
	private long StartId;
	public long getStartId() {
		return StartId;
	}
	public void setStartId(long value) {
		StartId = value;
	}
	private int Count;
	public int getCount() {
		return Count;
	}
	public void setCount(int value) {
		Count = value;
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setName(bb.ReadString());
		setStartId(bb.ReadLong());
		setCount(bb.ReadInt());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteString(getName());
		bb.WriteLong(getStartId());
		bb.WriteInt(getCount());
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}
}
