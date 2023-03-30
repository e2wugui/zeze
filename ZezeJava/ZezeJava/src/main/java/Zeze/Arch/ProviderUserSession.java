package Zeze.Arch;

import java.util.List;
import java.util.Map;
import Zeze.Arch.Beans.BSend;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Builtin.Provider.Send;
import Zeze.Game.ProviderWithOnline;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Transaction;
import Zeze.Util.PerfCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 用户登录会话。
 * 记录账号，roleId，LinkName，SessionId等信息。
 */
public class ProviderUserSession {
	protected final @NotNull Dispatch dispatch;

	public ProviderUserSession(@NotNull Dispatch dispatch) {
		this.dispatch = dispatch;
	}

	public void kick(int code, @NotNull String desc) {
		ProviderImplement.sendKick(getLink(), getLinkSid(), code, desc);
	}

	public @NotNull ProviderService getService() {
		return (ProviderService)dispatch.getSender().getService();
	}

	public String getAccount() {
		return dispatch.Argument.getAccount();
	}

	public String getContext() {
		return dispatch.Argument.getContext();
	}

	public boolean isLogin() {
		return getContext().isEmpty();
	}

	public @Nullable Long getRoleId() {
		var context = getContext();
		return context.isEmpty() ? null : Long.parseLong(context);
	}

	public long getLinkSid() {
		return dispatch.Argument.getLinkSid();
	}

	public @NotNull String getLinkName() {
		return ProviderService.getLinkName(getLink());
	}

	public AsyncSocket getLink() {
		return dispatch.getSender();
	}

	private void sendResponseDirectReal(@NotNull Rpc<?, ?> rpc) {
		rpc.setRequest(false);
		protocolLogSend(rpc);
		var pdata = new Binary(rpc.encode());
		var send = new Send(new BSend(rpc.getTypeId(), pdata));
		send.Argument.getLinkSids().add(getLinkSid());

		var link = getLink();
		if (link != null && !link.isClosed()) {
			var r = send.Send(link);
			if (PerfCounter.ENABLE_PERF && r)
				PerfCounter.instance.addSendInfo(rpc, pdata.size(), 1);
			return;
		}
		// 可能发生了重连，尝试再次查找发送。网络断开以后，linkSid已经不可靠了，先这样写着吧。
		var connector = getService().getLinks().get(getLinkName());
		if (connector != null && connector.isHandshakeDone()) {
			dispatch.setSender(link = connector.getSocket());
			var r = send.Send(link);
			if (PerfCounter.ENABLE_PERF && r)
				PerfCounter.instance.addSendInfo(rpc, pdata.size(), 1);
		}
	}

	public void sendResponseDirect(@NotNull Rpc<?, ?> rpc) {
		var t = Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(() -> sendResponseDirectReal(rpc));
		else
			sendResponseDirectReal(rpc);
	}

	public void sendResponse(@NotNull Binary fullEncodedProtocol) {
		var bytes = fullEncodedProtocol.bytesUnsafe();
		var offset = fullEncodedProtocol.getOffset();
		var moduleId = ByteBuffer.ToInt(bytes, offset);
		var protocolId = ByteBuffer.ToInt(bytes, offset + 4);
		sendResponse(Protocol.makeTypeId(moduleId, protocolId), fullEncodedProtocol);
	}

	protected boolean sendOnline(AsyncSocket link, @NotNull Send send) {
		var providerImpl = getService().providerApp.providerImplement;
		if (providerImpl instanceof ProviderWithOnline) {
			var online = ((ProviderWithOnline)providerImpl).getOnline();
			var roleId = getRoleId();
			if (roleId != null)
				return online.send(/*List.of(roleId),*/link, Map.of(getLinkSid(), roleId), send);
			return online.send(link, Map.of(), send);
		}
		if (providerImpl instanceof Zeze.Arch.ProviderWithOnline) {
			var online = ((Zeze.Arch.ProviderWithOnline)providerImpl).getOnline();
			var context = getContext();
			var loginKey = new Online.LoginKey(getAccount(), context);
			if (context != null && !context.isEmpty())
				return online.send(List.of(loginKey), link, Map.of(getLinkSid(), loginKey), send);
			return online.send(link, Map.of(getLinkSid(), loginKey), send);
		}
		return send.Send(link);
	}

	public boolean sendResponse(long typeId, @NotNull Binary fullEncodedProtocol) {
		var send = new Send(new BSend(typeId, fullEncodedProtocol));
		send.Argument.getLinkSids().add(getLinkSid());

		var link = getLink();
		if (link != null && !link.isClosed())
			return sendOnline(link, send);
		// 可能发生了重连，尝试再次查找发送。网络断开以后，linkSid已经不可靠了，先这样写着吧。
		var connector = getService().getLinks().get(getLinkName());
		if (connector != null && connector.isHandshakeDone()) {
			dispatch.setSender(link = connector.getSocket());
			return sendOnline(link, send);
		}
		return false;
	}

	private void protocolLogSend(@NotNull Protocol<?> p) {
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(p.getTypeId())) {
			var roleId = getRoleId();
			if (roleId == null)
				roleId = -getLinkSid();
			AsyncSocket.log("Send", roleId, p);
		}
	}

	public void sendResponse(@NotNull Protocol<?> p) {
		p.setRequest(false);
		protocolLogSend(p);
		var pdata = new Binary(p.encode());
		var r = sendResponse(p.getTypeId(), pdata);
		if (PerfCounter.ENABLE_PERF && r)
			PerfCounter.instance.addSendInfo(p, pdata.size(), 1);
	}

	public void sendResponseWhileCommit(long typeId, @NotNull Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendResponse(typeId, fullEncodedProtocol));
	}

	public void sendResponseWhileCommit(@NotNull Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendResponse(fullEncodedProtocol));
	}

	public void sendResponseWhileCommit(@NotNull Protocol<?> p) {
		Transaction.whileCommit(() -> sendResponse(p));
	}

	// 这个方法用来优化广播协议。不能用于Rpc，先隐藏。
	@SuppressWarnings("unused")
	protected void sendResponseWhileRollback(long typeId, @NotNull Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> sendResponse(typeId, fullEncodedProtocol));
	}

	@SuppressWarnings("unused")
	protected void sendResponseWhileRollback(@NotNull Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> sendResponse(fullEncodedProtocol));
	}

	public void sendResponseWhileRollback(@NotNull Protocol<?> p) {
		Transaction.whileRollback(() -> sendResponse(p));
	}

	public static @NotNull ProviderUserSession get(@NotNull Protocol<?> context) {
		var state = context.getUserState();
		if (state == null)
			throw new IllegalStateException("not auth");
		return (ProviderUserSession)state;
	}
}
