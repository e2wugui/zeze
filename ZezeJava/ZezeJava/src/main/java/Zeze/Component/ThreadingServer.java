package Zeze.Component;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.QueryLockInfo;
import Zeze.Builtin.Threading.SemaphoreCreate;
import Zeze.Builtin.Threading.SemaphoreRelease;
import Zeze.Builtin.Threading.SemaphoreTryAcquire;
import Zeze.Net.Service;
import Zeze.Util.Action1;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThreadingServer extends AbstractThreadingServer {
    static final Logger logger = LogManager.getLogger(ThreadingServer.class);
    private final Service service;
    private final HashMap<BGlobalThreadId, SimulateThread> simulateThreads = new HashMap<>();

    // 全局mutex一个命名空间。
    // 其他可做的优化【暂不考虑】。
    // WeakRef SimulateThread记住自己拥有的所有资源，当SimulateThread退出时，这里自动回收。
    private final ConcurrentHashMap<String, ReentrantLock> mutexes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Semaphore> semaphores = new ConcurrentHashMap<>();

    public ThreadingServer(Service service) {
        this.service = service;
    }

    public class SimulateThread extends Thread {
        private final BGlobalThreadId id;
        private long sessionId;
        private final LinkedBlockingQueue<Action1<SimulateThread>> actions = new LinkedBlockingQueue<>();
        private final HashMap<String, ReentrantLock> mutexRefs = new HashMap<>();

        static class SemaphoreAcquired {
            final Semaphore semaphore;
            int permits;

            SemaphoreAcquired(Semaphore s) {
                this.semaphore = s;
            }
        }

        private final HashMap<String, SemaphoreAcquired> semaphoreRefs = new HashMap<>();

        public SimulateThread(BGlobalThreadId id) {
            this.id = id;
        }

        private boolean acquireNothing() {
            // 增加其他类型的同步机制，需要修改这里。
            return mutexRefs.isEmpty() && semaphoreRefs.isEmpty();
        }

        public ReentrantLock getOrAddMutex(String name) {
            // ref cache
            var mutex = mutexRefs.get(name);
            if (null != mutex)
                return mutex;

            // alloc
            mutex = mutexes.computeIfAbsent(name, (k) -> new ReentrantLock());
            mutexRefs.put(name, mutex);
            return mutex;
        }

        public SemaphoreAcquired getSemaphore(String name) {
            // ref cache
            var semaphoreAcq = semaphoreRefs.get(name);
            if (null != semaphoreAcq)
                return semaphoreAcq;

            // ref global
            var semaphore = semaphores.get(name);
            if (null != semaphore)
                semaphoreRefs.put(name, semaphoreAcq = new SemaphoreAcquired(semaphore));
            return semaphoreAcq;
        }

        @Override
        public void run() {
            var lastQueryTime = System.currentTimeMillis();
            var queryTimeout = 10_000;
            while (true) {
                try {
                    var timeout = queryTimeout - (System.currentTimeMillis() - lastQueryTime);
                    var action = actions.poll(timeout, TimeUnit.MILLISECONDS);
                    if (null != action) {
                        action.run(this);
                    }

                    var now = System.currentTimeMillis();
                    if (now - lastQueryTime >= 10_000) {
                        lastQueryTime = now;
                        var r = new QueryLockInfo();
                        // todo prepare allocated locks.
                        r.Send(service.GetSocket(sessionId), (p) -> {
                            actions.offer((This) -> {
                                // todo process missing lock.
                            });
                            return 0;
                        });
                    }

                    if (acquireNothing()) {
                        // 没有已分配资源的时候，延时200ms，准备退出。
                        action = actions.poll(200, TimeUnit.MILLISECONDS);
                        if (null != action) {
                            action.run(this);
                            continue; // 发现新任务，继续工作，中断退出。
                        }
                        if (simulateThreadExit(id))
                            break; // 真正退出...
                        // else continue
                    }
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        }
    }

    private synchronized boolean simulateThreadExit(BGlobalThreadId id) {
        return null == simulateThreads.computeIfPresent(id, (key, This) -> {
            if (This.actions.isEmpty()) {
                logger.info("simulate exit thread=({}, {})", id.getServerId(), id.getThreadId());
                return null;
            }
            return This;
        });
    }

    private synchronized void simulateThreadOffer(BGlobalThreadId id, long sessionId, Action1<SimulateThread> action) {
        var st = simulateThreads.computeIfAbsent(
                id,
                (key) -> {
                    var simulate = new SimulateThread(key);
                    simulate.start();
                    logger.info("simulate new thread=({}, {}), sessionId={}",
                            key.getServerId(), key.getThreadId(), sessionId);
                    return simulate;
                });
        st.sessionId = sessionId;
        st.actions.offer(action);
    }

    @Override
    protected long ProcessMutexTryLockRequest(Zeze.Builtin.Threading.MutexTryLock r) {
        logger.info("mutex.tryLock ININININ (thread=({}, {}), name={})",
                r.Argument.getLockName().getGlobalThreadId().getServerId(),
                r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                r.Argument.getLockName().getName());
        simulateThreadOffer(r.Argument.getLockName().getGlobalThreadId(), r.getSender().getSessionId(),
                (This) -> {
                    var mutex = This.getOrAddMutex(r.Argument.getLockName().getName());
                    var locked = mutex.tryLock(r.Argument.getTimeoutMs(), TimeUnit.MILLISECONDS);
                    logger.info("mutex.tryLock(thread=({}, {}), name={}) -> {}",
                            r.Argument.getLockName().getGlobalThreadId().getServerId(),
                            r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                            r.Argument.getLockName().getName(), locked);
                    r.SendResultCode(locked ? 0 : 1);
                });

        return 0;
    }

    @Override
    protected long ProcessMutexUnlockRequest(Zeze.Builtin.Threading.MutexUnlock r) {
        simulateThreadOffer(r.Argument.getGlobalThreadId(), r.getSender().getSessionId(),
                (This) -> {
                    var mutex = This.mutexRefs.remove(r.Argument.getName());
                    if (null != mutex) {
                        mutex.unlock();
                        logger.info("mutex.unlock(thread=({}, {}), name={})",
                                r.Argument.getGlobalThreadId().getServerId(),
                                r.Argument.getGlobalThreadId().getThreadId(),
                                r.Argument.getName());
                    }
                    r.SendResultCode(0);
                });
        return 0;
    }

    @Override
    protected long ProcessSemaphoreCreateRequest(SemaphoreCreate r) throws Exception {
        semaphores.computeIfAbsent(r.Argument.getLockName().getName(),
                (key) -> new Semaphore(r.Argument.getPermits()));
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessSemaphoreReleaseRequest(SemaphoreRelease r) throws Exception {
        simulateThreadOffer(r.Argument.getLockName().getGlobalThreadId(), r.getSender().getSessionId(),
                (This) -> {
                    var semaphoreAcq = This.semaphoreRefs.get(r.Argument.getLockName().getName());
                    if (null != semaphoreAcq) {
                        if (r.Argument.getPermits() > semaphoreAcq.permits) {
                            logger.error("semaphore.release(thread=({}, {}), name={})",
                                    r.Argument.getLockName().getGlobalThreadId().getServerId(),
                                    r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                                    r.Argument.getLockName().getName());
                            r.SendResultCode(1);
                            return;
                        }
                        semaphoreAcq.semaphore.release(r.Argument.getPermits());
                        semaphoreAcq.permits -= r.Argument.getPermits();
                        if (semaphoreAcq.permits == 0)
                            This.semaphoreRefs.remove(r.Argument.getLockName().getName());
                        logger.info("semaphore.release(thread=({}, {}), name={}) remain={}",
                                r.Argument.getLockName().getGlobalThreadId().getServerId(),
                                r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                                r.Argument.getLockName().getName(),
                                semaphoreAcq.permits);
                    }
                    r.SendResult();
                });
        return 0;
    }

    @Override
    protected long ProcessSemaphoreTryAcquireRequest(SemaphoreTryAcquire r) throws Exception {
        simulateThreadOffer(r.Argument.getLockName().getGlobalThreadId(), r.getSender().getSessionId(),
                (This) -> {
                    var semaphoreAcq = This.getSemaphore(r.Argument.getLockName().getName());
                    if (null == semaphoreAcq) {
                        r.SendResultCode(1);
                    } else {
                        var acquired = semaphoreAcq.semaphore.tryAcquire(r.Argument.getPermits(),
                                r.Argument.getTimeoutMs(), TimeUnit.MILLISECONDS);
                        if (acquired)
                            semaphoreAcq.permits += r.Argument.getPermits();
                        logger.info("semaphore.tryAcquire(thread=({}, {}), name={}) -> {}",
                                r.Argument.getLockName().getGlobalThreadId().getServerId(),
                                r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                                r.Argument.getLockName().getName(),
                                acquired);
                        r.SendResultCode(acquired ? 0 : 2);
                    }
                });
        return 0;
    }
}
