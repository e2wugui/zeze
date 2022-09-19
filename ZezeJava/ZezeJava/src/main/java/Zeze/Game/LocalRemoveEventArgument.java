package Zeze.Game;

import Zeze.Builtin.Game.Online.BLocal;
import Zeze.Util.EventDispatcher;

public class LocalRemoveEventArgument implements EventDispatcher.EventArgument {
	public long roleId;
	public BLocal localData;
}
