package Zeze.Net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Util.FastLock;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Selectors extends ReentrantLock {
	public static final class InstanceHolder { // for lazy-init
		private static final Selectors instance = new Selectors("Selector");
	}

	public static @NotNull Selectors getInstance() {
		return InstanceHolder.instance;
	}

	@SuppressWarnings("CanBeFinal")
	public static final class Config {
		public int bbPoolBlockSize = 64 * 1024; // 单个buffer的字节容量,推荐等于Socket.getSendBufferSize()
		public int bbPoolLocalCapacity = 1024; // 本地池的最大保留buffer数量
		public int bbPoolMoveCount = 1024; // 本地池和全局池之间移动一次的buffer数量
		public int bbPoolGlobalCapacity = 64 * bbPoolMoveCount; // 全局池的最大buffer数量
		public int selectTimeout; // 0表示无超时,>0表示每次select的超时毫秒数,-1表示无超时且异步wakeup
		public int readBufferSize = 64 * 1024; // 读buffer的字节容量

		public void check() {
			if (bbPoolBlockSize <= 0)
				throw new IllegalArgumentException("bbPoolBlockSize <= 0: " + bbPoolBlockSize);
			if (bbPoolLocalCapacity < 0)
				throw new IllegalArgumentException("bbPoolLocalCapacity < 0: " + bbPoolLocalCapacity);
			if (bbPoolMoveCount <= 0)
				throw new IllegalArgumentException("bbPoolMoveCount <= 0: " + bbPoolMoveCount);
			if (bbPoolGlobalCapacity < 0)
				throw new IllegalArgumentException("bbPoolGlobalCapacity < 0: " + bbPoolGlobalCapacity);
			if (selectTimeout < -1)
				throw new IllegalArgumentException("selectTimeout < -1: " + selectTimeout);
			if (readBufferSize <= 0)
				throw new IllegalArgumentException("readBufferSize <= 0: " + readBufferSize);
		}
	}

	private final @NotNull String name;
	private final int bbPoolBlockSize;
	private final int bbPoolLocalCapacity;
	private final int bbPoolMoveCount;
	private final int bbPoolGlobalCapacity;
	private final int selectTimeout;
	private final int readBufferSize;
	private final ArrayList<ByteBuffer> bbGlobalPool = new ArrayList<>(); // 全局池,需要考虑并发访问
	private final FastLock bbGlobalPoolLock = new FastLock(); // 全局池的锁
	private volatile @NotNull Selector[] selectorList;
	private volatile boolean closed;
	private final AtomicLong choiceCount = new AtomicLong();

	public Selectors(@NotNull String name) {
		this(name, 1, null);
	}

	public Selectors(@NotNull String name, int count) {
		this(name, count, null);
	}

	public Selectors(@NotNull String name, int count, @Nullable Config config) {
		this.name = name;
		if (config == null)
			config = new Config();
		config.check();
		bbPoolBlockSize = config.bbPoolBlockSize;
		bbPoolLocalCapacity = config.bbPoolLocalCapacity;
		bbPoolMoveCount = config.bbPoolMoveCount;
		bbPoolGlobalCapacity = config.bbPoolGlobalCapacity;
		selectTimeout = config.selectTimeout;
		readBufferSize = config.readBufferSize;
		add(count);
	}

	public int getBbPoolBlockSize() {
		return bbPoolBlockSize;
	}

	public int getBbPoolLocalCapacity() {
		return bbPoolLocalCapacity;
	}

	public int getBbPoolMoveCount() {
		return bbPoolMoveCount;
	}

	public int getBbPoolGlobalCapacity() {
		return bbPoolGlobalCapacity;
	}

	public int getSelectTimeout() {
		return selectTimeout;
	}

	public int getReadBufferSize() {
		return readBufferSize;
	}

	@NotNull ArrayList<ByteBuffer> getBbGlobalPool() {
		return bbGlobalPool;
	}

	@NotNull FastLock getBbGlobalPoolLock() {
		return bbGlobalPoolLock;
	}

	public int getCount() {
		return selectorList.length;
	}

	public long getSelectCount() {
		long count = 0;
		for (var selector : selectorList)
			count += selector.getSelectCount();
		return count;
	}

	public @NotNull Selectors add(int count) {
		if (closed) // close后不允许重建线程（choice()对closed同样抛IllegalStateException）
			throw new IllegalStateException("closed");
		// 持锁（FND4-37）：原"读selectorList→copyOf→start→赋值"无锁check-then-act，并发add
		// 后写覆盖先写——先注册的Selector线程从数组丢失但仍在运行（daemon泄漏、choice()轮不到）。
		// 类型自身即ReentrantLock，数组变更与读取互斥由类型保证。
		lock();
		try {
			Selector[] tmp = selectorList;
			int i, n;
			if (tmp == null) {
				i = 0;
				n = Math.max(count, 1);
				tmp = new Selector[n];
			} else {
				i = tmp.length;
				n = Math.max(Math.addExact(i, count), 1);
				if (i != n)
					tmp = Arrays.copyOf(tmp, n);
			}
			var batchStart = i;
			try {
				for (; i < n; i++) {
					tmp[i] = new Selector(this, name + '-' + i);
					tmp[i].start();
				}
			} catch (IOException e) {
				// N2-F2：本批已创建/已启动的Selector不在selectorList（数组赋值在循环成功之后），
				// choice()/close()均不可及——线程永久泄漏且重试叠加。关闭本批再重抛；
				// 已有的旧Selector不受影响（selectorList未变）。
				for (int k = batchStart; k < n; k++) {
					var s = tmp[k];
					if (s != null)
						s.close();
				}
				throw Task.forceThrow(e);
			}
			selectorList = tmp;
			return this;
		} finally {
			unlock();
		}
	}

	public @NotNull Selector choice() {
		Selector[] tmp = selectorList; // thread safe
		if (tmp == null)
			throw new IllegalStateException("closed");

		long count = choiceCount.getAndIncrement();
		int index = (int)Long.remainderUnsigned(count, tmp.length);
		return tmp[index];
	}

	public void close() {
		lock();
		try {
			closed = true;
			Selector[] tmp = selectorList;
			if (tmp != null) {
				selectorList = null;
				for (Selector s : tmp)
					s.close();
			}
		} finally {
			unlock();
		}
	}
}
