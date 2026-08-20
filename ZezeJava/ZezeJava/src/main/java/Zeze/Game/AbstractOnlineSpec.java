package Zeze.Game;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;

abstract class AbstractOnlineSpec {
	@NotNull Online online;

	boolean trying;
	boolean withContext;

	AbstractOnlineSpec(@NotNull Online online) {
		this.online = online;
	}

	/**
	 * 检查协议是否rpc，并尝试记录协议日志。
	 * @param typeId typeId
	 * @param p protocol
	 * @param roleId role
	 */
	void tryLog(long typeId, Protocol<?> p, long roleId, String onlineSetName) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", roleId, onlineSetName, p);
	}

	/**
	 * 检查协议是否rpc，并尝试记录协议日志。
	 * @param typeId typeId
	 * @param p protocol
	 * @param roleIds roles
	 */
	void tryLog(long typeId, Protocol<?> p, Iterable<Long> roleIds, String onlineSetName) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");

		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
			var sb = new StringBuilder();
			for (var roleId : roleIds)
				sb.append(roleId).append(',');
			int n = sb.length();
			if (n > 0)
				sb.setLength(n - 1);
			if (!onlineSetName.isEmpty())
				sb.append('@').append(onlineSetName);
			var idsStr = sb.toString();
			AsyncSocket.log("Send", idsStr, p);
		}
	}

	/**
	 * 给多人发送编码好的协议。
	 * 根据人数选择不同发送函数。
	 * @param roleIds 目标角色
	 * @param typeId 协议编号
	 * @param fullEncodedProtocol 编码好的协议。
	 */
	void sendDirect(Collection<Long> roleIds, long typeId, Binary fullEncodedProtocol) {
		var size = roleIds.size();
		if (size == 1) {
			online.sendDirect(roleIds.iterator().next(), typeId, fullEncodedProtocol, trying);
		} else if (size > 1) {
			online.sendDirect(roleIds, typeId, fullEncodedProtocol, trying);
		}
	}

	void sendAll(long roleId, long typeId, Binary data) {
		online.getProviderWithOnline().foreachOnline(online -> online.sendDirect(roleId, typeId, data, trying));
	}

	void sendAll(Collection<Long> roleIds, long typeId, Binary data) {
		online.getProviderWithOnline().foreachOnline(online -> this.sendDirect(roleIds, typeId, data));
	}
}
