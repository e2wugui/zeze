package Zeze.Arch;

import Zeze.Util.EventDispatcher;

/**
 * 登录事件的参数：账号与 clientId。
 */
public class LoginArgument implements EventDispatcher.EventArgument {
	public final String account;
	public final String clientId;

	public LoginArgument(String account, String clientId) {
		this.account = account;
		this.clientId = clientId;
	}
}
