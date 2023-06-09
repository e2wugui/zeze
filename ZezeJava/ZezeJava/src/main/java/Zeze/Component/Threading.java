package Zeze.Component;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.BLockName;
import Zeze.Builtin.Threading.MutexTryLock;
import Zeze.Builtin.Threading.MutexUnlock;
import Zeze.Builtin.Threading.QueryLockInfo;
import Zeze.IModule;
import Zeze.Net.Service;
import Zeze.Util.ConcurrentHashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Threading extends AbstractThreading {
	static final Logger logger = LogManager.getLogger(Threading.class);

	public Service service;
	private int progressId;

	public Threading(Service service, int progressId) {
		this.service = service;
		this.progressId = progressId;
	}

	@Override
	protected long ProcessQueryLockInfoRequest(QueryLockInfo r) throws Exception {
		for (var lockName : r.Argument.getLockNames())
			if (!acquired.containsKey(lockName))
				r.Result.getLockNames().add(lockName);
		r.SendResult();
		return 0;
	}

	public ConcurrentHashMap<BLockName, Concurrent> acquired = new ConcurrentHashMap<>();

	public interface Concurrent {
		// 所有Threading并发控制机制的基类。
		// 以后可能定义公共方法。
	}

	// 即使相同的名字，每个线程调用createMutex也是创建新的实例。
	// 多个线程共享一个实例也是可以的。
	public class Mutex implements Concurrent {
		private final String name;

		Mutex(String name) {
			this.name = name;
		}

		public boolean tryLock(int timeoutMs) {
			var r = new MutexTryLock();
			var globalThreadId = new BGlobalThreadId(progressId, Thread.currentThread().getId());
			var lockName = new BLockName(globalThreadId, name);
			r.Argument.setLockName(lockName);
			r.Argument.setTimeoutMs(timeoutMs);
			r.SendForWait(service.GetSocket(), timeoutMs + 1000).await();
			if (r.getResultCode() == 0) {
				acquired.put(lockName, this);
				return true;
			}
			return false;
		}

		public void unlock() {
			var globalThreadId = new BGlobalThreadId(progressId, Thread.currentThread().getId());
			var lockName = new BLockName(globalThreadId, name);

			// 完美方案应该unlock成功以后才释放。这里先这样写了。
			if (!acquired.remove(lockName, this))
				return;

			var r = new MutexUnlock();
			r.Argument = lockName;
			r.SendForWait(service.GetSocket()).await();
			if (r.getResultCode() != 0)
				logger.error("unlock error={}", IModule.getErrorCode(r.getResultCode()));
		}
	}

	public Mutex createMutex(String name) {
		return new Mutex(name);
	}
}
