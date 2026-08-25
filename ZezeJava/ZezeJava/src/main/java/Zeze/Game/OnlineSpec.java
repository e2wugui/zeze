package Zeze.Game;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

/**
 * 在线发送的统一描述：工厂收目标，setter 收选项，终结方法定时机。
 *
 * 动词约定：无标记 = 事务感知（运行中的事务内延迟到 commit 发送，rollback 不发）；
 * Now 后缀 = 立即发送；WhileRollback = 事务回滚时发送。
 *
 * 集合目标直传（工厂不预拷贝），快照语义统一由 OnlineTarget 各 record 的规范构造器负责。
 *
 * 实例可复用、非线程安全。选项在终结方法调用时刻冻结：延迟闭包只捕获局部变量，
 * 从不捕获 spec 实例；调用后修改选项不影响已排队的发送（见 S4）。
 */
public class OnlineSpec {
	final @NotNull Online online;
	final @NotNull OnlineTarget target;
	boolean trying;
	boolean withContext;

	OnlineSpec(@NotNull Online online, @NotNull OnlineTarget target) {
		this.online = online;
		this.target = target;
	}

	// ---------------- 工厂 ----------------

	public static @NotNull RoleOnlineSpec ofRole(@NotNull Online online, long roleId) {
		return new RoleOnlineSpec(online, roleId);
	}

	public static @NotNull OnlineSpec ofRoles(@NotNull Online online, @NotNull Collection<Long> roleIds) {
		return new OnlineSpec(online, new OnlineTarget.Roles(roleIds));
	}

	/** 跨所有 OnlineSet 广播。trying 预设 true。 */
	public static @NotNull OnlineSpec ofAllOnline(@NotNull Online online, long roleId) {
		return new OnlineSpec(online, new OnlineTarget.AllRoles(Set.of(roleId))).trying(true); // 不可变 Set，规范构造器零拷贝别名
	}

	public static @NotNull OnlineSpec ofAllOnline(@NotNull Online online, @NotNull Collection<Long> roleIds) {
		return new OnlineSpec(online, new OnlineTarget.AllRoles(roleIds)).trying(true);
	}

	public static @NotNull OnlineSpec ofReliableNotify(@NotNull Online online, long roleId,
													   @NotNull String listenerName) {
		return new OnlineSpec(online, new OnlineTarget.Reliable(roleId, listenerName));
	}

	public static @NotNull TransmitOnlineSpec ofTransmit(@NotNull Online online, long sender,
														 @NotNull String actionName, long roleId) {
		return new TransmitOnlineSpec(online, sender, actionName, List.of(roleId));
	}

	public static @NotNull TransmitOnlineSpec ofTransmit(@NotNull Online online, long sender,
														 @NotNull String actionName, @NotNull Iterable<Long> targets) {
		return new TransmitOnlineSpec(online, sender, actionName, targets);
	}

	// ---------------- 选项 ----------------

	/** 本次发送是否只是尝试（允许失败），影响错误日志。Arch 的 reliable 目标忽略此选项。 */
	public @NotNull OnlineSpec trying(boolean trying) {
		this.trying = trying;
		return this;
	}

	/** 按当前上下文选择 Online 实例（OnlineSet）；默认直接用本 Online。终结方法调用时刻解析并固定。 */
	public @NotNull OnlineSpec withContext() {
		this.withContext = true;
		return this;
	}

	// ---------------- 内部 ----------------

	final @NotNull Online resolveOnline() {
		return withContext ? online.getOnlineByContext() : online;
	}

	private void tryLog(long typeId, @NotNull Protocol<?> p, @NotNull Online resolved) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", target.describe(resolved), p);
	}

	// ---------------- 终结方法 ----------------

	/** 事务感知发送：运行中的事务内延迟到 commit 发送，否则立即发送。 */
	public void send(@NotNull Protocol<?> p) {
		if (target.isEmpty())
			return; // 空目标不编码（对齐旧行为）
		var o = resolveOnline(); // 此刻解析并固定（commit 回调里上下文已变，不能晚解析）
		var typeId = p.getTypeId();
		tryLog(typeId, p, o);
		send0(typeId, new Binary(p.encode()), o);
	}

	public void send(long typeId, @NotNull Binary fullEncodedProtocol) {
		if (target.isEmpty())
			return;
		send0(typeId, fullEncodedProtocol, resolveOnline());
	}

	private void send0(long typeId, @NotNull Binary data, @NotNull Online o) {
		var tg = target; // 字段读进局部变量：延迟闭包只捕获局部变量，不捕获 spec 实例（S4）
		var tr = trying;
		Task.runTxnAware(() -> tg.send(o, typeId, data, tr));
	}

	/** 立即发送，不等事务提交（即使在事务内；之后 rollback/redo 无法撤销）。 */
	public void sendNow(@NotNull Protocol<?> p) {
		if (target.isEmpty())
			return;
		var o = resolveOnline();
		var typeId = p.getTypeId();
		tryLog(typeId, p, o);
		target.send(o, typeId, new Binary(p.encode()), trying);
	}

	public void sendNow(long typeId, @NotNull Binary fullEncodedProtocol) {
		if (target.isEmpty())
			return;
		target.send(resolveOnline(), typeId, fullEncodedProtocol, trying);
	}

	/** 事务回滚时发送。 */
	public void sendWhileRollback(@NotNull Protocol<?> p) {
		if (target.isEmpty())
			return;
		var o = resolveOnline();
		var typeId = p.getTypeId();
		tryLog(typeId, p, o);
		var data = new Binary(p.encode());
		var tg = target;
		var tr = trying;
		Transaction.whileRollback(() -> tg.send(o, typeId, data, tr));
	}

	public void sendWhileRollback(long typeId, @NotNull Binary fullEncodedProtocol) {
		if (target.isEmpty())
			return;
		var o = resolveOnline();
		var tg = target;
		var tr = trying;
		Transaction.whileRollback(() -> tg.send(o, typeId, fullEncodedProtocol, tr));
	}
}
