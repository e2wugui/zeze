package Zeze.Arch;

import Zeze.Util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

public class LoginArgument implements EventDispatcher.EventArgument {
	public final @NotNull String account;
	public final @NotNull String clientId;

	public LoginArgument(@NotNull String account, @NotNull String clientId) {
		this.account = account;
		this.clientId = clientId;
	}
}
