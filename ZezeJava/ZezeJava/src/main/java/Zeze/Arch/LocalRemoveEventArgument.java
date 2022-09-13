package Zeze.Arch;

import Zeze.Builtin.Online.BLocal;
import Zeze.Util.EventDispatcher;

public class LocalRemoveEventArgument implements EventDispatcher.EventArgument {
	public final String Account;
	public final String ClientId;
	public final BLocal LocalData;

	public LocalRemoveEventArgument(String account, String clientId, BLocal localData) {
		Account = account;
		ClientId = clientId;
		LocalData = localData;
	}
}
