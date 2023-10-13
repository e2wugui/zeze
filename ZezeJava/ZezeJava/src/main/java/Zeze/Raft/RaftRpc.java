package Zeze.Raft;

import java.util.function.ToLongFunction;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.FamilyClass;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.TaskCompletionSource;
import org.jetbrains.annotations.Nullable;

public abstract class RaftRpc<TArgument extends Serializable, TResult extends Serializable> extends Rpc<TArgument, TResult> implements IRaftRpc {
	private long createTime;
	private UniqueRequestId unique = new UniqueRequestId();
	private long sendTime;
	private boolean urgent;
	private ProxyRequest proxyRequest;

	TaskCompletionSource<RaftRpc<TArgument, TResult>> future;
	ToLongFunction<Protocol<?>> handle;

	public void setProxyRequest(ProxyRequest proxyRequest) {
		this.proxyRequest = proxyRequest;
	}

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

	public boolean isUrgent() {
		return urgent;
	}

	public void setUrgent(boolean urgent) {
		this.urgent = urgent;
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
	public void SendResult(@Nullable Binary result) {
		if (proxyRequest == null) {
			// 原始raft连接方式。
			super.SendResult(result);
			return;
		}

		// proxy 方式，基本逻辑拷贝自 Rpc.SendResult(Binary result)。
		if (sendResultDone) {
			logger.error("Rpc.SendResult Already Done: {} {}", getSender(), this, new Exception());
			return;
		}
		sendResultDone = true;
		resultEncoded = result;
		setRequest(false);

		// 填写proxyRequest.Result并发送。
		proxyRequest.Result.setData(new Binary(this.encode()));
		proxyRequest.SendResult();
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
	public void decode(ByteBuffer bb) {
		var header = bb.ReadInt();
		var familyClass = header & FamilyClass.FamilyClassMask;
		if (!FamilyClass.isRaftRpc(familyClass))
			throw new IllegalStateException("invalid header(" + header + ") for decoding raft rpc " + getClass());
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
