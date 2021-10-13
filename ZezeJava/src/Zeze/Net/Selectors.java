package Zeze.Net;

import org.pcollections.Empty;
import org.pcollections.PVector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class Selectors {
	private static Selectors Instance = new Selectors();

	public static Selectors getInstance() {
		return Instance;
	}

	private PVector<Selector> SelectorList = Empty.vector();
	private AtomicLong choiceCount = new AtomicLong();

	private Selectors() {
		add(Runtime.getRuntime().availableProcessors());
	}

	public void add(int count) {
		try {
			ArrayList<Selector> adding = new ArrayList<>();
			for (int i = 0; i < count; ++i)
				adding.add(new Selector("SelectorThread" + i));

			for (var add : adding)
				add.start();

			SelectorList = SelectorList.plusAll(adding);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Selector choice() {
		var tmp = SelectorList; // thread safe
		if (tmp.isEmpty())
			return null;

		long count = choiceCount.incrementAndGet();
		int index = (int)Long.remainderUnsigned(count, tmp.size());
		return tmp.get(index);
	}
	
	public void close() {
		PVector<Selector> tmp;
		synchronized (this) {
			tmp = SelectorList;
			SelectorList = Empty.vector();
		}
		for (var s : tmp)
			s.close();
	}
}
