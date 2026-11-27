package Zeze.Raft;

import org.jspecify.annotations.Nullable;

import java.util.function.ToLongFunction;
import Zeze.Net.AsyncSocket;
import Zeze.Net.FamilyClass;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.TaskCompletionSource;

/**
 * Raft 应用请求 Rpc 基类：携带唯一请求标识（UniqueRequestId/createTime），
 * 编码含应答码与已编码结果，发送经 RaftRpcBridge 桥接。
 */
public abstract class RaftRpc<TArgument extends Serializable, TResult extends Serializable>
		extends ProxyableRpc<TArgument, TResult> implements IRaftRpc {
	private long createTime;
	private UniqueRequestId unique = new UniqueRequestId();
	private long sendTime;
	// 发送前冻结的初始请求码（如 AcquireFreshSource），与应答/超时线程原地回写的 resultCode/isRequest 隔离。
	private long requestResultCode;
	private boolean requestFrozen;

	@Nullable TaskCompletionSource<RaftRpc<TArgument, TResult>> future;
	@Nullable ToLongFunction<Protocol<?>> handle;

	@Override
	public int getFamilyClass() {
		return isRequest() ? FamilyClass.RaftRequest : FamilyClass.RaftResponse;
	}

	@Override
	public long getCreateTime() {
		return createTime;
	}

	@Override
	public void setCreateTime(long value) {
		createTime = value;
	}

	@Override
	public UniqueRequestId getUnique() {
		return unique;
	}

	@Override
	public void setUnique(UniqueRequestId value) {
		unique = value;
	}

	@Override
	public long getSendTime() {
		return sendTime;
	}

	@Override
	public void setSendTime(long value) {
		sendTime = value;
	}

	void freezeRequestState() {
		requestResultCode = resultCode;
		requestFrozen = true;
	}

	long getRequestResultCode() {
		if (requestFrozen)
			return requestResultCode;
		return isRequest() && !isTimeout() ? resultCode : 0;
	}

	/**
	 * 只读请求方向编码：始终按请求头与冻结的初始请求码写出，避免与应答/超时线程原地改写字段竞争。
	 */
	public ByteBuffer encodeRequest() {
		int preAllocSize = preAllocSize();
		var bb = ByteBuffer.Allocate(Math.min(HEADER_SIZE + preAllocSize, 65536));
		bb.WriteInt4s(getModuleId(), getProtocolId());
		int saveSize = bb.BeginWriteWithSize4();
		long reqCode = getRequestResultCode();
		if (reqCode == 0)
			bb.WriteUInt(FamilyClass.RaftRequest);
		else {
			bb.WriteUInt(FamilyClass.RaftRequest | FamilyClass.BitResultCode);
			bb.WriteLong(reqCode);
		}
		bb.WriteLong(getSessionId());
		unique.encode(bb);
		bb.WriteLong(createTime);
		Argument.encode(bb);
		bb.EndWriteWithSize4(saveSize);
		return bb;
	}

	@Override
	public boolean Send(@Nullable AsyncSocket socket) {
		// 直连发送每次需要独立 sessionId，故通过 RaftRpcBridge 桥接发送并回派应答；代理路径由 ProxyRequest 封装，不经此方法。
		var bridge = new RaftRpcBridge<>(this);
		bridge.Argument = Argument;
		bridge.setCreateTime(createTime);
		bridge.setUnique(unique);
		bridge.setResultCode(getRequestResultCode());
		return bridge.Send(socket, getResponseHandle(), getTimeout());
	}

	@Override
	public void encode(ByteBuffer bb) {
		var header = getFamilyClass();
		if (resultCode == 0)
			bb.WriteUInt(header);
		else {
			bb.WriteUInt(header | FamilyClass.BitResultCode);
			bb.WriteLong(resultCode);
		}
		bb.WriteLong(getSessionId());

		unique.encode(bb);
		bb.WriteLong(createTime);

		if (isRequest())
			Argument.encode(bb);
		else if (resultEncoded != null)
			// 优先写出已编码应答（如 RaftApplied 重试回放路径）。
			bb.Append(resultEncoded.bytesUnsafe(), resultEncoded.getOffset(), resultEncoded.size());
		else
			Result.encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		var header = bb.ReadUInt();
		var familyClass = header & FamilyClass.FamilyClassMask;
		if (!FamilyClass.isRaftRpc(familyClass))
			throw new IllegalStateException("invalid header(" + header + ") for decoding raft rpc " + getClass().getName());
		setRequest(familyClass == FamilyClass.RaftRequest);
		resultCode = (header & FamilyClass.BitResultCode) != 0 ? bb.ReadLong() : 0;
		setSessionId(bb.ReadLong());

		unique.decode(bb);
		createTime = bb.ReadLong();

		if (isRequest())
			Argument.decode(bb);
		else
			Result.decode(bb);
	}

	@Override
	public String toString() {
		AsyncSocket sender = getSender();
		return String.format("(Client=%s Unique=%s %s)",
				sender != null ? sender.getRemoteAddress() : null, unique, super.toString());
	}
}
