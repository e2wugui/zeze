package Zeze.Arch;

import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class TransmitOnlineSpec extends AbstractOnlineSpec implements OnlineSpec {
	private final @NotNull String senderAccount;
	private final @NotNull String senderClientId;
	private final @NotNull String actionName;
	private final @NotNull Collection<BLoginKey> targets;
	private @Nullable Serializable parameter;

	TransmitOnlineSpec(@NotNull Online online, @NotNull String senderAccount, @NotNull String senderClientId,
					   @NotNull String actionName, @NotNull Collection<BLoginKey> targets) {
		super(online);
		this.senderAccount = senderAccount;
		this.senderClientId = senderClientId;
		this.actionName = actionName;
		this.targets = targets;
	}

	/**
	 * 设置transmit的参数。
	 *
	 * @param p parameter
	 * @return this
	 */
	public @NotNull TransmitOnlineSpec parameter(@Nullable Serializable p) {
		this.parameter = p;
		return this;
	}

	/**
	 * 发送transmit。
	 * 如果在事务中，则在提交时执行。
	 * 不在事务中，立即执行。
	 */
	public void transmit() {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning()) {
			t.runWhileCommit(() -> online.transmit(senderAccount, senderClientId, actionName, targets, parameter));
			return;
		}
		online.transmit(senderAccount, senderClientId, actionName, targets, parameter);
	}

	/**
	 * 事务回滚时，发送transmit。
	 */
	public void transmitWhileRollback() {
		Transaction.whileRollback(() -> online.transmit(senderAccount, senderClientId, actionName, targets, parameter));
	}
}
