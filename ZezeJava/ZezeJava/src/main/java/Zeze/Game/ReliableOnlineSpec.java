package Zeze.Game;

import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;

public final class ReliableOnlineSpec extends AbstractOnlineSpec implements OnlineSpec {
	private final long roleId;
	private @NotNull final String listenerName;

	ReliableOnlineSpec(@NotNull Online online, long roleId, @NotNull String listenerName) {
		super(online);
		this.roleId = roleId;
		this.listenerName = listenerName;
	}

	/**
	 * 发送协议。
	 * @param p protocol
	 */
	public void send(Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLog(typeId, p, roleId, listenerName + ":" + online.getOnlineSetName());
		var fullEncodedProtocol = new Binary(p.encode());
		send(typeId, fullEncodedProtocol);
	}

	/**
	 * 发送编码好的协议。
	 * 如果在事务中，那么会在事务提交的时候发送。
	 * 如果不在事务中，马上发送。
	 * @param typeId typeId
	 * @param fullEncodedProtocol encoded protocol
	 */
	public void send(long typeId, Binary fullEncodedProtocol) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning()) {
			t.runWhileCommit(() -> online.sendReliableNotifyDirect(roleId, listenerName, typeId, fullEncodedProtocol, trying));
			return;
		}
		online.sendReliableNotifyDirect(roleId, listenerName, typeId, fullEncodedProtocol, trying);
	}

	/**
	 * 当事务回滚时，发送协议。
	 * @param p protocol
	 */
	public void sendWhileRollback(Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLog(typeId, p, roleId, listenerName + ":" + online.getOnlineSetName());
		var fullEncodedProtocol = new Binary(p.encode());
		sendWhileRollback(typeId, fullEncodedProtocol);
	}

	/**
	 * 当事务回滚时，发送编码好的协议。
	 * 如果在事务中，那么会在事务提交的时候发送。
	 * 如果不在事务中，马上发送。
	 * @param typeId typeId
	 * @param fullEncodedProtocol encoded protocol
	 */
	public void sendWhileRollback(long typeId, Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> online.sendReliableNotifyDirect(roleId, listenerName, typeId, fullEncodedProtocol, trying));
	}

}
