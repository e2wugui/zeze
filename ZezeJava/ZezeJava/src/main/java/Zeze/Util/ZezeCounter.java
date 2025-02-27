package Zeze.Util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ZezeCounter {
	@NotNull Logger logger = LogManager.getLogger("StatLog");

	boolean ENABLE_PERF = !Boolean.parseBoolean(System.getProperty("disableZezeCounter"));

	@NotNull ZezeCounter instance = createInstance();

	private static @NotNull ZezeCounter createInstance() {
		try {
			var counterClass = Class.forName(System.getProperty("ZezeCounter", "Zeze.Util.PerfCounter"));
			return (ZezeCounter)counterClass.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}

	interface LongCounter {
		LongCounter dummy = v -> {
		};

		default void increment() {
			add(1L);
		}

		void add(long v);
	}

	interface TableCounter {
		@NotNull LongCounter readLock();

		@NotNull LongCounter writeLock();

		@NotNull LongCounter storageGet();

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

	default void init() {
	}

	// 通过name申请一个累加器的整数索引
	int allocCounterIndex(@NotNull String name);

	// 使用整数索引增加该累加器自增1
	void incCounterByIndex(int index);

	// 使用整数索引增加该累加器自增count
	void addCounterByIndex(int index, long count);

	// 通过指定的key累加其绑定的时间累加器(纳秒)并自增次数累加器. 通过equals方法判断绑定的key
	void addRunTime(@NotNull Object key, long timeNs);

	// 通过指定的key获取其绑定的累加器. 通过equals方法判断绑定的key
	@NotNull LongCounter getRunTimeCounter(@NotNull Object key);

	// 根据协议类(可选)及其类型ID,增加其绑定的大小(字节)累加器和处理时间(纳秒)累加器
	void addRecvSizeTime(long typeId, @Nullable Class<?> cls, int size, long timeNs);

	// 根据发送的完整协议序列化数据,增加其绑定的大小(字节)累加器
	void addSendSize(byte @NotNull [] bytes, int offset, int length);

	// {事务名,事务结果状态码}绑定的累加器自增1
	void countProcedureResultCode(@NotNull String name, long resultCode);

	// 根据表ID获取其绑定的表统计器
	@NotNull TableCounter getOrAddTableInfo(long tableId);
}
