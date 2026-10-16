package Zeze.Game;

import Zeze.Util.EventDispatcher;

/**
 * 角色登录事件的参数：Online 实例、账号与 roleId。
 */
public class LoginArgument implements EventDispatcher.EventArgument {
	public final Online online;
	public final String account;
	public final long roleId;

	public LoginArgument(Online online, String account, long roleId) {
		this.online = online;
		this.account = account;
		this.roleId = roleId;
	}
}
