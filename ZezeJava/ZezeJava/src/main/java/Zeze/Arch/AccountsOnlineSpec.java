package Zeze.Arch;

import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class AccountsOnlineSpec extends AbstractOnlineSpec implements OnlineSpec {
	private final @NotNull Collection<String> accounts;

	AccountsOnlineSpec(@NotNull Online online, @NotNull Collection<String> accounts) {
		super(online);
		this.accounts = accounts;
	}

	/**
	 * 本次send是否只是尝试，即允许失败。
	 * 这个参数影响是否记录错误日志。
	 *
	 * @param trySend 是否尝试发送
	 * @return this
	 */
	public @NotNull AccountsOnlineSpec trying(boolean trySend) {
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
		tryLogAccounts(typeId, p, accounts);
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
			t.runWhileCommit(() -> sendDirect(typeId, fullEncodedProtocol));
			return;
		}
		sendDirect(typeId, fullEncodedProtocol);
	}

	/**
	 * 给多个账号所有的登录终端发送编码好的协议。
	 * 根据账号个数选择不同发送函数。
	 *
	 * @param typeId             协议编号
	 * @param fullEncodedProtocol 编码好的协议。
	 */
	private void sendDirect(long typeId, @NotNull Binary fullEncodedProtocol) {
		var size = accounts.size();
		if (size == 1) {
			var it = accounts.iterator();
			if (it.hasNext()) // 不确定accounts是否稳定,所以还是判断一下保险
				online.sendAccountDirect(it.next(), typeId, fullEncodedProtocol, trying);
		} else if (size > 1) {
			online.sendAccountsDirect(accounts instanceof Set ? accounts : new HashSet<>(accounts),
					typeId, fullEncodedProtocol, trying);
		}
	}

	/**
	 * 当事务回滚时，发送协议。
	 *
	 * @param p protocol
	 */
	public void sendWhileRollback(@NotNull Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLogAccounts(typeId, p, accounts);
		sendWhileRollback(typeId, new Binary(p.encode()));
	}

	/**
	 * 当事务回滚时，发送编码好的协议。
	 *
	 * @param typeId             typeId
	 * @param fullEncodedProtocol encoded protocol
	 */
	public void sendWhileRollback(long typeId, @NotNull Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> sendDirect(typeId, fullEncodedProtocol));
	}
}
