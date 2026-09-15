package Zeze.Util;

import Zeze.Net.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ZezeCounter {
	@NotNull Logger logger = LogManager.getLogger("StatLog");

	/** 禁用统计（-DZezeCounter为空或"null"）时落入的空实现，所有方法无操作。 */
	@NotNull ZezeCounter instance = createInstance();

	/** 真实实现存在（instance不是NoopCounter）时为true，用于省略热路径上的nanoTime等采样开销。 */
	boolean ENABLE = !(instance instanceof NoopCounter);

	private static @NotNull ZezeCounter createInstance() {
		var className = System.getProperty("ZezeCounter", "Zeze.Util.PerfCounter");
		if (className.isBlank() || className.equalsIgnoreCase("null"))
			return NoopCounter.instance;
		try {
			return (ZezeCounter)Class.forName(className).getConstructor((Class<?>[])null).newInstance((Object[])null);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}

	static void tryInit() {
		if (instance != null)
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
		@NotNull LongCounter labelValues(@NotNull String... labels);
	}

	interface LabeledObserverCreator {
		@NotNull LongObserver labelValues(@NotNull String... labels);
	}

	interface TableCounter {
		@NotNull LongCounter cacheGet();

		@NotNull LongCounter storageGet();

		@NotNull LongCounter readLock();

		@NotNull LongCounter writeLock();

		// 这两个统计用来观察cache清理的影响
		@NotNull LongCounter tryReadLock();

		@NotNull LongCounter tryWriteLock();

		// global acquire 的次数，即时没有开启cache-sync，也会有一点点计数，因为没人抢，所以以后总是成功了。
		@NotNull LongCounter acquireShare();

		@NotNull LongCounter acquireModify();

		@NotNull LongCounter acquireInvalid();

		@NotNull LongCounter reduceInvalid();

		@NotNull LongCounter redo();
	}

	/**
	 * 初始化. 调用下面方法必须先调用过这个方法一次
	 */
	default void init() {
	}

	/**
	 * 通过name分配一个累加器
	 * 注意: 不判断name是否重复出现,总是分配新的,通常用于初始化全局的累加器,数量不应过多
	 */
	@NotNull LongCounter allocCounter(@NotNull String name);

	/**
	 * @return 有标签的Counter指标
	 */
	@NotNull LabeledCounterCreator allocLabeledCounterCreator(@NotNull String name, @NotNull String... labelNames);

	/**
	 * @return 有标签的数据分布指标
	 */
	@NotNull LabeledObserverCreator allocRunTimeObserverCreator(@NotNull String name, @NotNull String... labelNames);

	/**
	 * 通过指定的key获取其绑定的累加器. 通过equals方法判断绑定的key
	 */
	@NotNull LongObserver getRunTimeObserver(@NotNull Object key);

	/**
	 * 通过指定的key累加其绑定的时间累加器(纳秒)并自增次数累加器. 通过equals方法判断绑定的key
	 */
	void addTaskRunTime(@NotNull Object key, long timeNs);

	/**
	 * 服务开启
	 */
	void serviceStart(@NotNull Service service);

	/**
	 * 服务停止
	 */
	void serviceStop(@NotNull Service service);

	/**
	 * 事务开始
	 */
	void procedureStart(@NotNull String name);

	/**
	 * 事务完成
	 */
	void procedureEnd(@NotNull String name, long resultCode, long timeNs);

	/**
	 * 事务redo
	 */
	void procedureRedo(@NotNull String name);

	/**
	 * 事务redoAndReleaseLock
	 */
	void procedureRedoAndReleaseLock(@NotNull String name);

	/**
	 * 根据表ID获取其绑定的表统计器
	 */
	@NotNull TableCounter getOrAddTableInfo(long tableId);

	/**
	 * 根据协议类(可选)及其类型ID,增加其绑定的大小(字节)累加器和处理时间(纳秒)累加器
	 */
	void addRecvSizeTime(long typeId, @Nullable Class<?> cls, int size, long timeNs);

	/**
	 * 根据协议类型ID统计其协议大小
	 */
	void addSendSize(long typeId, int size);
}
