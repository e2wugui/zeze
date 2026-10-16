package Zeze.Transaction;

import java.io.Serial;

/**
 * 可选的历史采集能力。每个 Application 在构造期装配一次，未启用时持有 null，
 * 不创建空实现。实现拥有历史表、发号与账本，核心只编排提交和刷盘生命周期。
 */
public interface IHistory extends AutoCloseable {
	/** 启动期开表前批量注册；动态开表时传入该表。允许重复调用，不注销进程共享的工厂。 */
	void registerLogs(Iterable<Table> tables) throws Exception;

	/** 数据库打开后的归属校验。失败由应用启动流程报告，已创建资源仍须可 close。 */
	void afterOpen();

	/**
	 * 仅有脏写时，在记录锁/关联集合锁及提交使用权保护下、应用数据之前调用。
	 * 成功即完成取号和登记；受控失败保证数据未应用。纯读与关闭路径不调用。
	 */
	PreparedCommit prepareCommit(Procedure procedure) throws CommitRejectedException;

	/** 提交与刷盘排空后、关库前调用；幂等，部分初始化也可清理，不触发惰性创建。 */
	@Override
	void close();

	interface PreparedCommit {
		/**
		 * 应用日志并收集 Changes 后，在同一锁域中调用一次，固定本次历史的内容。
		 * 失败属于应用数据后的硬故障，不得转换成普通拒绝或补刷业务后的成功。
		 */
		PendingHistory capture(Changes changes);
	}

	interface PendingHistory {
		/** 表及所属实例在整个缓冲/重试生命周期内保持不变。 */
		Table getTable();

		/** 关联集合锁内合并；只改接收者，只允许相同所属实例，不共享可变容器。 */
		void mergeFrom(PendingHistory from);

		/** 关联集合锁内创建独立容器，保留表及账本绑定，供本轮刷盘组合使用。 */
		PendingHistory snapshot();

		/** 可重复调用；失败不清理内容、不核销 gid。 */
		void encode0();

		/** 仅写入核心提供的表和共享事务；实现不得 commit/rollback/close 该事务。 */
		void writeOnly(Database.Table table, Database.Transaction transaction);

		/** 全部相关数据库及本地缓存事务提交成功后调用；幂等，只做本地核销和清理。 */
		void onPersisted();
	}

	/** 唯一允许在应用数据前转为干净回滚的插件失败信号，不包装应用后的异常。 */
	final class CommitRejectedException extends Exception {
		@Serial
		private static final long serialVersionUID = 0L;

		public CommitRejectedException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
