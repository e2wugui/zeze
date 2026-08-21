package Zeze.Arch;

import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

public final class LoginsOnlineSpec extends AbstractOnlineSpec implements OnlineSpec {
	private final @NotNull Collection<BLoginKey> logins;

	LoginsOnlineSpec(@NotNull Online online, @NotNull Collection<BLoginKey> logins) {
		super(online);
		this.logins = logins;
	}

	/**
	 * 本次send是否只是尝试，即允许失败。
	 * 这个参数影响是否记录错误日志。
	 *
	 * @param trySend 是否尝试发送
	 * @return this
	 */
	public @NotNull LoginsOnlineSpec trying(boolean trySend) {
		this.trying = trySend;
		return this;
	}

	/**
	 * 发送协议。
	 *
	 * @param p protocol
	 */
	public void send(@NotNull Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLogLogins(typeId, p, logins);
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
	 * 给多个登录发送编码好的协议。
	 * 根据登录个数选择不同发送函数。
	 *
	 * @param typeId             协议编号
	 * @param fullEncodedProtocol 编码好的协议。
	 */
	private void sendDirect(long typeId, @NotNull Binary fullEncodedProtocol) {
		var size = logins.size();
		if (size == 1) {
			var it = logins.iterator();
			if (it.hasNext()) { // 不确定logins是否稳定,所以还是判断一下保险
				var login = it.next();
				online.sendDirect(login.getAccount(), login.getClientId(), typeId, fullEncodedProtocol, trying);
			}
		} else if (size > 1) {
			online.sendDirect(logins instanceof HashSet<BLoginKey> set ? set : new HashSet<>(logins),
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
		tryLogLogins(typeId, p, logins);
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
