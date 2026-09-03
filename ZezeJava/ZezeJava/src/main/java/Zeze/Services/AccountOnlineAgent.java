package Zeze.Services;

import java.util.ArrayList;
import java.util.Comparator;
import Zeze.Builtin.AccountOnline.BAccountLink;
import Zeze.Builtin.AccountOnline.BLogin;
import Zeze.Builtin.AccountOnline.Login;
import Zeze.Builtin.AccountOnline.Logout;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.Action2;

public class AccountOnlineAgent extends AbstractAccountOnlineAgent {
	private final AccountOnlineAgentService service;
	private final ArrayList<Connector> connectors = new ArrayList<>();
	private volatile Action2<String, Long> kickHandle;

	public AccountOnlineAgent(Config config) {
		service = new AccountOnlineAgentService(config);
		RegisterProtocols(service);
		service.getConfig().forEachConnector(connectors::add);
		if (connectors.isEmpty())
			throw new RuntimeException("connector missing.");
		connectors.sort(Comparator.comparing(Connector::getName));
	}

	public Action2<String, Long> getKickHandle() {
		return kickHandle;
	}

	public void setKickHandle(Action2<String, Long> kickHandle) {
		this.kickHandle = kickHandle;
	}

	public void start() throws Exception {
		service.start();
	}

	public void ready() {
		for (var c : connectors)
			c.GetReadySocket();
	}

	public void stop() throws Exception {
		service.stop();
	}

	public void login(String account, String linkName, long linkSid, boolean isKickOld,
					  ProtocolHandle<Rpc<BLogin.Data, EmptyBean.Data>> handle) {
		var r = new Login();
		r.Argument.getAccountLink().setAccount(account);
		r.Argument.getAccountLink().setLinkName(linkName);
		r.Argument.getAccountLink().setLinkSid(linkSid);
		r.Argument.setKickOld(isKickOld);
		var index = Integer.remainderUnsigned(account.hashCode(), connectors.size());
		var socket = connectors.get(index).TryGetReadySocket();
		// Send对null socket直接return false且回调永不触发，上层Auth流程吊死到自身超时；
		// SendReturnVoid总是建立上下文，socket为null时handle在超时后以Procedure.Timeout回调，快速失败。
		r.SendReturnVoid(service, socket, handle);
	}

	public void logout(String account, String linkName, long linkSid,
					   ProtocolHandle<Rpc<BAccountLink.Data, EmptyBean.Data>> handle) {
		var r = new Logout();
		r.Argument.setAccount(account);
		r.Argument.setLinkName(linkName);
		r.Argument.setLinkSid(linkSid);
		var index = Integer.remainderUnsigned(account.hashCode(), connectors.size());
		var socket = connectors.get(index).TryGetReadySocket();
		r.SendReturnVoid(service, socket, handle); // 同login：发送失败也要回调（FND-S3-11）
	}

	@Override
	protected long ProcessKickRequest(Zeze.Builtin.AccountOnline.Kick r) throws Exception {
		if (kickHandle != null)
			kickHandle.run(r.Argument.getAccount(), r.Argument.getLinkSid());
		r.SendResult();
		return 0;
	}

	public static class AccountOnlineAgentService extends Service {
		public AccountOnlineAgentService(Config config) {
			super("Zeze.Service.AccountOnlineAgent", config);
			setNoProcedure(true);
		}
	}
}
