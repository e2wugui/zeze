package Zeze.Game;

import Zeze.Util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

/**
 * 角色登录事件的参数：Online 实例、账号与 roleId。
 */
public class LoginArgument implements EventDispatcher.EventArgument {
	public final @NotNull Online online;
	public final @NotNull String account;
	public final long roleId;

	public LoginArgument(@NotNull Online online, @NotNull String account, long roleId) {
		this.online = online;
		this.account = account;
		this.roleId = roleId;
	}
}
