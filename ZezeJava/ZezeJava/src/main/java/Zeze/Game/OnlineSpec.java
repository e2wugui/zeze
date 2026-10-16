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

/**
 * 在线发送的统一描述：工厂收目标，setter 收选项，终结方法定时机。
 * <p>
 * 动词约定：无标记 = 事务感知（运行中的事务内延迟到 commit 发送，rollback 不发）；
 * Now 后缀 = 立即发送；WhileRollback = 事务回滚时发送。
 * <p>
 * 集合目标直传（工厂不预拷贝），快照语义统一由 OnlineTarget 各 record 的规范构造器负责。
 * <p>
 * 实例可复用、非线程安全。选项在终结方法调用时刻冻结：延迟闭包只捕获局部变量，
 * 从不捕获 spec 实例；调用后修改选项不影响已排队的发送。
 */
public class OnlineSpec {
	final Online online;
	final OnlineTarget target;
	boolean quietWhenAbsent;
	boolean withContext;

	OnlineSpec(Online online, OnlineTarget target) {
		this.online = online;
		this.target = target;
	}

	// ---------------- 工厂 ----------------

	public static RoleOnlineSpec ofRole(Online online, long roleId) {
		return new RoleOnlineSpec(online, roleId);
	}

	public static OnlineSpec ofRoles(Online online, Collection<Long> roleIds) {
		return new OnlineSpec(online, new OnlineTarget.Roles(roleIds));
	}

	/** 跨所有 OnlineSet 广播。quietWhenAbsent 预设 true（广播时目标缺席是常态）。 */
	public static OnlineSpec ofAllOnline(Online online, long roleId) {
		return new OnlineSpec(online, new OnlineTarget.AllRoles(Set.of(roleId))).quietWhenAbsent(true); // 不可变 Set，规范构造器零拷贝别名
	}

	public static OnlineSpec ofAllOnline(Online online, Collection<Long> roleIds) {
		return new OnlineSpec(online, new OnlineTarget.AllRoles(roleIds)).quietWhenAbsent(true);
	}

	public static OnlineSpec ofReliableNotify(Online online, long roleId,
													   String listenerName) {
		return new OnlineSpec(online, new OnlineTarget.Reliable(roleId, listenerName));
	}

	public static TransmitOnlineSpec ofTransmit(Online online, long sender,
														 String actionName, long roleId) {
		return new TransmitOnlineSpec(online, sender, actionName, List.of(roleId));
	}

	public static TransmitOnlineSpec ofTransmit(Online online, long sender,
														 String actionName, Iterable<Long> targets) {
		return new TransmitOnlineSpec(online, sender, actionName, targets);
	}

	// ---------------- 选项 ----------------

	/** 目标缺席（不在线/查无此登录/未登录完成）时是否静默跳过（不记缺席日志）；默认 false 记 info/debug 日志便于排障。Arch 的 reliable 目标忽略此选项。 */
	public OnlineSpec quietWhenAbsent(boolean quietWhenAbsent) {
		this.quietWhenAbsent = quietWhenAbsent;
		return this;
	}

	/** 按当前上下文选择 Online 实例（OnlineSet）；默认直接用本 Online。终结方法调用时刻解析并固定。 */
	public OnlineSpec withContext() {
		this.withContext = true;
		return this;
	}

	// ---------------- 内部 ----------------

	final Online resolveOnline() {
		return withContext ? online.getOnlineByContext() : online;
	}

	private void tryLog(long typeId, Protocol<?> p, Online resolved) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", target.describe(resolved), p);
	}

	// ---------------- 终结方法 ----------------

	/** 事务感知发送：运行中的事务内延迟到 commit 发送，否则立即发送。 */
	public void send(Protocol<?> p) {
		if (target.isEmpty())
			return; // 空目标不编码
		var o = resolveOnline(); // 此刻解析并固定（commit 回调里上下文已变，不能晚解析）
		var typeId = p.getTypeId();
		tryLog(typeId, p, o);
		send0(typeId, new Binary(p.encode()), o);
	}

	public void send(long typeId, Binary fullEncodedProtocol) {
		if (target.isEmpty())
			return;
		send0(typeId, fullEncodedProtocol, resolveOnline());
	}

	private void send0(long typeId, Binary data, Online o) {
		var tg = target; // 字段读进局部变量：延迟闭包只捕获局部变量，不捕获 spec 实例
		var quiet = quietWhenAbsent;
		Task.runTxnAware(() -> tg.send(o, typeId, data, quiet));
	}

	/**
	 * 立即发送，不等事务提交（即使在事务内；之后 rollback/redo 无法撤销）。
	 *
	 * @return 发送数（空目标 0；ofAllOnline 目标恒 -1，跨集合发送数无意义；仅供日志/统计参考）
	 */
	public int sendNow(Protocol<?> p) {
		if (target.isEmpty())
			return 0; // 空目标不编码
		var o = resolveOnline();
		var typeId = p.getTypeId();
		tryLog(typeId, p, o);
		return target.send(o, typeId, new Binary(p.encode()), quietWhenAbsent);
	}

	/** 同 {@link #sendNow(Protocol)}，载荷为已编码协议（typeId + 全编码字节）。 */
	public int sendNow(long typeId, Binary fullEncodedProtocol) {
		if (target.isEmpty())
			return 0;
		return target.send(resolveOnline(), typeId, fullEncodedProtocol, quietWhenAbsent);
	}

	/** 事务回滚时发送。 */
	public void sendWhileRollback(Protocol<?> p) {
		if (target.isEmpty())
			return;
		var o = resolveOnline();
		var typeId = p.getTypeId();
		tryLog(typeId, p, o);
		var data = new Binary(p.encode());
		var tg = target;
		var quiet = quietWhenAbsent;
		Transaction.whileRollback(() -> tg.send(o, typeId, data, quiet));
	}

	public void sendWhileRollback(long typeId, Binary fullEncodedProtocol) {
		if (target.isEmpty())
			return;
		var o = resolveOnline();
		var tg = target;
		var quiet = quietWhenAbsent;
		Transaction.whileRollback(() -> tg.send(o, typeId, fullEncodedProtocol, quiet));
	}
}
