package Zeze.Game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import Zeze.Serialize.Serializable;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * transmit 操作的统一描述：工厂收目标，setter 收选项，终结方法定时机。
 * transmit 是"路由动作到目标所在服务器"的另一操作族，独立于 OnlineSpec 的 send 体系。
 * 实例可复用、非线程安全；延迟闭包只捕获局部变量，从不捕获 spec 实例。
 */
public final class TransmitOnlineSpec {
	private final @NotNull Online online;
	private final long sender;
	private final @NotNull String actionName;
	private final @NotNull List<Long> targets; // 构造时快照
	private @Nullable Serializable parameter;
	private boolean processNotOnline = true; // 对齐旧默认值
	private boolean withContext;

	TransmitOnlineSpec(@NotNull Online online, long sender, @NotNull String actionName,
					   @NotNull Iterable<Long> targets) {
		this.online = online;
		this.sender = sender;
		this.actionName = actionName;
		if (targets instanceof Collection<Long> collection)
			this.targets = List.copyOf(collection);
		else {
			var list = new ArrayList<Long>();
			for (var target : targets)
				list.add(target);
			this.targets = List.copyOf(list);
		}
	}

	public @NotNull TransmitOnlineSpec parameter(@Nullable Serializable p) {
		this.parameter = p;
		return this;
	}

	/** 是否处理不在线，默认 true。 */
	public @NotNull TransmitOnlineSpec processNotOnline(boolean processNotOnline) {
		this.processNotOnline = processNotOnline;
		return this;
	}

	/** 按当前上下文选择 Online 实例（OnlineSet）；默认直接用本 Online。终结方法调用时刻解析并固定。 */
	public @NotNull TransmitOnlineSpec withContext() {
		this.withContext = true;
		return this;
	}

	private void verify() { // 每个动词开头调用：未知 actionName 立即抛（对齐旧 transmitWhileCommit 的调用时校验）
		if (!online.getTransmitActions().containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
	}

	private @NotNull Online resolveOnline() {
		return withContext ? online.getOnlineByContext() : online;
	}

	/** 事务感知：运行中的事务内延迟到 commit 执行，否则立即执行。 */
	public void transmit() {
		verify();
		var o = resolveOnline(); // 此刻解析并固定（commit 回调里上下文已变，不能晚解析）
		var sd = sender; // 字段全部读进局部变量：闭包不捕获 spec 实例
		var an = actionName;
		var tg = targets;
		var pm = parameter;
		var pno = processNotOnline;
		Task.runTxnAware(() -> o.transmit(sd, an, tg, pm, pno));
	}

	/** 立即执行，不等事务提交。 */
	public void transmitNow() {
		verify();
		resolveOnline().transmit(sender, actionName, targets, parameter, processNotOnline);
	}

	/** 事务回滚时执行。 */
	public void transmitWhileRollback() {
		verify();
		var o = resolveOnline();
		var sd = sender;
		var an = actionName;
		var tg = targets;
		var pm = parameter;
		var pno = processNotOnline;
		Transaction.whileRollback(() -> o.transmit(sd, an, tg, pm, pno));
	}
}
