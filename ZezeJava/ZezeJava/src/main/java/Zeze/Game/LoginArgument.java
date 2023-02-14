package Zeze.Game;

import Zeze.Builtin.Game.Online.BOnline;
import Zeze.Builtin.Game.Online.BVersion;
import Zeze.Util.EventDispatcher;

public class LoginArgument implements EventDispatcher.EventArgument {
	public String account;
	public long roleId;
	public BOnline online;
	public BVersion version;
}
