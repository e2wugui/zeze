package Zeze.Arch;

import Zeze.Util.EventDispatcher;

public class LoginArgument implements EventDispatcher.EventArgument {
	public final String Account;
	public final String ClientId;

	public LoginArgument(String account, String clientId) {
		Account = account;
		ClientId = clientId;
	}
}
