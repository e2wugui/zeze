package Zeze.Arch;

import Zeze.Util.EventDispatcher;

/**
 * 登出事件的参数：账号与 clientId。
 */
public class LogoutEventArgument implements EventDispatcher.EventArgument {
	public final String account;
	public final String clientId;

	public LogoutEventArgument(String account, String clientId) {
		this.account = account;
		this.clientId = clientId;
	}
}
