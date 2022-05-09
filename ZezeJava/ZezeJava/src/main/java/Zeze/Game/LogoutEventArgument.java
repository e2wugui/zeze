package Zeze.Game;

import Zeze.Builtin.Game.Online.BOnline;
import Zeze.Util.EventDispatcher;

public class LogoutEventArgument implements EventDispatcher.EventArgument {
	public long RoleId;
	public BOnline OnlineData;
}
