package Zeze.Arch;

import Zeze.Builtin.Online.BOnline;
import Zeze.Util.EventDispatcher;

public class LogoutEventArgument implements EventDispatcher.EventArgument {
	public final String Account;
	public final String ClientId;
	public final BOnline OnlineData;

	public LogoutEventArgument(String account, String clientId, BOnline onlineData) {
		Account = account;
		ClientId = clientId;
		OnlineData = onlineData;
	}
}
