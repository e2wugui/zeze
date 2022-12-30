package Zeze.Net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Util.Action0;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Selector extends Thread implements ByteBufferAllocator {
	public static final int DEFAULT_BUFFER_SIZE = 32 * 1024; // 单个buffer的字节容量
	public static final int DEFAULT_BBPOOL_LOCAL_CAPACITY = 1000; // 本地池的最大保留buffer数量
	public static final int DEFAULT_BBPOOL_MOVE_COUNT = 1000; // 本地池和全局池之间移动一次的buffer数量
	public static final int DEFAULT_BBPOOL_GLOBAL_CAPACITY = 100 * DEFAULT_BBPOOL_MOVE_COUNT; // 全局池的最大buffer数量
	public static final int DEFAULT_SELECT_TIMEOUT = 0; // 0表示无超时,>0表示每次select的超时毫秒数
	private static final Logger logger = LogManager.getLogger(Selector.class);
	private static final ArrayList<ByteBuffer> bbGlobalPool = new ArrayList<>(); // 全局池
	private static final Lock bbGlobalPoolLock = new ReentrantLock(); // 全局池的锁
	private static int bbPoolGlobalCapacity = DEFAULT_BBPOOL_GLOBAL_CAPACITY;

	private final java.nio.channels.Selector selector;
	private final ByteBuffer readBuffer = ByteBuffer.allocate(32 * 1024); // 此线程共享的buffer,只能临时使用
	private final AtomicInteger wakeupNotified = new AtomicInteger();
	private final ArrayList<ByteBuffer> bbPool = new ArrayList<>();
	private final int bufferSize;
	private final int bbPoolLocalCapacity;
	private final int bbPoolMoveCount;
	private final int selectTimeout;
	private ArrayList<Action0> operates = new ArrayList<>(); // 用于跟AsyncSocket交换
	private boolean firstAction;
	private volatile boolean running = true;

//	public final AtomicLong wakeupCount0 = new AtomicLong();
//	public final AtomicLong wakeupCount1 = new AtomicLong();
//	public final AtomicLong wakeupTime = new AtomicLong();
//	public long lastTime;

	public Selector(String threadName) throws IOException {
		this(threadName, DEFAULT_BUFFER_SIZE, DEFAULT_BBPOOL_LOCAL_CAPACITY, DEFAULT_BBPOOL_MOVE_COUNT,
				DEFAULT_SELECT_TIMEOUT);
	}

	public Selector(String threadName, int bufferSize, int bbPoolLocalCapacity, int bbPoolMoveCount)
			throws IOException {
		this(threadName, bufferSize, bbPoolLocalCapacity, bbPoolMoveCount, DEFAULT_SELECT_TIMEOUT);
	}

	public Selector(String threadName, int bufferSize, int bbPoolLocalCapacity, int bbPoolMoveCount, int selectTimeout)
			throws IOException {
		super(threadName);
		if (bufferSize <= 0)
			throw new IllegalArgumentException("bufferSize <= 0: " + bufferSize);
		if (bbPoolLocalCapacity < 0)
			throw new IllegalArgumentException("bbPoolLocalCapacity < 0: " + bbPoolLocalCapacity);
		if (bbPoolMoveCount <= 0)
			throw new IllegalArgumentException("bbPoolMoveCount <= 0: " + bbPoolMoveCount);
		if (selectTimeout < 0)
			throw new IllegalArgumentException("selectTimeout < 0: " + selectTimeout);
		this.bufferSize = bufferSize;
		this.bbPoolLocalCapacity = bbPoolLocalCapacity;
		this.bbPoolMoveCount = bbPoolMoveCount;
		this.selectTimeout = selectTimeout;
		setDaemon(true);
		selector = java.nio.channels.Selector.open();
	}

	public static int getBbPoolGlobalCapacity() {
		return bbPoolGlobalCapacity;
	}

	public static void setBbPoolGlobalCapacity(int bbPoolGlobalCapacity) {
		if (bbPoolGlobalCapacity < 0)
			throw new IllegalArgumentException("bbPoolGlobalCapacity < 0: " + bbPoolGlobalCapacity);
		Selector.bbPoolGlobalCapacity = bbPoolGlobalCapacity;
	}

	ByteBuffer getReadBuffer() {
		return readBuffer;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public int getBbPoolLocalCapacity() {
		return bbPoolLocalCapacity;
	}

	public int getBbPoolMoveCount() {
		return bbPoolMoveCount;
	}

	@Override
	public ByteBuffer alloc() {
		int n = bbPool.size();
		if (n <= 0) {
			bbGlobalPoolLock.lock();
			try {
				var gn = bbGlobalPool.size();
				if (gn >= bbPoolMoveCount) {
					var bbMoves = bbGlobalPool.subList(gn - bbPoolMoveCount, gn);
					bbPool.addAll(bbMoves);
					bbMoves.clear();
				}
			} finally {
				bbGlobalPoolLock.unlock();
			}
			n = bbPool.size();
		}
		return n > 0 ? bbPool.remove(n - 1) : ByteBuffer.allocateDirect(bufferSize);
	}

	@Override
	public void free(ByteBuffer bb) {
		int n = bbPool.size();
		if (n >= bbPoolLocalCapacity + bbPoolMoveCount) { // 可以释放一批
			var bbMoves = bbPool.subList(n - bbPoolMoveCount, n);
			bbGlobalPoolLock.lock();
			try {
				if (bbGlobalPool.size() >= bbPoolGlobalCapacity) // 全局池也放不下就丢弃这个bb
					return;
				bbGlobalPool.addAll(bbMoves);
			} finally {
				bbGlobalPoolLock.unlock();
			}
			bbMoves.clear();
		}
		bb.position(0);
		bb.limit(bb.capacity());
		bbPool.add(bb);
	}

	ArrayList<Action0> swapOperates(ArrayList<Action0> operates) {
		var t = this.operates;
		this.operates = operates;
		return t;
	}

	SelectionKey register(SelectableChannel sc, int ops, SelectorHandle handle) {
		try {
			SelectionKey key = sc.register(selector, ops, handle);
			// 当引擎线程执行register时，wakeup会导致一次多余唤醒。
			// 这在连接建立不是很繁忙的应用中问题不大。
			// 下面通过判断是否本线程来决定是否调用wakeup。
			if (Thread.currentThread() != this)
				selector.wakeup(); // 不会丢失。
			return key;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		running = false;
		selector.wakeup();

		// join
		while (true) {
			try {
				join();
				break;
			} catch (Throwable ex) {
				logger.error("{} close skip.", getClass().getName(), ex);
			}
		}

		try {
			selector.close();
		} catch (Throwable e) {
			logger.error("{} selector.close skip.", getClass().getName(), e);
		}
	}

	public void wakeup() {
		if (selectTimeout == 0 && Thread.currentThread() != this && wakeupNotified.compareAndSet(0, 1)) {
//			wakeupCount1.incrementAndGet();
//			long t = System.nanoTime();
			selector.wakeup();
//			wakeupTime.addAndGet(System.nanoTime() - t);
		}// else
//			wakeupCount0.incrementAndGet();
	}

	@Override
	public void run() {
//		lastTime = System.nanoTime();
		while (running) {
//			var t = System.nanoTime();
//			if (t - lastTime >= 1_000_000_000L) {
//				long time = t - lastTime;
//				lastTime = t;
//				long count0 = wakeupCount0.getAndSet(0);
//				long count1 = wakeupCount1.getAndSet(0);
//				long wTime = wakeupTime.getAndSet(0);
//				logger.info("wakeup: {}, {}, {} ns, {} ms", count0, count1, count1 > 0 ? wTime / count1 : -1,
//						time / 1_000_000);
//			}
			try {
				// 如果在这个时间窗口 wakeup，下面的 select 会马上返回。wakeup 不会丢失。
				if (selectTimeout == 0) {
					firstAction = true;
					wakeupNotified.set(0);
				}
				selector.select(key -> {
					if (firstAction) {
						firstAction = false;
						wakeupNotified.set(1);
					}
					if (!key.isValid())
						return; // key maybe cancel in loop
					SelectorHandle handle = null;
					try {
						handle = (SelectorHandle)key.attachment();
						handle.doHandle(key);
					} catch (Throwable e) {
						if (handle != null) {
							try {
								handle.doException(key, e);
							} catch (Throwable e3) {
								logger.error("Selector.run", e);
								logger.error("SelectorHandle.doException: {}", e, e3);
							}
						} else
							logger.error("Selector.run", e);
						try {
							key.channel().close();
						} catch (Throwable e2) {
							logger.error("SocketChannel.close", e2);
						}
					}
				}, selectTimeout);
			} catch (Throwable e) {
				logger.error("Selector.run", e);
			}
		}
	}
}
