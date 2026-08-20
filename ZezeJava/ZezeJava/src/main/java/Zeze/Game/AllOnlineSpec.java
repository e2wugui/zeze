package Zeze.Game;

import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Transaction.Transaction;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

public final class AllOnlineSpec extends AbstractOnlineSpec implements OnlineSpec {
	private final long roleId;
	private final Collection<Long> roleIds;

	AllOnlineSpec(@NotNull Online online, long roleId) {
		super(online);
		this.roleId = roleId;
		this.roleIds = null;
		super.trying = true; // 修改默认值。
	}

	AllOnlineSpec(@NotNull Online online, Collection<Long> roleIds) {
		super(online);
		this.roleId = 0;
		this.roleIds = roleIds;
		super.trying = true; // 修改默认值。
	}

	/**
	 * 本次send是否只是尝试，即允许失败。
	 * 这个参数影响是否记录错误日志。
	 * @param trySend 是否尝试发送
	 * @return this
	 */
	public @NotNull AllOnlineSpec trying(boolean trySend) {
		this.trying = trySend;
		return this;
	}

	/**
	 * 发送协议。
	 * @param p protocol
	 */
	public void send(Protocol<?> p) {
		var typeId = p.getTypeId();
		if (null == roleIds)
			tryLog(typeId, p, roleId, "*");
		else
			tryLog(typeId, p, roleIds, "*");
		var fullEncodedProtocol = new Binary(p.encode());
		this.send(typeId, fullEncodedProtocol);
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
			t.runWhileCommit(() -> this.sendAll(typeId, fullEncodedProtocol));
			return;
		}
		this.sendAll(typeId, fullEncodedProtocol);
	}

	/**
	 * 根据目标角色的设置选择不同的实现。
	 * @param typeId 协议编号
	 * @param fullEncodedProtocol 编码好的协议。
	 */
	private void sendAll(long typeId, Binary fullEncodedProtocol) {
		if (null == roleIds)
			super.sendAll(roleId, typeId, fullEncodedProtocol);
		else
			super.sendAll(roleIds, typeId, fullEncodedProtocol);
	}

	/**
	 * 当事务回滚时，发送协议。
	 * @param p protocol
	 */
	public void sendWhileRollback(Protocol<?> p) {
		var typeId = p.getTypeId();
		if (null == roleIds)
			tryLog(typeId, p, roleId, "*");
		else
			tryLog(typeId, p, roleIds, "*");
		var fullEncodedProtocol = new Binary(p.encode());
		sendWhileRollback(typeId, fullEncodedProtocol);
	}

	/**
	 * 当事务回滚时，发送编码好的协议。
	 * @param typeId typeId
	 * @param fullEncodedProtocol encoded protocol
	 */
	public void sendWhileRollback(long typeId, Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> this.sendAll(typeId, fullEncodedProtocol));
	}

}
