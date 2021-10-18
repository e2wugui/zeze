package Game.Login;

import Zeze.Net.*;
import Zeze.Transaction.*;
import Game.*;
import Zeze.Transaction.Collections.PList1;

import java.util.*;

public class Session {
	private String Account;
	public final String getAccount() {
		return Account;
	}
	private Long RoleId = null;
	public final Long getRoleId() {
		return RoleId;
	}

	private String LinkName;
	public final String getLinkName() {
		return LinkName;
	}
	private long SessionId;
	public final long getSessionId() {
		return SessionId;
	}

	private AsyncSocket Link;
	public final AsyncSocket getLink() {
		return Link;
	}
	public final void setLink(AsyncSocket value) {
		Link = value;
	}

	public Session(String account, PList1<Long> states, AsyncSocket link, long linkSid) {
		Account = account;
		RoleId = states.isEmpty() ? null : states.get(0);
		SessionId = linkSid;
		setLink(link);
		LinkName = App.getInstance().Server.GetLinkName(link);
	}

	public final void SendResponse(Binary fullEncodedProtocol) {
		SendResponse(Zeze.Serialize.ByteBuffer.Wrap(fullEncodedProtocol).ReadInt4(), fullEncodedProtocol);
	}

	public final void SendResponse(int typeId, Binary fullEncodedProtocol) {
		var send = new Zezex.Provider.Send();
		send.Argument.getLinkSids().add(getSessionId());
		send.Argument.setProtocolType(typeId);
		send.Argument.setProtocolWholeData(fullEncodedProtocol);

		if (null != getLink() && null != getLink().getSocket()) {
			getLink().Send(send);
			return;
		}
		// 可能发生了重连，尝试再次查找发送。网络断开以后，已经不可靠了，先这样写着吧。
		var link = App.getInstance().Server.getLinks().get(getLinkName());
		if (null != link) {
			if (link.isHandshakeDone()) {
				setLink(link.getSocket());
				link.getSocket().Send(send);
			}
		}
	}

	public final void SendResponse(Protocol p) {
		p.setRequest(false);
		SendResponse(p.getTypeId(), new Binary(p.Encode()));
	}

	public final void SendResponseWhileCommit(int typeId, Binary fullEncodedProtocol) {
		Transaction.getCurrent().RunWhileCommit(() -> SendResponse(typeId, fullEncodedProtocol));
	}

	public final void SendResponseWhileCommit(Binary fullEncodedProtocol) {
		Transaction.getCurrent().RunWhileCommit(() -> SendResponse(fullEncodedProtocol));
	}

	public final void SendResponseWhileCommit(Protocol p) {
		Transaction.getCurrent().RunWhileCommit(() -> SendResponse(p));
	}

	// 这个方法用来优化广播协议。不能用于Rpc，先隐藏。
	private void SendResponseWhileRollback(int typeId, Binary fullEncodedProtocol) {
		Transaction.getCurrent().RunWhileRollback(() -> SendResponse(typeId, fullEncodedProtocol));
	}

	private void SendResponseWhileRollback(Binary fullEncodedProtocol) {
		Transaction.getCurrent().RunWhileRollback(() -> SendResponse(fullEncodedProtocol));
	}

	public final void SendResponseWhileRollback(Protocol p) {
		Transaction.getCurrent().RunWhileRollback(() -> SendResponse(p));
	}

	public static Session Get(Protocol context) {
		if (null == context.UserState) {
			throw new RuntimeException("not auth");
		}
		return (Session)context.UserState;
	}
}