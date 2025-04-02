package Zeze.Game;

import Zeze.Util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

public class LogoutEventArgument implements EventDispatcher.EventArgument {
	public final @NotNull Online online;
	public final long roleId;
	public final @NotNull LogoutReason logoutReason;

	public LogoutEventArgument(@NotNull Online online, long roleId, @NotNull LogoutReason logoutReason) {
		this.online = online;
		this.roleId = roleId;
		this.logoutReason = logoutReason;
	}
}
