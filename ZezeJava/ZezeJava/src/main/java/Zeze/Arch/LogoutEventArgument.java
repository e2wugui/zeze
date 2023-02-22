package Zeze.Arch;

import Zeze.Util.EventDispatcher;

public class LogoutEventArgument implements EventDispatcher.EventArgument {
	public final String account;
	public final String clientId;

	public LogoutEventArgument(String account, String clientId) {
		this.account = account;
		this.clientId = clientId;
	}
}
