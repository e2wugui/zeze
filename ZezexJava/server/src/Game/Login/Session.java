package Game.Login;

import Zeze.Net.*;
import Zeze.Transaction.*;
import Game.*;
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

	public Session(String account, List<Long> states, AsyncSocket link, long linkSid) {
		Account = account;
		RoleId = states.isEmpty() ? null : states.get(0);
		SessionId = linkSid;
		setLink(link);
		LinkName = App.getInstance().getServer().GetLinkName(link);
	}

	public final void SendResponse(Binary fullEncodedProtocol) {
		SendResponse(Zeze.Serialize.ByteBuffer.Wrap(fullEncodedProtocol).ReadInt4(), fullEncodedProtocol);
	}

	public final void SendResponse(int typeId, Binary fullEncodedProtocol) {
		var send = new Zezex.Provider.Send();
		send.getArgument().getLinkSids().Add(getSessionId());
		send.getArgument().ProtocolType = typeId;
		send.getArgument().ProtocolWholeData = fullEncodedProtocol;

		if (null != getLink() && null != getLink().Socket) {
			getLink().Send(send);
			return;
		}
		// 可能发生了重连，尝试再次查找发送。网络断开以后，已经不可靠了，先这样写着吧。
		TValue link;
		tangible.OutObject<TValue> tempOut_link = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (App.getInstance().getServer().getLinks().TryGetValue(getLinkName(), tempOut_link)) {
		link = tempOut_link.outArgValue;
			if (link.IsHandshakeDone) {
				setLink(link.Socket);
				link.Socket.Send(send);
			}
		}
	else {
		link = tempOut_link.outArgValue;
	}
	}

	public final void SendResponse(Protocol p) {
		p.IsRequest = false;
		SendResponse(p.TypeId, new Binary(p.Encode()));
	}

	public final void SendResponseWhileCommit(int typeId, Binary fullEncodedProtocol) {
		Transaction.Current.RunWhileCommit(() -> SendResponse(typeId, fullEncodedProtocol));
	}

	public final void SendResponseWhileCommit(Binary fullEncodedProtocol) {
		Transaction.Current.RunWhileCommit(() -> SendResponse(fullEncodedProtocol));
	}

	public final void SendResponseWhileCommit(Protocol p) {
		Transaction.Current.RunWhileCommit(() -> SendResponse(p));
	}

	// 这个方法用来优化广播协议。不能用于Rpc，先隐藏。
	private void SendResponseWhileRollback(int typeId, Binary fullEncodedProtocol) {
		Transaction.Current.RunWhileRollback(() -> SendResponse(typeId, fullEncodedProtocol));
	}

	private void SendResponseWhileRollback(Binary fullEncodedProtocol) {
		Transaction.Current.RunWhileRollback(() -> SendResponse(fullEncodedProtocol));
	}

	public final void SendResponseWhileRollback(Protocol p) {
		Transaction.Current.RunWhileRollback(() -> SendResponse(p));
	}

	public static Session Get(Protocol context) {
		if (null == context.UserState) {
			throw new RuntimeException("not auth");
		}
		return (Session)context.UserState;
	}
}