package Zeze.Arch;

import java.util.Collection;
import java.util.List;

import Zeze.Builtin.ProviderDirect.BLoginKey;
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
 * 从不捕获 spec 实例；调用后修改选项不影响已排队的发送。
 */
public class OnlineSpec {
	final @NotNull Online online;
	final @NotNull OnlineTarget target;
	boolean trying;

	OnlineSpec(@NotNull Online online, @NotNull OnlineTarget target) {
		this.online = online;
		this.target = target;
	}

	// ---------------- 工厂 ----------------

	public static @NotNull LoginOnlineSpec ofLogin(@NotNull Online online,
												   @NotNull String account, @NotNull String clientId) {
		return new LoginOnlineSpec(online, account, clientId);
	}

	public static @NotNull OnlineSpec ofLogins(@NotNull Online online, @NotNull Collection<BLoginKey> logins) {
		return new OnlineSpec(online, new OnlineTarget.Logins(logins));
	}

	public static @NotNull OnlineSpec ofAccount(@NotNull Online online, @NotNull String account) {
		return new OnlineSpec(online, new OnlineTarget.Account(account));
	}

	public static @NotNull OnlineSpec ofAccounts(@NotNull Online online, @NotNull Collection<String> accounts) {
		return new OnlineSpec(online, new OnlineTarget.Accounts(accounts));
	}

	public static @NotNull OnlineSpec ofReliableNotify(@NotNull Online online,
													   @NotNull String account, @NotNull String clientId,
													   @NotNull String listenerName) {
		return new OnlineSpec(online, new OnlineTarget.Reliable(account, clientId, listenerName));
	}

	public static @NotNull TransmitOnlineSpec ofTransmit(@NotNull Online online,
														 @NotNull String senderAccount, @NotNull String senderClientId,
														 @NotNull String actionName,
														 @NotNull String targetAccount, @NotNull String targetClientId) {
		return new TransmitOnlineSpec(online, senderAccount, senderClientId, actionName,
				List.of(new BLoginKey(targetAccount, targetClientId)));
	}

	public static @NotNull TransmitOnlineSpec ofTransmit(@NotNull Online online,
														 @NotNull String senderAccount, @NotNull String senderClientId,
														 @NotNull String actionName,
														 @NotNull Collection<BLoginKey> targets) {
		return new TransmitOnlineSpec(online, senderAccount, senderClientId, actionName, targets);
	}

	// ---------------- 选项 ----------------

	/** 本次发送是否只是尝试（允许失败），影响错误日志。reliable 目标忽略此选项（底层 sendReliableNotifyDirect 不支持 trySend）。 */
	public @NotNull OnlineSpec trying(boolean trying) {
		this.trying = trying;
		return this;
	}

	// ---------------- 内部 ----------------

	private void tryLog(long typeId, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendResponse");
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", target.describe(), p);
	}

	// ---------------- 终结方法 ----------------

	/** 事务感知发送：运行中的事务内延迟到 commit 发送，否则立即发送。 */
	public void send(@NotNull Protocol<?> p) {
		if (target.isEmpty())
			return; // 空目标不编码
		var typeId = p.getTypeId();
		tryLog(typeId, p);
		send0(typeId, new Binary(p.encode()));
	}

	public void send(long typeId, @NotNull Binary fullEncodedProtocol) {
		if (target.isEmpty())
			return;
		send0(typeId, fullEncodedProtocol);
	}

	private void send0(long typeId, @NotNull Binary data) {
		var ol = online; // 字段读进局部变量：延迟闭包只捕获局部变量，不捕获 spec 实例
		var tg = target;
		var tr = trying;
		Task.runTxnAware(() -> tg.send(ol, typeId, data, tr));
	}

	/** 立即发送，不等事务提交（即使在事务内；之后 rollback/redo 无法撤销）。 */
	public void sendNow(@NotNull Protocol<?> p) {
		if (target.isEmpty())
			return;
		var typeId = p.getTypeId();
		tryLog(typeId, p);
		target.send(online, typeId, new Binary(p.encode()), trying);
	}

	public void sendNow(long typeId, @NotNull Binary fullEncodedProtocol) {
		if (target.isEmpty())
			return;
		target.send(online, typeId, fullEncodedProtocol, trying);
	}

	/** 事务回滚时发送。 */
	public void sendWhileRollback(@NotNull Protocol<?> p) {
		if (target.isEmpty())
			return;
		var typeId = p.getTypeId();
		tryLog(typeId, p);
		var data = new Binary(p.encode());
		var ol = online;
		var tg = target;
		var tr = trying;
		Transaction.whileRollback(() -> tg.send(ol, typeId, data, tr));
	}

	public void sendWhileRollback(long typeId, @NotNull Binary fullEncodedProtocol) {
		if (target.isEmpty())
			return;
		var ol = online;
		var tg = target;
		var tr = trying;
		Transaction.whileRollback(() -> tg.send(ol, typeId, fullEncodedProtocol, tr));
	}
}
