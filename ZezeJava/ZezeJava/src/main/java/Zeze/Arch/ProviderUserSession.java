package Zeze.Arch;

import Zeze.Beans.Provider.Send;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.PList1;
import Zeze.Transaction.Transaction;

/**
 * 用户登录会话。
 * 记录账号，roleId，LinkName，SessionId等信息。
 */
public class ProviderUserSession {
	private final ProviderService service;
	private final String Account;
	private final Long RoleId;
	private final long SessionId;
	private final String LinkName;
	private AsyncSocket Link;

	public ProviderUserSession(ProviderService service, String account, PList1<Long> states, AsyncSocket link, long linkSid) {
		this.service = service;
		Account = account;
		RoleId = states.isEmpty() ? null : states.get(0);
		SessionId = linkSid;
		LinkName = service.GetLinkName(link);
		Link = link;
	}

	public final ProviderService getService() {
		return service;
	}

	public final String getAccount() {
		return Account;
	}

	public final Long getRoleId() {
		return RoleId;
	}

	public final long getSessionId() {
		return SessionId;
	}

	public final String getLinkName() {
		return LinkName;
	}

	public final AsyncSocket getLink() {
		return Link;
	}

	public final void setLink(AsyncSocket value) {
		Link = value;
	}

	public final void SendResponse(Binary fullEncodedProtocol) {
		SendResponse(ByteBuffer.Wrap(fullEncodedProtocol).ReadInt4(), fullEncodedProtocol);
	}

	public final void SendResponse(long typeId, Binary fullEncodedProtocol) {
		var send = new Send();
		send.Argument.getLinkSids().add(getSessionId());
		send.Argument.setProtocolType(typeId);
		send.Argument.setProtocolWholeData(fullEncodedProtocol);

		if (null != getLink() && !getLink().isClosed()) {
			getLink().Send(send);
			return;
		}
		// 可能发生了重连，尝试再次查找发送。网络断开以后，已经不可靠了，先这样写着吧。
		var link = service.getLinks().get(getLinkName());
		if (null != link) {
			if (link.isHandshakeDone()) {
				setLink(link.getSocket());
				link.getSocket().Send(send);
			}
		}
	}

	public final void SendResponse(Protocol<?> p) {
		p.setRequest(false);
		SendResponse(p.getTypeId(), new Binary(p.Encode()));
	}

	@SuppressWarnings("ConstantConditions")
	public final void SendResponseWhileCommit(int typeId, Binary fullEncodedProtocol) {
		Transaction.getCurrent().RunWhileCommit(() -> SendResponse(typeId, fullEncodedProtocol));
	}

	@SuppressWarnings("ConstantConditions")
	public final void SendResponseWhileCommit(Binary fullEncodedProtocol) {
		Transaction.getCurrent().RunWhileCommit(() -> SendResponse(fullEncodedProtocol));
	}

	@SuppressWarnings("ConstantConditions")
	public final void SendResponseWhileCommit(Protocol<?> p) {
		Transaction.getCurrent().RunWhileCommit(() -> SendResponse(p));
	}

	// 这个方法用来优化广播协议。不能用于Rpc，先隐藏。
	@SuppressWarnings({"ConstantConditions", "unused"})
	private void SendResponseWhileRollback(int typeId, Binary fullEncodedProtocol) {
		Transaction.getCurrent().RunWhileRollback(() -> SendResponse(typeId, fullEncodedProtocol));
	}

	@SuppressWarnings({"ConstantConditions", "unused"})
	private void SendResponseWhileRollback(Binary fullEncodedProtocol) {
		Transaction.getCurrent().RunWhileRollback(() -> SendResponse(fullEncodedProtocol));
	}

	@SuppressWarnings("ConstantConditions")
	public final void SendResponseWhileRollback(Protocol<?> p) {
		Transaction.getCurrent().RunWhileRollback(() -> SendResponse(p));
	}

	public static ProviderUserSession Get(Protocol<?> context) {
		if (null == context.getUserState()) {
			throw new RuntimeException("not auth");
		}
		return (ProviderUserSession)context.getUserState();
	}
}
