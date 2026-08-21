package Zeze.Game;

import Zeze.Serialize.Serializable;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TransmitOnlineSpec extends AbstractOnlineSpec implements OnlineSpec {
	private final long sender;
	private @NotNull final String actionName;
	private @NotNull final Iterable<Long> targets;
	private @Nullable Serializable parameter;
	private boolean processNotOnline = true;

	TransmitOnlineSpec(@NotNull Online online, long sender, @NotNull String actionName, @NotNull Iterable<Long> targets) {
		super(online);

		this.sender = sender;
		this.actionName = actionName;
		this.targets = targets;
	}

	/**
	 * 由于OnlineSet，现在允许多个Online集合。
	 * 这个参数决定是否根据当前上下文选择Online实例。
	 * 默认情况下直接通过本Online发送。
	 * @return this
	 */
	public @NotNull TransmitOnlineSpec withContext() {
		this.withContext = true;
		online = online.getOnlineByContext();
		return this;
	}

	/**
	 * 设置transmit的参数。
	 * @param p parameter
	 * @return this
	 */
	public @NotNull TransmitOnlineSpec parameter(Serializable p) {
		this.parameter = p;
		return this;
	}

	/**
	 * 是否处理不在线
	 * @param b 是否处理不在线
	 * @return this
	 */
	public @NotNull TransmitOnlineSpec processNotOnline(boolean b) {
		this.processNotOnline = b;
		return this;
	}

	private void verify() {
		if (!online.getTransmitActions().containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
	}

	/**
	 * 发送transmit。
	 * 如果在事务中，则在提交时执行。
	 * 不在事务中，立即执行。
	 */
	public void transmit() {
		verify();
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning()) {
			t.runWhileCommit(() -> online.transmit(sender, actionName, targets, parameter, processNotOnline));
			return;
		}
		online.transmit(sender, actionName, targets, parameter, processNotOnline);
	}

	/**
	 * 事务回滚时，发送transmit。
	 */
	public void transmitWhileRollback() {
		verify();
		Transaction.whileRollback(() -> online.transmit(sender, actionName, targets, parameter, processNotOnline));
	}
}
