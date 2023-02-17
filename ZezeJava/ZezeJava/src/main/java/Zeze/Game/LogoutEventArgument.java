package Zeze.Game;

import Zeze.Builtin.Game.Online.BOnline;
import Zeze.Builtin.Game.Online.BVersion;
import Zeze.Util.EventDispatcher;

public class LogoutEventArgument implements EventDispatcher.EventArgument {
	public long roleId;
	public BOnline online;
	public LogoutReason logoutReason;
}
