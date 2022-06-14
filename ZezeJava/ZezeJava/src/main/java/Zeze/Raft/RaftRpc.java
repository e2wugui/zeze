package Zeze.Raft;

import java.util.function.ToLongFunction;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.TaskCompletionSource;

public abstract class RaftRpc<TArgument extends Bean, TResult extends Bean> extends Rpc<TArgument, TResult> implements IRaftRpc {
	private long CreateTime;
	private UniqueRequestId Unique = new UniqueRequestId();
	private long SendTime;
	private boolean Urgent;

	TaskCompletionSource<RaftRpc<TArgument, TResult>> Future;
	ToLongFunction<Protocol<?>> Handle;

	@Override
	public long getCreateTime() {
		return CreateTime;
	}

	@Override
	public void setCreateTime(long value) {
		CreateTime = value;
	}

	@Override
	public UniqueRequestId getUnique() {
		return Unique;
	}

	@Override
	public void setUnique(UniqueRequestId value) {
		Unique = value;
	}

	@Override
	public long getSendTime() {
		return SendTime;
	}

	@Override
	public void setSendTime(long value) {
		SendTime = value;
	}

	public boolean isUrgent() {
		return Urgent;
	}

	public void setUrgent(boolean urgent) {
		Urgent = urgent;
	}

	@Override
	public boolean Send(AsyncSocket socket) {
		var bridge = new RaftRpcBridge<>(this);
		bridge.Argument = Argument;
		bridge.setCreateTime(CreateTime);
		bridge.setUnique(Unique);
		bridge.setResultCode(this.getResultCode());
		return bridge.Send(socket, getResponseHandle(), getTimeout());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteBool(isRequest());
		bb.WriteLong(SessionId);
		bb.WriteLong(getResultCode());
		Unique.Encode(bb);
		bb.WriteLong(CreateTime);

		if (isRequest())
			Argument.Encode(bb);
		else
			Result.Encode(bb);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setRequest(bb.ReadBool());
		SessionId = bb.ReadLong();
		setResultCode(bb.ReadLong());
		Unique.Decode(bb);
		CreateTime = bb.ReadLong();

		if (isRequest())
			Argument.Decode(bb);
		else
			Result.Decode(bb);
	}

	@Override
	public String toString() {
		AsyncSocket sender = getSender();
		return String.format("(Client=%s Unique=%s %s)",
				sender != null ? sender.getRemoteAddress() : null, Unique, super.toString());
	}
}
