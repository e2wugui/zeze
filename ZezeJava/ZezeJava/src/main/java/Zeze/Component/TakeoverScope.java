package Zeze.Component;

/**
 * Takeover 租约接管的作用域。每个需要参与“死者数据搬运”的组件实现一个scope：
 * CsQueue（每个命名队列一个）、Timer（进程一个）。
 * <p>
 * 生命周期约定：
 * <ul>
 * <li>owner存活期间通过 {@link #stamp} 把自己名下root行的loadSerialNo写成当前epoch
 *     （addScope晚注册时由 {@link Takeover} 在独立小事务内调用）；</li>
 * <li>owner死亡后，接管者对每个scope在独立zeze事务内（事务内重验租约过期）调用 {@link #transferAll}：
 *     执行时租约必已过期（复活者被重验拦截），链上有数据（头指针非0）即属于死者、应搬运——
 *     不以 {@code root.loadSerialNo == deadEpoch} 作前置：claim后stamp前的崩溃窗口会留下旧stamp
 *     数据，按epoch折叠会被误判为"已被搬走"，租约照常立碑后积压永久搁浅（FND2-C0-1）；</li>
 * <li>搬运成功后由 {@link #transferAll} 自己清空死者root链指针（幂等重入出口）并把loadSerialNo
 *     清0（墓碑，供复活者认领）。</li>
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
	 * @param deadEpoch    死者租约的epoch（诊断/日志用；搬运不再以root stamp对账epoch为前提）
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
