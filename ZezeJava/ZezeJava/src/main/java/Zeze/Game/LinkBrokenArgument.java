package Zeze.Game;

import Zeze.Util.EventDispatcher;

/**
 * 链路断开事件的参数：roleId。
 */
public class LinkBrokenArgument implements EventDispatcher.EventArgument {
	public final long roleId;

	public LinkBrokenArgument(long roleId) {
		this.roleId = roleId;
	}
}
