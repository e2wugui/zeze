package Zeze.Arch;

import java.util.Map;
import java.util.TreeMap;
import Zeze.Builtin.Provider.BSend;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Builtin.Provider.Send;
import Zeze.Game.ProviderImplementWithOnline;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Transaction;
import Zeze.Util.KV;

/**
 * 用户登录会话。
 * 记录账号，roleId，LinkName，SessionId等信息。
 */
public class ProviderUserSession {
	private final Dispatch dispatch;

	public ProviderUserSession(Dispatch dispatch) {
		this.dispatch = dispatch;
	}

	public void kick(int code, String desc) {
		ProviderImplement.sendKick(getLink(), getLinkSid(), code, desc);
	}

	public final ProviderService getService() {
		return (ProviderService)dispatch.getSender().getService();
	}

	public final String getAccount() {
		return dispatch.Argument.getAccount();
	}

	public final String getContext() {
		return dispatch.Argument.getContext();
	}

	public final boolean isLogin() {
		return getContext().isEmpty();
	}

	public final Long getRoleId() {
		var context = getContext();
		return context.isEmpty() ? null : Long.parseLong(context);
	}

	public final long getLinkSid() {
		return dispatch.Argument.getLinkSid();
	}

	public final String getLinkName() {
		return ProviderService.getLinkName(getLink());
	}

	public final AsyncSocket getLink() {
		return dispatch.getSender();
	}

	public final void sendResponse(Binary fullEncodedProtocol) {
		var bytes = fullEncodedProtocol.bytesUnsafe();
		var offset = fullEncodedProtocol.getOffset();
		var moduleId = ByteBuffer.ToInt(bytes, offset);
		var protocolId = ByteBuffer.ToInt(bytes, offset + 4);
		sendResponse(Protocol.makeTypeId(moduleId, protocolId), fullEncodedProtocol);
	}

	private void sendOnline(AsyncSocket link, Send send) {
		var providerImpl = getService().providerApp.providerImplement;
		if (providerImpl instanceof ProviderWithOnline) {
			((ProviderWithOnline)providerImpl).online.send(
					link, Map.of(getLinkSid(), KV.create(getAccount(), getContext())), send);
		} else if (providerImpl instanceof ProviderImplementWithOnline) {
			var contexts = new TreeMap<Long, Long>();
			contexts.put(getLinkSid(), getRoleId());
			((ProviderImplementWithOnline)providerImpl).online.send(link, contexts, send);
		} else
			link.Send(send);
	}

	public final void sendResponse(long typeId, Binary fullEncodedProtocol) {
		var send = new Send(new BSend(typeId, fullEncodedProtocol));
		send.Argument.getLinkSids().add(getLinkSid());

		var link = getLink();
		if (link != null && !link.isClosed()) {
			sendOnline(link, send);
			return;
		}
		// 可能发生了重连，尝试再次查找发送。网络断开以后，linkSid已经不可靠了，先这样写着吧。
		var connector = getService().getLinks().get(getLinkName());
		if (connector != null && connector.isHandshakeDone()) {
			dispatch.setSender(link = connector.getSocket());
			sendOnline(link, send);
		}
	}

	public final void sendResponse(Protocol<?> p) {
		p.setRequest(false);
		if (AsyncSocket.ENABLE_PROTOCOL_LOG) {
			var linkSid = getLinkSid();
			var className = p.getClass().getSimpleName();
			if (p.isRequest()) {
				if (p instanceof Rpc)
					AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "RESP[{}] {}({}): {}", linkSid,
							className, ((Rpc<?, ?>)p).getSessionId(), p.Argument);
				else
					AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "RESP[{}] {}: {}", linkSid,
							className, p.Argument);
			} else
				AsyncSocket.logger.log(AsyncSocket.LEVEL_PROTOCOL_LOG, "RESP[{}] {}({})> {}", linkSid,
						className, ((Rpc<?, ?>)p).getSessionId(), p.getResultBean());
		}
		sendResponse(p.getTypeId(), new Binary(p.encode()));
	}

	public final void sendResponseWhileCommit(long typeId, Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendResponse(typeId, fullEncodedProtocol));
	}

	public final void sendResponseWhileCommit(Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendResponse(fullEncodedProtocol));
	}

	public final void sendResponseWhileCommit(Protocol<?> p) {
		Transaction.whileCommit(() -> sendResponse(p));
	}

	// 这个方法用来优化广播协议。不能用于Rpc，先隐藏。
	@SuppressWarnings("unused")
	private void sendResponseWhileRollback(long typeId, Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendResponse(typeId, fullEncodedProtocol));
	}

	@SuppressWarnings("unused")
	private void sendResponseWhileRollback(Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendResponse(fullEncodedProtocol));
	}

	public final void sendResponseWhileRollback(Protocol<?> p) {
		Transaction.whileCommit(() -> sendResponse(p));
	}

	public static ProviderUserSession get(Protocol<?> context) {
		var state = context.getUserState();
		if (state == null)
			throw new IllegalStateException("not auth");
		return (ProviderUserSession)state;
	}
}
