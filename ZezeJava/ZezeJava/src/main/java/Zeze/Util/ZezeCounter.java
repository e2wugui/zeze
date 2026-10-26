package Zeze.Util;

import java.util.Map;

import Zeze.Net.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

// 统计接口：protocol/procedure/table/task 等维度的计数与耗时观察者，instance 按系统属性选择实现
// 本接口不声明default方法，避免实现类初始化时反向触发全局实例创建。
public interface ZezeCounter {
	Logger logger = LogManager.getLogger("StatLog");

	/** 禁用统计（-DZezeCounter为空或"null"）时落入的空实现，所有方法无操作。 */
	ZezeCounter instance = createInstance();

	/** 真实实现存在（instance不是NoopCounter）时为true，用于省略热路径上的nanoTime等采样开销。 */
	boolean ENABLE = !(instance instanceof NoopCounter);

	private static ZezeCounter createInstance() {
		var className = System.getProperty("ZezeCounter", "Zeze.Util.PerfCounter");
		if (className.isBlank() || className.equalsIgnoreCase("null"))
			return new NoopCounter();
		try {
			return (ZezeCounter)Class.forName(className).getConstructor((Class<?>[])null).newInstance((Object[])null);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}

	static void tryInit() {
		instance.init();
	}

	interface LongCounter {
		default void increment() {
			inc(1L);
		}

		void inc(long v);
	}

	interface LongObserver {
		void observe(long v);
	}

	interface LabeledCounterCreator {
		LongCounter labelValues(String... labels);
	}

	interface LabeledObserverCreator {
		LongObserver labelValues(String... labels);
	}

	/** 表级度量。key为快照与Prometheus label使用的驼峰名。 */
	enum TableMetric {
		CACHE_GET("cacheGet"),
		STORAGE_GET("storageGet"),
		READ_LOCK("readLock"),
		WRITE_LOCK("writeLock"),
		// 这两个统计用来观察cache清理的影响
		TRY_READ_LOCK("tryReadLock"),
		TRY_WRITE_LOCK("tryWriteLock"),
		// global acquire 的次数，即时没有开启cache-sync，也会有一点点计数，因为没人抢，所以以后总是成功了。
		ACQUIRE_SHARE("acquireShare"),
		ACQUIRE_MODIFY("acquireModify"),
		ACQUIRE_INVALID("acquireInvalid"),
		REDUCE_INVALID("reduceInvalid"),
		REDO("redo");

		final String key;

		TableMetric(String key) {
			this.key = key;
		}
	}

	/** 初始化；调用统计功能前执行一次。 */
	void init();

	/**
	 * 通过name分配一个累加器
	 * 注意: name重复时的行为由实现决定——Prometheus实现重名register抛IllegalArgumentException，
	 * 其余实现（Noop/Perf）总是分配新的,通常用于初始化全局的累加器,数量不应过多
	 */
	LongCounter allocCounter(String name);

	/**
	 * @return 有标签的Counter指标
	 */
	LabeledCounterCreator allocLabeledCounterCreator(String name, String... labelNames);

	/**
	 * @return 有标签的数据分布指标
	 */
	LabeledObserverCreator allocRunTimeObserverCreator(String name, String... labelNames);

	/**
	 * 通过指定的key获取其绑定的累加器. 通过equals方法判断绑定的key
	 */
	LongObserver getRunTimeObserver(Object key);

	/**
	 * 服务开启
	 */
	void serviceStart(Service service);

	/**
	 * 服务停止
	 */
	void serviceStop(Service service);

	/**
	 * 事务（存储过程）度量handle：通过 {@link #allocProcedureCounter(String)} 按name解析一次，
	 * 调用点缓存复用（如Procedure实例字段），热路径零名字解析。
	 */
	interface ProcedureCounter {
		/** 事务开始（每次执行调用） */
		void start();

		/** 事务完成（每次执行调用） */
		void end(long resultCode, long timeNs);

		/** 事务redo */
		void redo();

		/** 事务redo且释放锁 */
		void redoAndReleaseLock();

		/** 事务持有锁数量观察（达到ProcedureLockWatcherMin阈值时调用） */
		void manyLocks(int count);
	}

	/**
	 * 通过name分配事务度量handle。同name多次分配安全（各实现内部按name聚合统计）。
	 */
	ProcedureCounter allocProcedureCounter(String name);

	/**
	 * 通过表ID与度量获取其绑定的表统计器
	 */
	LongCounter tableCounter(long tableId, TableMetric metric);

	/**
	 * 一个统计周期的结果；不支持周期快照的实现返回空快照。
	 * 直接持有传入的Map，不复制或包装。按调用约定只读：生产方在构造后不得再修改
	 * 传入的外层和内层Map，读取方也不得修改返回的任何一层Map。
	 * 后续采集应新建结果Map，避免影响已经发布的快照。
	 *
	 * @param procedureResults procedureName -&gt; resultCode -&gt; 最近周期计数（含零计数条目）
	 * @param tableResults     tableName -&gt; 度量名 -&gt; 最近周期计数
	 * @param formattedLog     最近一次格式化日志
	 */
	record Snapshot(Map<String, Map<Long, Long>> procedureResults,
					Map<String, Map<String, Long>> tableResults,
					String formattedLog) {
		public static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of(), "");
	}

	/**
	 * 收集当前周期的统计，重置周期计数，保存并返回生成的快照。
	 * 不支持周期快照的实现返回空快照。
	 * 返回的Map遵循{@link Snapshot}的只读约定。
	 */
	Snapshot snapshotAndReset();

	/**
	 * 返回最近一次收集生成的快照，不进行收集或重置。
	 * 尚未收集过或不支持快照时返回空快照。
	 * 返回的Map遵循{@link Snapshot}的只读约定。
	 */
	Snapshot getLastSnapshot();

	/**
	 * 根据协议类(可选)及其类型ID,增加其绑定的大小(字节)累加器和处理时间(纳秒)累加器
	 */
	void addRecvSizeTime(long typeId, @Nullable Class<?> cls, int size, long timeNs);

	/** 协议从接收解析到开始处理的等待时间，单位为纳秒。 */
	void addRecvDispatchTime(long typeId, long timeNs);

	/**
	 * 根据协议类型ID统计其协议大小
	 */
	void addSendSize(long typeId, int size);
}
