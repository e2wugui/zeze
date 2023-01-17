package Zeze.Net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Selectors {
	public static final class InstanceHolder { // for lazy-init
		private static final Selectors instance = new Selectors("Selector");
	}

	public static Selectors getInstance() {
		return InstanceHolder.instance;
	}

	public static final int DEFAULT_BBPOOL_BLOCK_SIZE = 32 * 1024; // 单个buffer的字节容量,推荐等于Socket.getSendBufferSize()
	public static final int DEFAULT_BBPOOL_LOCAL_CAPACITY = 1000; // 本地池的最大保留buffer数量
	public static final int DEFAULT_BBPOOL_MOVE_COUNT = 1000; // 本地池和全局池之间移动一次的buffer数量
	public static final int DEFAULT_BBPOOL_GLOBAL_CAPACITY = 100 * DEFAULT_BBPOOL_MOVE_COUNT; // 全局池的最大buffer数量
	public static final int DEFAULT_SELECT_TIMEOUT = 0; // 0表示无超时,>0表示每次select的超时毫秒数
	public static final int DEFAULT_READ_BUFFER_SIZE = 32 * 1024; // 读buffer的字节容量

	private final String name;
	private final int bbPoolBlockSize;
	private final int bbPoolGlobalCapacity;
	private final int bbPoolLocalCapacity;
	private final int bbPoolMoveCount;
	private final int selectTimeout;
	private final int readBufferSize;
	private final ArrayList<ByteBuffer> bbGlobalPool = new ArrayList<>(); // 全局池
	private final Lock bbGlobalPoolLock = new ReentrantLock(); // 全局池的锁
	private volatile Selector[] selectorList;
	private final AtomicLong choiceCount = new AtomicLong();

	public Selectors(String name) {
		this(name, 1, DEFAULT_BBPOOL_BLOCK_SIZE, DEFAULT_BBPOOL_LOCAL_CAPACITY, DEFAULT_BBPOOL_MOVE_COUNT,
				DEFAULT_BBPOOL_GLOBAL_CAPACITY, DEFAULT_SELECT_TIMEOUT, DEFAULT_READ_BUFFER_SIZE);
	}

	public Selectors(String name, int count, int bbPoolBlockSize, int bbPoolLocalCapacity, int bbPoolMoveCount) {
		this(name, count, bbPoolBlockSize, bbPoolLocalCapacity, bbPoolMoveCount, DEFAULT_BBPOOL_GLOBAL_CAPACITY,
				DEFAULT_SELECT_TIMEOUT, DEFAULT_READ_BUFFER_SIZE);
	}

	public Selectors(String name, int count, int bbPoolBlockSize, int bbPoolLocalCapacity, int bbPoolMoveCount,
					 int selectTimeout) {
		this(name, count, bbPoolBlockSize, bbPoolLocalCapacity, bbPoolMoveCount, DEFAULT_BBPOOL_GLOBAL_CAPACITY,
				selectTimeout, DEFAULT_READ_BUFFER_SIZE);
	}

	public Selectors(String name, int count, int bbPoolBlockSize, int bbPoolLocalCapacity, int bbPoolMoveCount,
					 int bbPoolGlobalCapacity, int selectTimeout) {
		this(name, count, bbPoolBlockSize, bbPoolLocalCapacity, bbPoolMoveCount, bbPoolGlobalCapacity,
				selectTimeout, DEFAULT_READ_BUFFER_SIZE);
	}

	public Selectors(String name, int count, int bbPoolBlockSize, int bbPoolLocalCapacity, int bbPoolMoveCount,
					 int bbPoolGlobalCapacity, int selectTimeout, int readBufferSize) {
		if (bbPoolBlockSize <= 0)
			throw new IllegalArgumentException("bbPoolBlockSize <= 0: " + bbPoolBlockSize);
		if (bbPoolLocalCapacity < 0)
			throw new IllegalArgumentException("bbPoolLocalCapacity < 0: " + bbPoolLocalCapacity);
		if (bbPoolMoveCount <= 0)
			throw new IllegalArgumentException("bbPoolMoveCount <= 0: " + bbPoolMoveCount);
		if (bbPoolGlobalCapacity < 0)
			throw new IllegalArgumentException("bbPoolGlobalCapacity < 0: " + bbPoolGlobalCapacity);
		if (selectTimeout < 0)
			throw new IllegalArgumentException("selectTimeout < 0: " + selectTimeout);
		if (readBufferSize <= 0)
			throw new IllegalArgumentException("readBufferSize <= 0: " + readBufferSize);
		this.name = name;
		this.bbPoolBlockSize = bbPoolBlockSize;
		this.bbPoolLocalCapacity = bbPoolLocalCapacity;
		this.bbPoolMoveCount = bbPoolMoveCount;
		this.bbPoolGlobalCapacity = bbPoolGlobalCapacity;
		this.selectTimeout = selectTimeout;
		this.readBufferSize = readBufferSize;
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

	ArrayList<ByteBuffer> getBbGlobalPool() {
		return bbGlobalPool;
	}

	Lock getBbGlobalPoolLock() {
		return bbGlobalPoolLock;
	}

	public int getCount() {
		return selectorList.length;
	}

	public Selectors add(int count) {
		try {
			Selector[] tmp = selectorList;
			tmp = tmp == null ? new Selector[count] : Arrays.copyOf(tmp, tmp.length + count);
			for (int i = tmp.length - count; i < tmp.length; i++) {
				tmp[i] = new Selector(this, name + '-' + i);
				tmp[i].start();
			}
			selectorList = tmp;
			return this;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Selector choice() {
		Selector[] tmp = selectorList; // thread safe
		if (tmp == null)
			return null;

		long count = choiceCount.getAndIncrement();
		int index = (int)((count & Long.MAX_VALUE) % tmp.length);
		return tmp[index];
	}

	public synchronized void close() {
		Selector[] tmp = selectorList;
		if (tmp != null) {
			selectorList = null;
			for (Selector s : tmp)
				s.close();
		}
	}
}
