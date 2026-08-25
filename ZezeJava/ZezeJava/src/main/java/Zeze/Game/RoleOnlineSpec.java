package Zeze.Game;

import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 单角色发送器：在 OnlineSpec 之上追加仅对单端点成立的能力（rpc/response/link 直发）。 */
public final class RoleOnlineSpec extends OnlineSpec {
	private final long roleId;
	private int timeout = 5000; // 仅 sendRpc 族消费，send 族忽略

	RoleOnlineSpec(@NotNull Online online, long roleId) {
		super(online, new OnlineTarget.Role(roleId));
		this.roleId = roleId;
	}

	@Override
	public @NotNull RoleOnlineSpec trying(boolean trying) { // 协变返回，保持链式
		super.trying(trying);
		return this;
	}

	@Override
	public @NotNull RoleOnlineSpec withContext() {
		super.withContext();
		return this;
	}

	/** rpc 超时（毫秒），默认 5000；仅 sendRpc/sendRpcForWait 消费，send 族忽略。 */
	public @NotNull RoleOnlineSpec timeout(int timeoutMs) {
		this.timeout = timeoutMs;
		return this;
	}

	/** 发送 rpc 响应（事务感知）。 */
	public void sendResponse(@NotNull Rpc<?, ?> r) {
		r.setRequest(false);
		send(r); // 走 send 族：非 request 放行守卫
	}

	/**
	 * 异步发送 rpc（事务感知）。
	 * 注意：rpc 本体在执行时刻才被接线与编码，sendRpc 后不要再修改 rpc.Argument。
	 */
	public <A extends Serializable, R extends Serializable>
	void sendRpc(@NotNull Rpc<A, R> rpc, @Nullable ProtocolHandle<Rpc<A, R>> responseHandle) {
		var o = resolveOnline();
		var rid = roleId; // 以下字段全部读进局部变量：闭包不捕获 spec 实例（S4）
		var to = timeout;
		var tr = trying;
		Task.runTxnAware(() -> o.sendOnlineRpc(rid, rpc, responseHandle, to, tr));
	}

	/**
	 * 同步发送 rpc，返回可等待的 future（立即发送）。
	 * 运行中的事务内调用抛 IllegalStateException：发送若延迟到 commit 而等待阻塞 commit 将死锁；
	 * 请在事务外调用，或改用 sendRpc + responseHandle。
	 */
	public <A extends Serializable, R extends Serializable>
	@NotNull TaskCompletionSource<R> sendRpcForWait(@NotNull Rpc<A, R> rpc) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			throw new IllegalStateException("sendRpcForWait 不能用于运行中的事务内："
					+ "发送会延迟到 commit，而等待会阻塞 commit（死锁）。请在事务外调用，或改用 sendRpc + responseHandle。");
		var future = new TaskCompletionSource<R>();
		rpc.setFuture(future);
		if (!resolveOnline().sendOnlineRpc(roleId, rpc, null, timeout, trying))
			future.setException(new IllegalStateException("sendOnlineRpc fail.")); // 对齐旧 sendOnlineRpcForWait
		return future;
	}

	/** 直接通过 link 发送（事务感知）；忽略 trying/timeout 选项。 */
	public void send(@NotNull String linkName, long linkSid, @NotNull Protocol<?> p) {
		var o = resolveOnline();
		var rid = roleId; // 读进局部变量：闭包不捕获 spec 实例（S4）
		Task.runTxnAware(() -> o.send(rid, linkName, linkSid, p));
	}

	/** 事务回滚时直接通过 link 发送；选项规则同上。 */
	public void sendWhileRollback(@NotNull String linkName, long linkSid, @NotNull Protocol<?> p) {
		var o = resolveOnline();
		var rid = roleId;
		Transaction.whileRollback(() -> o.send(rid, linkName, linkSid, p));
	}
}
