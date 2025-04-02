package Zeze.Game;

import Zeze.Util.EventDispatcher;

public class LinkBrokenArgument implements EventDispatcher.EventArgument {
	public final long roleId;

	public LinkBrokenArgument(long roleId) {
		this.roleId = roleId;
	}
}
