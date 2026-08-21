package Zeze.Arch;

import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;

public final class ReliableOnlineSpec extends AbstractOnlineSpec implements OnlineSpec {
	private final @NotNull String account;
	private final @NotNull String clientId;
	private final @NotNull String listenerName;

	ReliableOnlineSpec(@NotNull Online online, @NotNull String account, @NotNull String clientId,
					   @NotNull String listenerName) {
		super(online);
		this.account = account;
		this.clientId = clientId;
		this.listenerName = listenerName;
	}

	// note: 不提供 trying 选项。底层 sendReliableNotifyDirect 不支持 trySend。

	/**
	 * 发送协议。
	 *
	 * @param p protocol
	 */
	public void send(@NotNull Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLog(typeId, p, account + ',' + clientId + ':' + listenerName);
		send(typeId, new Binary(p.encode()));
	}

	/**
	 * 发送编码好的协议。
	 * 如果在事务中，那么会在事务提交的时候发送。
	 * 如果不在事务中，马上发送。
	 *
	 * @param typeId             typeId
	 * @param fullEncodedProtocol encoded protocol
	 */
	public void send(long typeId, @NotNull Binary fullEncodedProtocol) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning()) {
			t.runWhileCommit(() -> online.sendReliableNotifyDirect(account, clientId, listenerName, typeId,
					fullEncodedProtocol));
			return;
		}
		online.sendReliableNotifyDirect(account, clientId, listenerName, typeId, fullEncodedProtocol);
	}

	/**
	 * 当事务回滚时，发送协议。
	 *
	 * @param p protocol
	 */
	public void sendWhileRollback(@NotNull Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLog(typeId, p, account + ',' + clientId + ':' + listenerName);
		sendWhileRollback(typeId, new Binary(p.encode()));
	}

	/**
	 * 当事务回滚时，发送编码好的协议。
	 *
	 * @param typeId             typeId
	 * @param fullEncodedProtocol encoded protocol
	 */
	public void sendWhileRollback(long typeId, @NotNull Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> online.sendReliableNotifyDirect(account, clientId, listenerName, typeId,
				fullEncodedProtocol));
	}
}
