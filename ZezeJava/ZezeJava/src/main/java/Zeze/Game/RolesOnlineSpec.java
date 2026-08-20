package Zeze.Game;

import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import java.util.Collection;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;

public final class RolesOnlineSpec extends AbstractOnlineSpec implements OnlineSpec {
	private final Collection<Long> roleIds;

	RolesOnlineSpec(@NotNull Online online, Collection<Long> roleIds) {
		super(online);
		this.roleIds = roleIds;
	}

	/**
	 * 本次send是否只是尝试，即允许失败。
	 * 这个参数影响是否记录错误日志。
	 * @param trySend 是否尝试发送
	 * @return this
	 */
	public @NotNull RolesOnlineSpec trying(boolean trySend) {
		this.trying = trySend;
		return this;
	}

	/**
	 * 由于OnlineSet，现在允许多个Online集合。
	 * 这个参数决定是否根据当前上下文选择Online实例。
	 * 默认情况下直接通过本Online发送。
	 * @return this
	 */
	public @NotNull RolesOnlineSpec withContext() {
		this.withContext = true;
		online = online.getOnlineByContext();
		return this;
	}

	/**
	 * 发送协议。
	 * @param p protocol
	 */
	public void send(Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLog(typeId, p, roleIds, online.getOnlineSetName());
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
			t.runWhileCommit(() -> super.sendDirect(roleIds, typeId, fullEncodedProtocol));
		} else {
			// 事务外。
			super.sendDirect(roleIds, typeId, fullEncodedProtocol);
		}
	}

	/**
	 * 当事务回滚时，发送协议。
	 * @param p protocol
	 */
	public void sendWhileRollback(Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLog(typeId, p, roleIds, online.getOnlineSetName());
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
		Transaction.whileRollback(() -> super.sendDirect(roleIds, typeId, fullEncodedProtocol));
	}

}
