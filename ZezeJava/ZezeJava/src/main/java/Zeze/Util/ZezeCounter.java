package Zeze.Util;

import Zeze.Builtin.Provider.Send;
import Zeze.Net.FamilyClass;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ZezeCounter {
	@NotNull Logger logger = LogManager.getLogger("StatLog");

	@Nullable ZezeCounter instance = createInstance();

	boolean ENABLE = instance != null;

	private static @Nullable ZezeCounter createInstance() {
		var className = System.getProperty("ZezeCounter", "Zeze.Util.PerfCounter");
		if (className.isBlank() || className.equalsIgnoreCase("null"))
			return null;
		try {
			return (ZezeCounter)Class.forName(className).getConstructor().newInstance();
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

	interface LabeledCounterCreator {
		LongCounter labelValues(String... labels);
	}

	interface LongObserver {
		void observe(long v);
	}

	interface LabeledObserverCreator {
		LongObserver labelValues(String... labels);
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


	@NotNull LabeledCounterCreator allocLabeledCounterCreator(@NotNull String name, @NotNull String... labelNames);

	@NotNull LabeledObserverCreator allocRunTimeObserverCreator(@NotNull String name, @NotNull String... labelNames);

	/**
	 * 通过指定的key获取其绑定的累加器. 通过equals方法判断绑定的key
	 */
	@NotNull LongObserver getRunTimeObserver(@NotNull Object key);

	/**
	 * 通过指定的key累加其绑定的时间累加器(纳秒)并自增次数累加器. 通过equals方法判断绑定的key
	 */
	void addRunTime(@NotNull Object key, long timeNs);

	void procedureStart(@NotNull String name);

	/**
	 * 以{事务名,事务结果状态码}绑定的累加器自增1
	 */
	void procedureEnd(@NotNull String name, long resultCode, long timeNs);

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

	/**
	 * 根据发送的完整协议序列化数据,统计其中若干个协议的类型及其大小
	 */
	default void addSendSize(byte @NotNull [] bytes, int offset, int length) {
		while (length >= 12) {
			int moduleId = ByteBuffer.ToInt(bytes, offset);
			int protocolId = ByteBuffer.ToInt(bytes, offset + 4);
			int size = ByteBuffer.ToInt(bytes, offset + 8);
			if (size < 0) {
				logger.warn("addSendSize: moduleId={}, protocolId={}, size={} < 0", moduleId, protocolId, size);
				break;
			}
			size += Protocol.HEADER_SIZE;
			var typeId = Protocol.makeTypeId(moduleId, protocolId);
			addSendSize(typeId, size);
			if (typeId == Send.TypeId_) {
				try {
					var bb = ByteBuffer.Wrap(bytes, offset + Protocol.HEADER_SIZE, length - Protocol.HEADER_SIZE);
					var header = bb.ReadUInt();
					if ((header & FamilyClass.FamilyClassMask) != FamilyClass.Request)
						return;
					if ((header & FamilyClass.BitResultCode) != 0)
						bb.SkipLong(); // resultCode
					bb.SkipLong(); // sessionId

					int t = bb.ReadByte();
					int i = bb.ReadTagSize(t);
					if (i == 1) { // linkSids
						bb.SkipUnknownField(t);
						i += bb.ReadTagSize(t = bb.ReadByte());
					}
					if (i == 2) { // protocolType
						bb.SkipUnknownField(t);
						i += bb.ReadTagSize(t = bb.ReadByte());
					}
					if (i == 3 && (t & ByteBuffer.TAG_MASK) == ByteBuffer.BYTES) { // protocolWholeData
						int n = bb.ReadUInt();
						addSendSize(bytes, bb.ReadIndex, Math.min(n, bb.size()));
					}
				} catch (Exception e) {
					logger.warn("addSendSize: decode Send failed", e);
				}
			}
			offset += size;
			length -= size;
		}
	}
}
