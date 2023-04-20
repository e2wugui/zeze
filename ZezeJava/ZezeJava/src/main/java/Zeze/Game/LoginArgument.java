package Zeze.Game;

import Zeze.Util.EventDispatcher;

public class LoginArgument implements EventDispatcher.EventArgument {
	public Online online;
	public String account;
	public long roleId;
}
