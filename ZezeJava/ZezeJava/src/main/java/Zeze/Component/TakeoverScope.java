package Zeze.Component;

/**
 * Takeover 租约接管的作用域。每个需要参与“死者数据搬运”的组件实现一个scope：
 * CsQueue（每个命名队列一个）、Timer（进程一个）。
 * <p>
 * 生命周期约定：
 * <ul>
 * <li>owner存活期间通过 {@link #stamp} 把自己名下root行的loadSerialNo写成当前epoch
 *     （addScope晚注册时由 {@link Takeover} 在独立小事务内调用）；</li>
 * <li>owner死亡后，接管者在同一zeze事务内调用 {@link #transferAll}，
 *     scope以 {@code root.loadSerialNo == deadEpoch} 作为“数据仍属于死者且未被搬运”的守卫；</li>
 * <li>搬运成功后由 {@link #transferAll} 自己把死者root行的loadSerialNo清0（墓碑），幂等。</li>
 * </ul>
 */
public interface TakeoverScope {
	String name();

	/**
	 * 把epoch写进自己名下root行的loadSerialNo（epoch fence）。必须在事务内调用。
	 *
	 * @param epoch 当前进程的租约epoch（Takeover.claim得到的myEpoch）
	 */
	void stamp(long epoch);

	/**
	 * 把deadServerId名下的数据搬运到自己名下。事务内、必须幂等。
	 *
	 * @param deadServerId 死者serverId
	 * @param deadEpoch    死者租约的epoch（与其生前stamp在root行上的值对账）
	 * @return 搬运数量（可为近似值，&gt;0表示发生了搬运）；-1=veto（不立租约墓碑，留给高版本/其他处理者）
	 */
	long transferAll(int deadServerId, long deadEpoch);

	/**
	 * tryTransfer事务成功提交后的回调（事务外），如Timer需要重新loadTimer调度。
	 * 仅在本次tryTransfer实际搬运了数据时调用。
	 */
	default void afterTransfer(int deadServerId) {
	}
}
