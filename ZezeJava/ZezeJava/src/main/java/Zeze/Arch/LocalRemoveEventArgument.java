package Zeze.Arch;

import Zeze.Builtin.Online.BLocal;
import Zeze.Util.EventDispatcher;

/**
 * 本地在线移除事件的参数：账号、clientId 与移除时的本地在线数据。
 */
public class LocalRemoveEventArgument implements EventDispatcher.EventArgument {
	public final String account;
	public final String clientId;
	public final BLocal local;

	public LocalRemoveEventArgument(String account, String clientId, BLocal localData) {
		this.account = account;
		this.clientId = clientId;
		this.local = localData;
	}
}
