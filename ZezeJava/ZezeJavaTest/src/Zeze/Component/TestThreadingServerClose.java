package Zeze.Component;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.BLockName;
import Zeze.Builtin.Threading.MutexTryLock;
import Zeze.Builtin.Threading.MutexUnlock;
import Zeze.Builtin.Threading.ReadWriteLockOperate;
import Zeze.Builtin.Threading.SemaphoreCreate;
import Zeze.Builtin.Threading.SemaphoreRelease;
import Zeze.Builtin.Threading.SemaphoreTryAcquire;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestThreadingServerClose {

	@Test
	public void closeReleasesResourcesAndStopsTheirOwner() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("ThreadingServerClose");
		var server = new ThreadingServer(service, new ServiceManagerServer.Conf());
		var name = new BLockName(new BGlobalThreadId(1, 1), "close-resources");
		ThreadingServer.SimulateThread owner = null;
		try {
			for (int i = 0; i < 2; i++) {
				var request = new MutexTryLock();
				request.Argument.setLockName(name);
				server.ProcessMutexTryLockRequest(request);
				assertEquals(0, await(request));
			}
			var create = new SemaphoreCreate();
			create.Argument.setLockName(name);
			create.Argument.setPermits(2);
			server.ProcessSemaphoreCreateRequest(create);
			var acquire = new SemaphoreTryAcquire();
			acquire.Argument.setLockName(name);
			acquire.Argument.setPermits(2);
			server.ProcessSemaphoreTryAcquireRequest(acquire);
			assertEquals(0, await(acquire));
			operate(server, name, Threading.eEnterWrite);
			operate(server, name, Threading.eEnterRead);

			// Observe the actual resource owner; all acquisition and release use request handlers.
			Field owners = ThreadingServer.class.getDeclaredField("simulateThreads");
			owners.setAccessible(true);
			server.lock();
			try {
				owner = (ThreadingServer.SimulateThread)((Map<?, ?>)owners.get(server)).values().iterator().next();
			} finally {
				server.unlock();
			}
			var mutex = owner.getMutex(name.getName());
			var rwLock = owner.getReadWriteLock(name.getName());
			var semaphore = owner.getSemaphore(name.getName()).semaphore;
			server.close();
			owner.join(3_000);
			assertFalse(owner.isAlive(), "close must stop the resource-owning thread");
			assertEquals(2, semaphore.availablePermits());
			assertTrue(mutex.tryLock());
			mutex.unlock();
			assertTrue(rwLock.writeLock().tryLock());
			rwLock.writeLock().unlock();
		} finally {
			// Clean up the deliberately failing pre-fix run without leaving a non-daemon owner.
			if (owner != null && owner.isAlive()) {
				for (int i = 0; i < 2; i++) {
					var request = new MutexUnlock();
					request.Argument.setLockName(name);
					server.ProcessMutexUnlockRequest(request);
					await(request);
				}
				operate(server, name, Threading.eExitRead);
				operate(server, name, Threading.eExitWrite);
				var release = new SemaphoreRelease();
				release.Argument.setLockName(name);
				release.Argument.setPermits(2);
				server.ProcessSemaphoreReleaseRequest(release);
				await(release);
				owner.join(3_000);
			}
			server.close();
			service.stop();
		}
	}

	@Test
	public void closedServerRejectsNewResourceOwners() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("ThreadingServerClosedRequests");
		var server = new ThreadingServer(service, new ServiceManagerServer.Conf());
		var name = new BLockName(new BGlobalThreadId(1, 2), "closed-request");
		try {
			server.close();
			var request = new MutexTryLock();
			request.Argument.setLockName(name);
			server.ProcessMutexTryLockRequest(request);
			var result = await(request);
			if (result == 0) {
				var release = new MutexUnlock();
				release.Argument.setLockName(name);
				server.ProcessMutexUnlockRequest(release);
				await(release);
			}
			assertEquals(ThreadingServer.ResultCodeInvalidArgument, result);
		} finally {
			server.close();
			service.stop();
		}
	}

	private static void operate(ThreadingServer server, BLockName name, int type) throws Exception {
		var request = new ReadWriteLockOperate();
		request.Argument.setLockName(name);
		request.Argument.setOperateType(type);
		server.ProcessReadWriteLockOperateRequest(request);
		assertEquals(0, await(request));
	}

	private static long await(Rpc<?, ?> request) throws InterruptedException {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
		while (!request.isSendResultDone()) {
			assertTrue(System.nanoTime() < deadline, "request must receive a response");
			Thread.sleep(5);
		}
		return request.getResultCode();
	}
}
