package Zeze.Arch;

import java.util.Map;
import java.util.TreeMap;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Builtin.Provider.Send;
import Zeze.Game.ProviderWithOnline;
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
	protected final Dispatch dispatch;

	public ProviderUserSession(Dispatch dispatch) {
		this.dispatch = dispatch;
	}

	public void kick(int code, String desc) {
		ProviderImplement.sendKick(getLink(), getLinkSid(), code, desc);
	}

	public ProviderService getService() {
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

	public Long getRoleId() {
		var context = getContext();
		return context.isEmpty() ? null : Long.parseLong(context);
	}

	public long getLinkSid() {
		return dispatch.Argument.getLinkSid();
	}

	public String getLinkName() {
		return ProviderService.getLinkName(getLink());
	}

	public AsyncSocket getLink() {
		return dispatch.getSender();
	}

	public void sendResponse(Binary fullEncodedProtocol) {
		var bytes = fullEncodedProtocol.bytesUnsafe();
		var offset = fullEncodedProtocol.getOffset();
		var moduleId = ByteBuffer.ToInt(bytes, offset);
		var protocolId = ByteBuffer.ToInt(bytes, offset + 4);
		sendResponse(Protocol.makeTypeId(moduleId, protocolId), fullEncodedProtocol);
	}

	protected void sendOnline(AsyncSocket link, Send send) {
		var providerImpl = getService().providerApp.providerImplement;
		if (providerImpl instanceof Zeze.Arch.ProviderWithOnline) {
			((Zeze.Arch.ProviderWithOnline)providerImpl).getOnline().send(
					link, Map.of(getLinkSid(), KV.create(getAccount(), getContext())), send);
		} else if (providerImpl instanceof ProviderWithOnline) {
			var contexts = new TreeMap<Long, Long>();
			contexts.put(getLinkSid(), getRoleId());
			((ProviderWithOnline)providerImpl).getOnline().send(link, contexts, send);
		} else
			send.Send(link);
	}

	public void sendResponse(long typeId, Binary fullEncodedProtocol) {
		var send = new Send(new Zeze.Arch.Beans.BSend(typeId, fullEncodedProtocol));
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

	public void sendResponse(Protocol<?> p) {
		p.setRequest(false);
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
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
		sendResponse(typeId, new Binary(p.encode()));
	}

	public void sendResponseWhileCommit(long typeId, Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendResponse(typeId, fullEncodedProtocol));
	}

	public void sendResponseWhileCommit(Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendResponse(fullEncodedProtocol));
	}

	public void sendResponseWhileCommit(Protocol<?> p) {
		Transaction.whileCommit(() -> sendResponse(p));
	}

	// 这个方法用来优化广播协议。不能用于Rpc，先隐藏。
	@SuppressWarnings("unused")
	protected void sendResponseWhileRollback(long typeId, Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendResponse(typeId, fullEncodedProtocol));
	}

	@SuppressWarnings("unused")
	protected void sendResponseWhileRollback(Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendResponse(fullEncodedProtocol));
	}

	public void sendResponseWhileRollback(Protocol<?> p) {
		Transaction.whileCommit(() -> sendResponse(p));
	}

	public static ProviderUserSession get(Protocol<?> context) {
		var state = context.getUserState();
		if (state == null)
			throw new IllegalStateException("not auth");
		return (ProviderUserSession)state;
	}
}
