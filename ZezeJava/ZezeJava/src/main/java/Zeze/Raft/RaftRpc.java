package Zeze.Raft;

import java.util.function.ToLongFunction;
import Zeze.Net.AsyncSocket;
import Zeze.Net.FamilyClass;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.TaskCompletionSource;

public abstract class RaftRpc<TArgument extends Serializable, TResult extends Serializable>
		extends ProxyableRpc<TArgument, TResult> implements IRaftRpc {
	private long createTime;
	private UniqueRequestId unique = new UniqueRequestId();
	private long sendTime;

	TaskCompletionSource<RaftRpc<TArgument, TResult>> future;
	ToLongFunction<Protocol<?>> handle;

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

	@Override
	public boolean Send(AsyncSocket socket) {
		// 1.
		// 通过原始raft连接方式发送请求。
		// 由于每次发送需要新的rpc.sessionId，所以这里新建了一个桥接类发送，
		// 并不会真正发送原始的RaftRpc。
		// 2.
		// 通过Proxy发送的时候，ProxyRequest已经是一个新的请求，所有的dispatch特别处理，
		// 不会使用这个方法发送请求了。
		// 3.
		// Rpc.SendResult 针对上面两个发送方式，也有两个路径。
		// 3.1 原始方式通过SessionId找到本地存根并最终找到本RaftRpc.ResponseHandle把结果派发出去。
		// 3.2 Proxy方式由ProxyRequest的嵌套ResponseHandle派发出去。其中SendResult需要拦截。see 本类。
		var bridge = new RaftRpcBridge<>(this);
		bridge.Argument = Argument;
		bridge.setCreateTime(createTime);
		bridge.setUnique(unique);
		bridge.setResultCode(this.getResultCode());
		return bridge.Send(socket, getResponseHandle(), getTimeout());
	}

	@Override
	public void encode(ByteBuffer bb) {
		var header = getFamilyClass();
		if (resultCode == 0)
			bb.WriteInt(header);
		else {
			bb.WriteInt(header | FamilyClass.BitResultCode);
			bb.WriteLong(resultCode);
		}
		bb.WriteLong(getSessionId());

		unique.encode(bb);
		bb.WriteLong(createTime);

		if (isRequest())
			Argument.encode(bb);
		else
			Result.encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		var header = bb.ReadInt();
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
