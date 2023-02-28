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

	private void sendResponseDirectReal(Rpc<?, ?> rpc) {
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

	public void sendResponseDirect(Rpc<?, ?> rpc) {
		var t = Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(() -> sendResponseDirectReal(rpc));
		else
			sendResponseDirectReal(rpc);
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
		if (providerImpl instanceof ProviderWithOnline) {
			var online = ((ProviderWithOnline)providerImpl).getOnline();
			var roleId = getRoleId();
			if (roleId != null)
				online.sendOneByOne(List.of(roleId), link, Map.of(getLinkSid(), roleId), send);
			else
				online.send(link, Map.of(), send);
		} else if (providerImpl instanceof Zeze.Arch.ProviderWithOnline) {
			var online = ((Zeze.Arch.ProviderWithOnline)providerImpl).getOnline();
			var context = getContext();
			var loginKey = new Online.LoginKey(getAccount(), context);
			if (context != null && !context.isEmpty())
				online.sendOneByOne(List.of(loginKey), link, Map.of(getLinkSid(), loginKey), send);
			else
				online.send(link, Map.of(getLinkSid(), loginKey), send);
		} else
			send.Send(link);
	}

	public void sendResponse(long typeId, Binary fullEncodedProtocol) {
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

	private void protocolLogSend(Protocol<?> p) {
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(p.getTypeId())) {
			var roleId = getRoleId();
			if (roleId == null)
				roleId = -getLinkSid();
			AsyncSocket.log("Send", roleId, p);
		}
	}

	public void sendResponse(Protocol<?> p) {
		p.setRequest(false);
		protocolLogSend(p);
		sendResponse(p.getTypeId(), new Binary(p.encode()));
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
