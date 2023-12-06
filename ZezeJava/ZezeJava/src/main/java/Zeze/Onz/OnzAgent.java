package Zeze.Onz;

import Zeze.Builtin.Onz.FlushReady;
import Zeze.Builtin.Onz.Ready;
import Zeze.Util.LongConcurrentHashMap;

public class OnzAgent extends AbstractOnzAgent {
	private final LongConcurrentHashMap<OnzTransactionPending> transactions = new LongConcurrentHashMap<>();

	@Override
	protected long ProcessReadyRequest(Ready r) throws Exception {
		var pending = transactions.get(r.Argument.getOnzTid());
		if (null == pending)
			return errorCode(eOnzTidNotFound);

		pending.trySetReady(r);
		return 0;
	}

	@Override
	protected long ProcessFlushReadyRequest(FlushReady r) throws Exception {
		var pending = transactions.get(r.Argument.getOnzTid());
		if (null == pending)
			return errorCode(eOnzTidNotFound);

		pending.trySetFlushReady(r);
		return 0;
	}

}
