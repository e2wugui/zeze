package Zeze.Arch;

import java.util.Collection;
import java.util.List;

import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * transmit 操作的统一描述：工厂收目标，setter 收选项，终结方法定时机。
 * transmit 是"路由动作到目标所在服务器"的另一操作族，独立于 OnlineSpec 的 send 体系。
 * 实例可复用、非线程安全；延迟闭包只捕获局部变量，从不捕获 spec 实例（S4）。
 */
public final class TransmitOnlineSpec {
	private final @NotNull Online online;
	private final @NotNull String senderAccount;
	private final @NotNull String senderClientId;
	private final @NotNull String actionName;
	private final @NotNull Collection<BLoginKey> targets; // 构造时 List.copyOf 快照
	private @Nullable Serializable parameter;

	TransmitOnlineSpec(@NotNull Online online, @NotNull String senderAccount, @NotNull String senderClientId,
					   @NotNull String actionName, @NotNull Collection<BLoginKey> targets) {
		this.online = online;
		this.senderAccount = senderAccount;
		this.senderClientId = senderClientId;
		this.actionName = actionName;
		this.targets = List.copyOf(targets);
	}

	public @NotNull TransmitOnlineSpec parameter(@Nullable Serializable p) {
		this.parameter = p;
		return this;
	}

	private void verify() { // 每个动词开头调用：未知 actionName 立即抛（对齐旧 transmitWhileCommit 的调用时校验）
		if (!online.getTransmitActions().containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
	}

	/** 事务感知：运行中的事务内延迟到 commit 执行，否则立即执行。 */
	public void transmit() {
		verify();
		var ol = online; // 字段全部读进局部变量：闭包不捕获 spec 实例（S4）
		var sa = senderAccount;
		var sc = senderClientId;
		var an = actionName;
		var tg = targets;
		var pm = parameter;
		Task.runTxnAware(() -> ol.transmit(sa, sc, an, tg, pm));
	}

	/** 立即执行，不等事务提交。 */
	public void transmitNow() {
		verify();
		online.transmit(senderAccount, senderClientId, actionName, targets, parameter);
	}

	/** 事务回滚时执行。 */
	public void transmitWhileRollback() {
		verify();
		var ol = online;
		var sa = senderAccount;
		var sc = senderClientId;
		var an = actionName;
		var tg = targets;
		var pm = parameter;
		Transaction.whileRollback(() -> ol.transmit(sa, sc, an, tg, pm));
	}
}
