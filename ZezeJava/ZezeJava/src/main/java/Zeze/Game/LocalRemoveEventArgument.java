package Zeze.Game;

import Zeze.Builtin.Game.Online.BLocal;
import Zeze.Util.EventDispatcher;
import org.jetbrains.annotations.Nullable;

public class LocalRemoveEventArgument implements EventDispatcher.EventArgument {
	public final long roleId;
	public final @Nullable BLocal local;

	public LocalRemoveEventArgument(long roleId, @Nullable BLocal local) {
		this.roleId = roleId;
		this.local = local;
	}
}
