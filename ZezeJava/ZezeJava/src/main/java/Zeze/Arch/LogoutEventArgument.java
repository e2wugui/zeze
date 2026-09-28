package Zeze.Arch;

import Zeze.Util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

/**
 * 登出事件的参数：账号与 clientId。
 */
public class LogoutEventArgument implements EventDispatcher.EventArgument {
	public final @NotNull String account;
	public final @NotNull String clientId;

	public LogoutEventArgument(@NotNull String account, @NotNull String clientId) {
		this.account = account;
		this.clientId = clientId;
	}
}
