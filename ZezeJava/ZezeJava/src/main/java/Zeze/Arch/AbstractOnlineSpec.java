package Zeze.Arch;

import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

abstract class AbstractOnlineSpec {
	@NotNull Online online;

	boolean trying;

	AbstractOnlineSpec(@NotNull Online online) {
		this.online = online;
	}

	/**
	 * 检查协议是否rpc请求，并尝试记录协议日志。
	 *
	 * @param typeId typeId
	 * @param p      protocol
	 * @param id     日志标识，一般是 "account,clientId"。
	 */
	void tryLog(long typeId, @NotNull Protocol<?> p, @NotNull String id) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendResponse");
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", id, p);
	}

	/**
	 * 检查协议是否rpc请求，并尝试记录协议日志。
	 *
	 * @param typeId typeId
	 * @param p      protocol
	 * @param logins 目标登录
	 */
	void tryLogLogins(long typeId, @NotNull Protocol<?> p, @NotNull Collection<BLoginKey> logins) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendResponse");

		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
			var sb = new StringBuilder();
			for (var login : logins)
				sb.append(login.getAccount()).append(',').append(login.getClientId()).append(';');
			int n = sb.length();
			if (n > 0)
				sb.setLength(n - 1);
			AsyncSocket.log("Send", sb.toString(), p);
		}
	}

	/**
	 * 检查协议是否rpc请求，并尝试记录协议日志。
	 *
	 * @param typeId   typeId
	 * @param p        protocol
	 * @param accounts 目标账号
	 */
	void tryLogAccounts(long typeId, @NotNull Protocol<?> p, @NotNull Collection<String> accounts) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendResponse");

		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
			var sb = new StringBuilder();
			for (var account : accounts)
				sb.append(account).append(',');
			int n = sb.length();
			if (n > 0)
				sb.setLength(n - 1);
			AsyncSocket.log("Send", sb.toString(), p);
		}
	}
}
