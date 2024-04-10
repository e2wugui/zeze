package Zeze.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.BKeepAlive;
import Zeze.Builtin.Threading.KeepAlive;
import Zeze.Builtin.Threading.ReadWriteLockOperate;
import Zeze.Builtin.Threading.SemaphoreCreate;
import Zeze.Builtin.Threading.SemaphoreRelease;
import Zeze.Builtin.Threading.SemaphoreTryAcquire;
import Zeze.Net.Service;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Action1;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThreadingServer extends AbstractThreadingServer {
    static final Logger logger = LogManager.getLogger(ThreadingServer.class);
    private final Service service;
    private final ServiceManagerServer.Conf conf;
    private final HashMap<BGlobalThreadId, SimulateThread> simulateThreads = new HashMap<>();
    private final ConcurrentHashMap<Integer, SimulateThreads> simulateThreadsByServerId = new ConcurrentHashMap<>();

    // 每种锁一个命名空间。
    // 其他可做的优化【暂不考虑】。
    // WeakRef SimulateThread记住自己拥有的所有资源，当SimulateThread退出时，这里自动回收。
    private final ConcurrentHashMap<String, ReentrantLock> mutexes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Semaphore> semaphores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> rwLocks = new ConcurrentHashMap<>();

    private final Future<?> timeoutReleaseTask;

    public ThreadingServer(Service service, ServiceManagerServer.Conf conf) {
        this.service = service;
        this.conf = conf;
        this.timeoutReleaseTask = Task.scheduleUnsafe(60_000, 60_000, this::timeoutRelease);
    }

    public void close() {
        timeoutReleaseTask.cancel(false);
    }

    public Service getService() {
        return service;
    }

    static class SemaphoreAcquired {
        final Semaphore semaphore;
        int permits;

        SemaphoreAcquired(Semaphore s) {
            this.semaphore = s;
        }
    }

    public class SimulateThread extends Thread {
        private final BGlobalThreadId id;
        private final LinkedBlockingQueue<Action1<SimulateThread>> actions = new LinkedBlockingQueue<>();
        private final HashMap<String, ReentrantLock> mutexRefs = new HashMap<>();

        private final HashMap<String, SemaphoreAcquired> semaphoreRefs = new HashMap<>();

        private final HashMap<String, ReentrantReadWriteLock> rwLockRefs = new HashMap<>();

        public SimulateThread(BGlobalThreadId id) {
            this.id = id;
        }

        private boolean acquireNothing() {
            // 增加其他类型的同步机制，需要修改这里。
            return mutexRefs.isEmpty() && semaphoreRefs.isEmpty();
        }

        public ReentrantLock getMutex(String name) {
            // ref cache
            var mutex = mutexRefs.get(name);
            if (null != mutex)
                return mutex;

            // alloc
            return mutexes.computeIfAbsent(name, (k) -> new ReentrantLock());
        }

        SemaphoreAcquired getSemaphore(String name) {
            // ref cache
            var semaphoreAcq = semaphoreRefs.get(name);
            if (null != semaphoreAcq)
                return semaphoreAcq;

            // ref global
            var semaphore = semaphores.get(name);
            if (null != semaphore)
                return new SemaphoreAcquired(semaphore);
            return null;
        }

        public ReentrantReadWriteLock getReadWriteLock(String name) {
            // ref cache
            var rwLock = rwLockRefs.get(name);
            if (null != rwLock)
                return rwLock;

            // getOrAdd global
            return rwLocks.computeIfAbsent(name, (key) -> new ReentrantReadWriteLock());
        }

        @Override
        public void run() {
            while (true) {
                try {
                    var action = actions.poll();
                    if (null != action) {
                        action.run(this);
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

        public void release() {
            logger.info("timeout(thread=({}, {}))", id.getServerId(), id.getThreadId());
            for (var e : mutexRefs.entrySet()) {
                while (e.getValue().getHoldCount() > 0)
                    e.getValue().unlock();
                logger.info("mutex timeout(name={})", e.getKey());
            }
            mutexRefs.clear();

            for (var e : semaphoreRefs.entrySet()) {
                e.getValue().semaphore.release(e.getValue().permits);
                logger.info("semaphore timeout(name={})", e.getKey());
            }
            semaphoreRefs.clear();

            for (var e : rwLockRefs.entrySet()) {
                while (e.getValue().getReadHoldCount() > 0)
                    e.getValue().readLock().unlock();
                while (e.getValue().getWriteHoldCount() > 0)
                    e.getValue().writeLock().unlock();
                logger.info("rwLock timeout(name={})", e.getKey());
            }
            rwLockRefs.clear();
        }
    }

    public class SimulateThreads {
        private final HashSet<SimulateThread> threads = new HashSet<>();
        private long activeTime = System.currentTimeMillis();
        private final int serverId;
        private BKeepAlive.Data lastAppSerial;

        public SimulateThreads(int serverId) {
            this.serverId = serverId;
        }

        public int getServerId() {
            return serverId;
        }

        public void release() {
            for (var thread : threads) {
                thread.actions.offer(SimulateThread::release);
            }
        }
    }

    @Override
    protected long ProcessKeepAlive(KeepAlive p) throws Exception {
        var threads = simulateThreadsByServerId.computeIfAbsent(p.Argument.getServerId(), SimulateThreads::new);
        threads.activeTime = System.currentTimeMillis();
        if (null == threads.lastAppSerial) {
            threads.lastAppSerial = p.Argument;
            return 0; // first keepAlive。record only。
        }
        if (threads.lastAppSerial.getAppSerialId() != p.Argument.getAppSerialId()) {
            threads.release();
            threads.lastAppSerial = p.Argument;
            return 0;
        }
        // same app serialId. done.
        return 0;
    }

    private void timeoutRelease() {
        var now = System.currentTimeMillis();
        for (var threads : simulateThreadsByServerId.values()) {
            // 【30 minutes 没有联系】强制释放获得的所有资源。
            // 这个时间可以看作异常情况下，Agent获得锁后的最长安全工作时间。
            // 再超出，就没有锁定保证了。
            if (now - threads.activeTime > conf.threadingReleaseTimeout)
                threads.release();
        }
    }

    private boolean simulateThreadExit(BGlobalThreadId id) {
        lock();
        try {
            return null == simulateThreads.computeIfPresent(id, (key, This) -> {
                if (This.actions.isEmpty()) {
                    logger.info("simulate exit thread=({}, {})", id.getServerId(), id.getThreadId());
                    var x = simulateThreadsByServerId.get(This.id.getServerId());
                    if (null != x)
                        x.threads.remove(This);
                    return null;
                }
                return This;
            });
        } finally {
            unlock();
        }
    }

    private void simulateThreadOffer(BGlobalThreadId id, Action1<SimulateThread> action) {
        lock();
        try {
            var st = simulateThreads.computeIfAbsent(
                    id,
                    (key) -> {
                        var simulate = new SimulateThread(key);
                        simulateThreadsByServerId.computeIfAbsent(id.getServerId(), SimulateThreads::new)
                                .threads.add(simulate);
                        simulate.start();
                        logger.info("simulate new thread=({}, {})",
                                key.getServerId(), key.getThreadId());
                        return simulate;
                    });
            st.actions.offer(action);
        } finally {
            unlock();
        }
    }

    @Override
    protected long ProcessMutexTryLockRequest(Zeze.Builtin.Threading.MutexTryLock r) {
        logger.info("mutex.tryLock ININININ (thread=({}, {}), name={})",
                r.Argument.getLockName().getGlobalThreadId().getServerId(),
                r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                r.Argument.getLockName().getName());
        simulateThreadOffer(r.Argument.getLockName().getGlobalThreadId(),
                (This) -> {
                    var mutex = This.getMutex(r.Argument.getLockName().getName());
                    var locked = mutex.tryLock(r.Argument.getTimeoutMs(), TimeUnit.MILLISECONDS);
                    logger.info("mutex.tryLock(thread=({}, {}), name={}) -> {}",
                            r.Argument.getLockName().getGlobalThreadId().getServerId(),
                            r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                            r.Argument.getLockName().getName(), locked);
                    if (locked)
                        This.mutexRefs.put(r.Argument.getLockName().getName(), mutex);
                    r.SendResultCode(locked ? 0 : 1);
                });

        return 0;
    }

    @Override
    protected long ProcessMutexUnlockRequest(Zeze.Builtin.Threading.MutexUnlock r) {
        simulateThreadOffer(r.Argument.getLockName().getGlobalThreadId(),
                (This) -> {
                    var mutex = This.mutexRefs.get(r.Argument.getLockName().getName());
                    if (null != mutex) {
                        mutex.unlock();
                        var hold = mutex.getHoldCount();
                        if (0 == hold)
                            This.mutexRefs.remove(r.Argument.getLockName().getName());
                        logger.info("mutex.unlock(thread=({}, {}), name={}) hold={}",
                                r.Argument.getLockName().getGlobalThreadId().getServerId(),
                                r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                                r.Argument.getLockName().getName(),
                                hold);
                        r.SendResultCode(hold);
                        return;
                    }
                    r.SendResultCode(0);
                });
        return 0;
    }

    @Override
    protected long ProcessReadWriteLockOperateRequest(ReadWriteLockOperate r) throws Exception {
        switch (r.Argument.getOperateType()) {
        case Threading.eEnterRead:
            simulateThreadOffer(r.Argument.getLockName().getGlobalThreadId(),
                    (This) -> {
                        var rwLock = This.getReadWriteLock(r.Argument.getLockName().getName());
                        var locked = rwLock.readLock().tryLock(r.Argument.getTimeoutMs(), TimeUnit.MILLISECONDS);
                        if (locked)
                            This.rwLockRefs.put(r.Argument.getLockName().getName(), rwLock);

                        logger.info("RWLock.enterRead(thread=({}, {}), name={}) -> {}",
                                r.Argument.getLockName().getGlobalThreadId().getServerId(),
                                r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                                r.Argument.getLockName().getName(),
                                locked);
                        r.SendResultCode(locked ? 0 : 1);
                    });
            break;

        case Threading.eEnterWrite:
            simulateThreadOffer(r.Argument.getLockName().getGlobalThreadId(),
                    (This) -> {
                        var rwLock = This.getReadWriteLock(r.Argument.getLockName().getName());
                        var locked = rwLock.writeLock().tryLock(r.Argument.getTimeoutMs(), TimeUnit.MILLISECONDS);
                        if (locked)
                            This.rwLockRefs.put(r.Argument.getLockName().getName(), rwLock);

                        logger.info("RWLock.enterWrite(thread=({}, {}), name={}) -> {}",
                                r.Argument.getLockName().getGlobalThreadId().getServerId(),
                                r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                                r.Argument.getLockName().getName(),
                                locked);
                        r.SendResultCode(locked ? 0 : 1);
                    });
            break;

        case Threading.eExitRead:
            simulateThreadOffer(r.Argument.getLockName().getGlobalThreadId(),
                    (This) -> {
                        var rwLock = This.rwLockRefs.get(r.Argument.getLockName().getName());
                        if (null != rwLock) {
                            rwLock.readLock().unlock();
                            var hold = rwLock.getReadHoldCount();
                            if (hold == 0)
                                This.rwLockRefs.remove(r.Argument.getLockName().getName());
                            logger.info("RWLock.exitRead(thread=({}, {}), name={} hold={})",
                                    r.Argument.getLockName().getGlobalThreadId().getServerId(),
                                    r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                                    r.Argument.getLockName().getName(),
                                    hold);

                            r.SendResultCode(hold);
                            return;
                        }
                        r.SendResultCode(0);
                    });
            break;

        case Threading.eExitWrite:
            simulateThreadOffer(r.Argument.getLockName().getGlobalThreadId(),
                    (This) -> {
                        var rwLock = This.rwLockRefs.get(r.Argument.getLockName().getName());
                        if (null != rwLock) {
                            rwLock.writeLock().unlock();
                            var hold = rwLock.getWriteHoldCount();
                            if (hold == 0)
                                This.rwLockRefs.remove(r.Argument.getLockName().getName());

                            logger.info("RWLock.exitWrite(thread=({}, {}), name={}) hold={}",
                                    r.Argument.getLockName().getGlobalThreadId().getServerId(),
                                    r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                                    r.Argument.getLockName().getName(),
                                    hold);
                            r.SendResultCode(hold);
                            return; // done
                        }
                        r.SendResultCode(0);
                    });
            break;
        }
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
        simulateThreadOffer(r.Argument.getLockName().getGlobalThreadId(),
                (This) -> {
                    var semaphoreAcq = This.semaphoreRefs.get(r.Argument.getLockName().getName());
                    if (null != semaphoreAcq) {
                        semaphoreAcq.semaphore.release(r.Argument.getPermits());
                        semaphoreAcq.permits -= r.Argument.getPermits();
                        if (semaphoreAcq.permits <= 0) {
                            // 马上要删除了，这个值本来不需要重置。如果下一次申请继续使用这个对象，必须设为0。
                            // 【现在不清除它，让后面的日志和结果能反应更多信息】
                            // semaphoreAcq.permits = 0;
                            This.semaphoreRefs.remove(r.Argument.getLockName().getName());
                        }
                        logger.info("semaphore.release(thread=({}, {}), name={}) permits={}",
                                r.Argument.getLockName().getGlobalThreadId().getServerId(),
                                r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                                r.Argument.getLockName().getName(),
                                semaphoreAcq.permits);
                        r.SendResultCode(semaphoreAcq.permits);
                        return; // done
                    }
                    r.SendResultCode(0);
                });
        return 0;
    }

    @Override
    protected long ProcessSemaphoreTryAcquireRequest(SemaphoreTryAcquire r) throws Exception {
        simulateThreadOffer(r.Argument.getLockName().getGlobalThreadId(),
                (This) -> {
                    var semaphoreAcq = This.getSemaphore(r.Argument.getLockName().getName());
                    if (null == semaphoreAcq) {
                        r.SendResultCode(1);
                        return; // done
                    }
                    var acquired = semaphoreAcq.semaphore.tryAcquire(r.Argument.getPermits(),
                            r.Argument.getTimeoutMs(), TimeUnit.MILLISECONDS);
                    if (acquired) {
                        semaphoreAcq.permits += r.Argument.getPermits();
                        This.semaphoreRefs.put(r.Argument.getLockName().getName(), semaphoreAcq);
                    }
                    logger.info("semaphore.tryAcquire(thread=({}, {}), name={}) -> {}",
                            r.Argument.getLockName().getGlobalThreadId().getServerId(),
                            r.Argument.getLockName().getGlobalThreadId().getThreadId(),
                            r.Argument.getLockName().getName(),
                            acquired);
                    r.SendResultCode(acquired ? 0 : 2);
                });
        return 0;
    }
}
