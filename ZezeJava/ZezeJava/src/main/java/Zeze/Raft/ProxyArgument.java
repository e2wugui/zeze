package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.ProtocolFactoryFinder;
import org.jetbrains.annotations.NotNull;

/**
 * 代理协议的请求参数。
 */
public class ProxyArgument implements Serializable {
	private String raftId;
	private Binary rpc = Binary.Empty;

	public ProxyArgument() {
	}

	public ProxyArgument(String raftId, Rpc<?, ?> rr) {
		this.raftId = raftId;
		this.rpc = new Binary(rr.encode());
	}

	public String getRaftName() {
		return raftId;
	}

	public Binary getRpcBinary() {
		return rpc;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteString(raftId);
		bb.WriteBinary(rpc);
	}

	@Override
	public void decode(@NotNull ByteBuffer bb) {
		raftId = bb.ReadString();
		rpc = bb.ReadBinary();
	}
}
