package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

public class UniqueRequestId implements Serializable {
	private String clientId = "";
	private long requestId;

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String value) {
		clientId = value;
	}

	public long getRequestId() {
		return requestId;
	}

	public void setRequestId(long value) {
		requestId = value;
	}

	@Override
	public final void encode(ByteBuffer bb) {
		bb.WriteString(clientId);
		bb.WriteLong(requestId);
	}

	@Override
	public final void decode(IByteBuffer bb) {
		clientId = bb.ReadString();
		requestId = bb.ReadLong();
	}

	@Override
	public int hashCode() {
		final int _prime_ = 31;
		return clientId.hashCode() * _prime_ + Long.hashCode(requestId);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof UniqueRequestId))
			return false;
		UniqueRequestId other = (UniqueRequestId)obj;
		return clientId.equals(other.clientId) && requestId == other.requestId;
	}

	@Override
	public String toString() {
		return String.format("(ClientId=%s RequestId=%d)", clientId, requestId);
	}
}
