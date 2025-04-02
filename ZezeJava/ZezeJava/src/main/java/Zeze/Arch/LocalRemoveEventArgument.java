package Zeze.Arch;

import Zeze.Builtin.Online.BLocal;
import Zeze.Util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

public class LocalRemoveEventArgument implements EventDispatcher.EventArgument {
	public final @NotNull String account;
	public final @NotNull String clientId;
	public final @NotNull BLocal local;

	public LocalRemoveEventArgument(@NotNull String account, @NotNull String clientId, @NotNull BLocal localData) {
		this.account = account;
		this.clientId = clientId;
		this.local = localData;
	}
}
