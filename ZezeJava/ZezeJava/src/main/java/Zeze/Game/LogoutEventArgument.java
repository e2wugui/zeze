package Zeze.Game;

import Zeze.Util.EventDispatcher;

/**
 * 角色登出事件的参数：Online 实例、roleId 与登出原因。
 */
public class LogoutEventArgument implements EventDispatcher.EventArgument {
	public final Online online;
	public final long roleId;
	public final LogoutReason logoutReason;

	public LogoutEventArgument(Online online, long roleId, LogoutReason logoutReason) {
		this.online = online;
		this.roleId = roleId;
		this.logoutReason = logoutReason;
	}
}
