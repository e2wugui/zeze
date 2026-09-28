package Zeze.Arch;

import Zeze.Util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

/**
 * 登录事件的参数：账号与 clientId。
 */
public class LoginArgument implements EventDispatcher.EventArgument {
	public final @NotNull String account;
	public final @NotNull String clientId;

	public LoginArgument(@NotNull String account, @NotNull String clientId) {
		this.account = account;
		this.clientId = clientId;
	}
}
