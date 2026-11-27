package Zeze.Component;

/**
 * Takeover 租约接管作用域：各参与死者数据搬运的组件（如 CsQueue、Timer）实现本接口。
 * 存活期通过 {@link #stamp} 写入当前 epoch；owner 租约过期后，接管者在事务内重验租约并调用
 * 幂等的 {@link #transferAll} 搬运数据、清空死者链头与 loadSerialNo，提交后回调 {@link #afterTransfer}。
 */
public interface TakeoverScope {
	String name();

	/**
	 * 将当前租约 epoch 写入本进程 root 行的 loadSerialNo（必须在事务内调用）。
	 *
	 * @param epoch 当前进程的租约 epoch
	 */
	void stamp(long epoch);

	/**
	 * 在事务内幂等搬运 deadServerId 名下的数据到本进程名下，并清空死者 root 链头与 loadSerialNo。
	 *
	 * @param deadServerId 死者 serverId
	 * @param deadEpoch    死者租约 epoch（用于诊断与日志）
	 * @return 搬运数量（&gt;0 表示发生了搬运）；-1 表示 veto（不立租约墓碑，留待其他处理者）
	 */
	long transferAll(int deadServerId, long deadEpoch);

	/**
	 * tryTransfer事务成功提交后的回调（事务外），如Timer需要重新loadTimer调度。
	 * 仅在本次tryTransfer实际搬运了数据时调用。
	 */
	default void afterTransfer(int deadServerId) {
	}
}
