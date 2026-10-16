package Zeze.Raft;

import org.jspecify.annotations.Nullable;

import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

/**
 * 代理协议的请求参数。
 */
public class ProxyArgument implements Serializable {
	private @Nullable String raftId;
	private Binary rpc = Binary.Empty;

	public ProxyArgument() {
	}

	public ProxyArgument(String raftId, Rpc<?, ?> rr) {
		this.raftId = raftId;
		this.rpc = new Binary(rr.encode());
	}

	public @Nullable String getRaftName() {
		return raftId;
	}

	public Binary getRpcBinary() {
		return rpc;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(raftId);
		bb.WriteBinary(rpc);
	}

	@Override
	public void decode(IByteBuffer bb) {
		raftId = bb.ReadString();
		rpc = bb.ReadBinary();
	}
}
