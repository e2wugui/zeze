package Zeze.Raft;

import java.util.function.ToLongFunction;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.TaskCompletionSource;

public abstract class RaftRpc<TArgument extends Bean, TResult extends Bean> extends Rpc<TArgument, TResult> implements IRaftRpc {
	private long createTime;
	private UniqueRequestId unique = new UniqueRequestId();
	private long sendTime;
	private boolean urgent;

	TaskCompletionSource<RaftRpc<TArgument, TResult>> future;
	ToLongFunction<Protocol<?>> handle;

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
		var bridge = new RaftRpcBridge<>(this);
		bridge.Argument = Argument;
		bridge.setCreateTime(createTime);
		bridge.setUnique(unique);
		bridge.setResultCode(this.getResultCode());
		return bridge.Send(socket, getResponseHandle(), getTimeout());
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBool(isRequest());
		bb.WriteLong(getSessionId());
		bb.WriteLong(getResultCode());
		unique.encode(bb);
		bb.WriteLong(createTime);

		if (isRequest())
			Argument.encode(bb);
		else
			Result.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		setRequest(bb.ReadBool());
		setSessionId(bb.ReadLong());
		setResultCode(bb.ReadLong());
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
