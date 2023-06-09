package Zeze.Component;

import java.util.concurrent.ExecutionException;
import Zeze.Application;
import Zeze.Builtin.Threading.MutexTryLock;
import Zeze.Builtin.Threading.MutexUnlock;
import Zeze.Builtin.Threading.QueryLockInfo;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Task;

public class Threading extends AbstractThreading {
	@Override
	protected long ProcessQueryLockInfoRequest(QueryLockInfo r) throws Exception {
		return 0;
	}
}
