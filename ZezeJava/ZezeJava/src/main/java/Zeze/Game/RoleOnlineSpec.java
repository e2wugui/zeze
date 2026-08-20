package Zeze.Game;

import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Transaction;
import Zeze.Util.TaskCompletionSource;
import org.jetbrains.annotations.NotNull;

public final class RoleOnlineSpec extends AbstractOnlineSpec implements OnlineSpec {
	private final long roleId;
	private int timeout = 5000;

	RoleOnlineSpec(@NotNull Online online, long roleId) {
		super(online);
		this.roleId = roleId;
	}

	/**
	 * 本次send是否只是尝试，即允许失败。
	 * 这个参数影响是否记录错误日志。
	 * @param trySend 是否尝试发送
	 * @return this
	 */
	public @NotNull RoleOnlineSpec trying(boolean trySend) {
		this.trying = trySend;
		return this;
	}

	/**
	 * 由于OnlineSet，现在允许多个Online集合。
	 * 这个参数决定是否根据当前上下文选择Online实例。
	 * 默认情况下直接通过本Online发送。
	 * @return this
	 */
	public @NotNull RoleOnlineSpec withContext() {
		this.withContext = true;
		online = online.getOnlineByContext();
		return this;
	}

	/**
	 * 设置rpc的timeout参数。仅对rpc有效。
	 *
	 * @param timeout timeout default=5000
	 * @return this
	 */
	public @NotNull RoleOnlineSpec timeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * 发送协议。
	 * @param p protocol
	 */
	public void send(Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLog(typeId, p, roleId, online.getOnlineSetName());
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
			t.runWhileCommit(() -> online.sendDirect(roleId, typeId, fullEncodedProtocol, trying));
			return;
		}
		online.sendDirect(roleId, typeId, fullEncodedProtocol, trying);
	}

	/**
	 * 当事务回滚时，发送协议。
	 * @param p protocol
	 */
	public void sendWhileRollback(Protocol<?> p) {
		var typeId = p.getTypeId();
		tryLog(typeId, p, roleId, online.getOnlineSetName());
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
		Transaction.whileRollback(() -> online.sendDirect(roleId, typeId, fullEncodedProtocol, trying));
	}

	public <A extends Serializable, R extends Serializable>
	void sendRpc(@NotNull Rpc<A, R> rpc, ProtocolHandle<Rpc<A, R>> responseHandle) {

		var t = Transaction.getCurrent();
		if (t != null && t.isRunning()) {
			t.runWhileCommit(() -> online.sendOnlineRpc(roleId, rpc, responseHandle, timeout, trying));
			return;
		}
		online.sendOnlineRpc(roleId, rpc, responseHandle, timeout, trying);
	}

	public <A extends Serializable, R extends Serializable> TaskCompletionSource<R> sendRpcForWait(@NotNull Rpc<A, R> rpc) {
		var future = new TaskCompletionSource<R>();
		rpc.setFuture(future);
		sendRpc(rpc, null);
		return future;
	}
}
