package Zeze.Arch;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 用户登录会话。
 * 记录账号，roleId，LinkName，SessionId等信息。
 */
public class ProviderUserSession {
	private static final Logger logger = LogManager.getLogger(ProviderUserSession.class);

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

	public @NotNull String getAccount() {
		return dispatch.Argument.getAccount();
	}

	public @NotNull String getContext() {
		return dispatch.Argument.getContext();
	}

	public @NotNull String getOnlineSetName() {
		return dispatch.Argument.getOnlineSetName();
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
		var send = new Send(new BSend(rpc.getTypeId(), new Binary(rpc.encode())));
		send.Argument.getLinkSids().add(getLinkSid());

		var link = getLink();
		if (link != null && !link.isClosed()) {
			send.Send(link);
			return;
		}
		// 可能发生了重连，尝试再次查找发送。网络断开以后，linkSid已经不可靠了，先这样写着吧。
		var connector = getService().getLinks().get(getLinkName());
		if (connector != null && connector.isHandshakeDone()) {
			dispatch.setSender(link = connector.getSocket());
			send.Send(link);
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
			var roleId = getRoleId();
			if (roleId != null) {
				var name = dispatch.Argument.getOnlineSetName();
				var online = ((ProviderWithOnline)providerImpl).getOnline(name);
				if (online != null)
					return online.send(link, Map.of(getLinkSid(), roleId), send);
				logger.error("unknown onlineSetName: {}", name);
			}
			// 没有登录的会话不需要转给Online处理。转给Online是为了处理发送失败的结果。
			// 这种情况下，忽略发送结果。
			return send.Send(link);
		}
		if (providerImpl instanceof Zeze.Arch.ProviderWithOnline) {
			var online = ((Zeze.Arch.ProviderWithOnline)providerImpl).getOnline();
			var context = getContext();
			var loginKey = new Online.LoginKey(getAccount(), context);
			if (!context.isEmpty())
				return online.send(link, Map.of(getLinkSid(), loginKey), send);
			// 没有登录的会话不需要转给Online处理。转给Online是为了处理发送失败的结果。
			// 这种情况下，忽略发送结果。
			return send.Send(link);
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
			AsyncSocket.log("Send", roleId, dispatch.Argument.getOnlineSetName(), p);
		}
	}

	public void trySendResponse(@NotNull Protocol<?> p, long resultCode) {
		if (p.isRequest()) {
			p.setResultCode(resultCode);
			sendResponse(p);
		}
	}

	public void sendResponse(@NotNull Protocol<?> p) {
		p.setRequest(false);
		protocolLogSend(p);
		sendResponse(p.getTypeId(), new Binary(p.encode()));
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

	@Override
	public String toString() {
		return "(account=" + dispatch.Argument.getAccount() + ",roleId=" + dispatch.Argument.getContext() + ")";
	}
}
