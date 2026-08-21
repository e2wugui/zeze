package Zeze.Arch;

import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;

public final class LoginOnlineSpec extends AbstractOnlineSpec implements OnlineSpec {
	private final @NotNull String account;
	private final @NotNull String clientId;

	LoginOnlineSpec(@NotNull Online online, @NotNull String account, @NotNull String clientId) {
		super(online);
		this.account = account;
		this.clientId = clientId;
	}

	/**
	 * 本次send是否只是尝试，即允许失败。
	 * 这个参数影响是否记录错误日志。
	 *
	 * @param trySend 是否尝试发送
	 * @return this
	 */
	public @NotNull LoginOnlineSpec trying(boolean trySend) {
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
		tryLog(typeId, p, account + ',' + clientId);
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
			t.runWhileCommit(() -> online.sendDirect(account, clientId, typeId, fullEncodedProtocol, trying));
			return;
		}
		online.sendDirect(account, clientId, typeId, fullEncodedProtocol, trying);
	}

	/**
	 * 当事务回滚时，发送协议。
	 *
	 * @param p protocol
	 */
	public void sendWhileRollback(@NotNull Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLog(typeId, p, account + ',' + clientId);
		sendWhileRollback(typeId, new Binary(p.encode()));
	}

	/**
	 * 当事务回滚时，发送编码好的协议。
	 *
	 * @param typeId             typeId
	 * @param fullEncodedProtocol encoded protocol
	 */
	public void sendWhileRollback(long typeId, @NotNull Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> online.sendDirect(account, clientId, typeId, fullEncodedProtocol, trying));
	}

	/**
	 * 发送rpc的响应。
	 *
	 * @param r rpc response
	 */
	public void sendResponse(@NotNull Rpc<?, ?> r) {
		r.setRequest(false);
		send(r);
	}

	/**
	 * 直接通过link发送协议。
	 *
	 * @param linkName linkName
	 * @param linkSid  linkSid
	 * @param p        protocol
	 */
	public void send(@NotNull String linkName, long linkSid, @NotNull Protocol<?> p) {
		var loginKey = new BLoginKey(account, clientId);
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning()) {
			t.runWhileCommit(() -> online.send(loginKey, linkName, linkSid, p));
			return;
		}
		online.send(loginKey, linkName, linkSid, p);
	}

	/**
	 * 事务回滚时，直接通过link发送协议。
	 *
	 * @param linkName linkName
	 * @param linkSid  linkSid
	 * @param p        protocol
	 */
	public void sendWhileRollback(@NotNull String linkName, long linkSid, @NotNull Protocol<?> p) {
		Transaction.whileRollback(() -> online.send(new BLoginKey(account, clientId), linkName, linkSid, p));
	}
}
