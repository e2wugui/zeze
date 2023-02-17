package Zeze.Game;

import Zeze.Util.EventDispatcher;

public class LogoutEventArgument implements EventDispatcher.EventArgument {
	public long roleId;
	public LogoutReason logoutReason;
}
