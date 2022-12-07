package Zeze.Net;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class Selectors {
	public static final class InstanceHolder { // for lazy-init
		private static final Selectors instance = new Selectors("Selector");
	}

	public static Selectors getInstance() {
		return InstanceHolder.instance;
	}

	private final String name;
	private volatile Selector[] selectorList;
	private final AtomicLong choiceCount = new AtomicLong();

	public Selectors(String name) {
		this.name = name;
		add(Math.min(Runtime.getRuntime().availableProcessors(), 8));
	}

	public Selectors(String name, int count, int bufferSize, int bbPoolLocalCapacity, int bbPoolMoveCount,
					 int bbPoolGlobalCapacity) {
		this.name = name;
		add(count, bufferSize, bbPoolLocalCapacity, bbPoolMoveCount, bbPoolGlobalCapacity);
	}

	public int getCount() {
		return selectorList.length;
	}

	public Selectors add(int count) {
		return add(count, Selector.DEFAULT_BUFFER_SIZE, Selector.DEFAULT_BBPOOL_LOCAL_CAPACITY,
				Selector.DEFAULT_BBPOOL_MOVE_COUNT, Selector.DEFAULT_BBPOOL_GLOBAL_CAPACITY);
	}

	public synchronized Selectors add(int count, int bufferSize, int bbPoolLocalCapacity, int bbPoolMoveCount,
									  int bbPoolGlobalCapacity) {
		try {
			Selector[] tmp = selectorList;
			tmp = tmp == null ? new Selector[count] : Arrays.copyOf(tmp, tmp.length + count);
			for (int i = tmp.length - count; i < tmp.length; i++) {
				tmp[i] = new Selector(name + '-' + i, bufferSize, bbPoolLocalCapacity, bbPoolMoveCount,
						bbPoolGlobalCapacity);
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
		int index = (int)Long.remainderUnsigned(count, tmp.length);
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
