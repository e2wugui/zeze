package Zeze.Onz;

import Zeze.Builtin.Onz.FlushReady;
import Zeze.Builtin.Onz.Ready;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Rpc;
import Zeze.Util.ConcurrentHashSet;

public class OnzTransactionPending {
	private final long onzTid = 0; // allocate todo
	private final ConcurrentHashSet<Rpc<?, ?>> readies = new ConcurrentHashSet<>();
	private final ConcurrentHashSet<Rpc<?, ?>> flushReadies = new ConcurrentHashSet<>();
	private final ConcurrentHashSet<AsyncSocket> zezes = new ConcurrentHashSet<>(); // todo

	public long getOnzTid() {
		return onzTid;
	}

	public void trySetReady(Ready r) {
		readies.add(r);

		if (readies.size() == zezes.size()) {
			// 简单的用数量判断，足够可靠了。
			for (var ready : readies)
				ready.SendResult();
		}
	}

	public void trySetFlushReady(FlushReady r) {
		flushReadies.add(r);

		if (flushReadies.size() == zezes.size()) {
			// 简单的用数量判断，足够可靠了。
			for (var ready : flushReadies)
				ready.SendResult();
		}
	}
}
