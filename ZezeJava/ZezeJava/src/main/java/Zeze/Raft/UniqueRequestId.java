package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

public class UniqueRequestId implements Serializable {
	private String ClientId = "";
	private long RequestId;

	public String getClientId() {
		return ClientId;
	}

	public void setClientId(String value) {
		ClientId = value;
	}

	public long getRequestId() {
		return RequestId;
	}

	public void setRequestId(long value) {
		RequestId = value;
	}

	@Override
	public final void Encode(ByteBuffer bb) {
		bb.WriteString(ClientId);
		bb.WriteLong(RequestId);
	}

	@Override
	public final void Decode(ByteBuffer bb) {
		ClientId = bb.ReadString();
		RequestId = bb.ReadLong();
	}

	@Override
	public int hashCode() {
		final int _prime_ = 31;
		return ClientId.hashCode() * _prime_ + Long.hashCode(RequestId);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof UniqueRequestId))
			return false;
		UniqueRequestId other = (UniqueRequestId)obj;
		return ClientId.equals(other.ClientId) && RequestId == other.RequestId;
	}

	@Override
	public String toString() {
		return String.format("(ClientId=%s RequestId=%d)", ClientId, RequestId);
	}
}
