package Zeze.Util;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import org.jspecify.annotations.Nullable;

/** 承载 CommandConsole 的 TCP 服务：接收行协议输入并回显命令结果。 */
public class CommandConsoleService extends Service {
	private volatile @Nullable CommandConsole cc;

	public void setCommandConsole(@Nullable CommandConsole cc) {
		this.cc = cc;
	}

	public CommandConsoleService(String name, @Nullable Config config) {
		super(name, config);
	}

	@Override
	public void OnSocketAccept(AsyncSocket so) throws Exception {
		var c = cc;
		if (c != null)
			so.setUserState(CommandConsole.dup(c));
		super.OnSocketAccept(so);
	}

	@Override
	public boolean OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input) {
		var cc = (CommandConsole)so.getUserState();
		if (cc == null)
			throw new IllegalStateException("CommandConsole not ready (setCommandConsole not called?)");
		cc.input(so, input.Bytes, input.ReadIndex, input.size());
		input.ReadIndex = input.WriteIndex;
		return true;
	}
}
