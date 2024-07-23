package Zeze.Util;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandConsoleService extends Service {
	private @Nullable CommandConsole cc;

	public void setCommandConsole(@Nullable CommandConsole cc) {
		this.cc = cc;
	}

	public CommandConsoleService(@NotNull String name, @Nullable Config config) {
		super(name, config);
	}

	@Override
	public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
		var c = cc;
		if (c != null)
			so.setUserState(CommandConsole.dup(c));
		super.OnSocketAccept(so);
	}

	@Override
	public boolean OnSocketProcessInputBuffer(@NotNull AsyncSocket so, @NotNull ByteBuffer input) {
		var cc = (CommandConsole)so.getUserState();
		if (cc != null)
			cc.input(so, input.Bytes, input.ReadIndex, input.size());

		input.ReadIndex = input.WriteIndex; // all processed
		return true;
	}
}
