package Zeze.Arch;

import Zeze.Util.EventDispatcher;

public class LoginArgument implements EventDispatcher.EventArgument {
	public final String account;
	public final String clientId;

	public LoginArgument(String account, String clientId) {
		this.account = account;
		this.clientId = clientId;
	}
}
