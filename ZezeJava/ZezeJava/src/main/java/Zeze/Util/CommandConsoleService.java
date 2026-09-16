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
		if (cc == null)
			// R2-U2：连接accept时setCommandConsole尚未调用（或被显式置null）——不能整块消费静默吞输入
			// （客户端敲任何命令都无响应也无断开，不可观测）；显式抛错关闭连接并留痕，
			// 客户端重连即得已就绪的控制台。
			throw new IllegalStateException("CommandConsole not ready (setCommandConsole not called?)");
		cc.input(so, input.Bytes, input.ReadIndex, input.size());
		input.ReadIndex = input.WriteIndex; // all processed
		return true;
	}
}
