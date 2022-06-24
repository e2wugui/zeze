package Zeze.Net;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class Selectors {
	private static final Selectors Instance = new Selectors();

	public static Selectors getInstance() {
		return Instance;
	}

	private volatile Selector[] SelectorList;
	private final AtomicLong choiceCount = new AtomicLong();

	private Selectors() {
		add(Math.min(Runtime.getRuntime().availableProcessors(), 8));
	}

	public int getCount() {
		return SelectorList.length;
	}

	public synchronized void add(int count) {
		try {
			Selector[] tmp = SelectorList;
			tmp = tmp == null ? new Selector[count] : Arrays.copyOf(tmp, tmp.length + count);
			for (int i = tmp.length - count; i < tmp.length; i++) {
				tmp[i] = new Selector("Selector-" + i);
				tmp[i].start();
			}
			SelectorList = tmp;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Selector choice() {
		Selector[] tmp = SelectorList; // thread safe
		if (tmp == null)
			return null;

		long count = choiceCount.getAndIncrement();
		int index = (int)Long.remainderUnsigned(count, tmp.length);
		return tmp[index];
	}

	public synchronized void close() {
		Selector[] tmp = SelectorList;
		if (tmp != null) {
			SelectorList = null;
			for (Selector s : tmp)
				s.close();
		}
	}
}
