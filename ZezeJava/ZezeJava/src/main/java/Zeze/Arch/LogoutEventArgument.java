package Zeze.Arch;

import Zeze.Builtin.Online.BOnline;
import Zeze.Util.EventDispatcher;

public class LogoutEventArgument implements EventDispatcher.EventArgument {
	public final String account;
	public final String clientId;
	public final BOnline onlineData;

	public LogoutEventArgument(String account, String clientId, BOnline onlineData) {
		this.account = account;
		this.clientId = clientId;
		this.onlineData = onlineData;
	}
}
