package Zeze.Arch;

import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;

public final class AccountOnlineSpec extends AbstractOnlineSpec implements OnlineSpec {
	private final @NotNull String account;

	AccountOnlineSpec(@NotNull Online online, @NotNull String account) {
		super(online);
		this.account = account;
	}

	/**
	 * 本次send是否只是尝试，即允许失败。
	 * 这个参数影响是否记录错误日志。
	 *
	 * @param trySend 是否尝试发送
	 * @return this
	 */
	public @NotNull AccountOnlineSpec trying(boolean trySend) {
		this.trying = trySend;
		return this;
	}

	/**
	 * 发送协议，给账号所有的登录终端发送。
	 *
	 * @param p protocol
	 */
	public void send(@NotNull Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLog(typeId, p, account);
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
			t.runWhileCommit(() -> online.sendAccountDirect(account, typeId, fullEncodedProtocol, trying));
			return;
		}
		online.sendAccountDirect(account, typeId, fullEncodedProtocol, trying);
	}

	/**
	 * 当事务回滚时，发送协议。
	 *
	 * @param p protocol
	 */
	public void sendWhileRollback(@NotNull Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLog(typeId, p, account);
		sendWhileRollback(typeId, new Binary(p.encode()));
	}

	/**
	 * 当事务回滚时，发送编码好的协议。
	 *
	 * @param typeId             typeId
	 * @param fullEncodedProtocol encoded protocol
	 */
	public void sendWhileRollback(long typeId, @NotNull Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> online.sendAccountDirect(account, typeId, fullEncodedProtocol, trying));
	}
}
