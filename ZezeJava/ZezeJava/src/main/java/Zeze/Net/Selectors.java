package Zeze.Net;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class Selectors {
	private static final Selectors instance = new Selectors();

	public static Selectors getInstance() {
		return instance;
	}

	private volatile Selector[] selectorList;
	private final AtomicLong choiceCount = new AtomicLong();

	private Selectors() {
		add(Math.min(Runtime.getRuntime().availableProcessors(), 8));
	}

	public int getCount() {
		return selectorList.length;
	}

	public synchronized void add(int count) {
		try {
			Selector[] tmp = selectorList;
			tmp = tmp == null ? new Selector[count] : Arrays.copyOf(tmp, tmp.length + count);
			for (int i = tmp.length - count; i < tmp.length; i++) {
				tmp[i] = new Selector("Selector-" + i);
				tmp[i].start();
			}
			selectorList = tmp;
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
