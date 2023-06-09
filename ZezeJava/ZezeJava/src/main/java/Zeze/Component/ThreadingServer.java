package Zeze.Component;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.QueryLockInfo;
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

    public ThreadingServer(Service service) {
        this.service = service;
    }

    public class SimulateThread extends Thread {
        private final BGlobalThreadId id;
        private long sessionId;
        private final LinkedBlockingQueue<Action1<SimulateThread>> actions = new LinkedBlockingQueue<>();
        private final HashMap<String, ReentrantLock> mutexRefs = new HashMap<>();

        public SimulateThread(BGlobalThreadId id) {
            this.id = id;
        }

        private boolean acquireNothing() {
            // 增加其他类型的同步机制，需要修改这里。
            return mutexRefs.isEmpty();
        }

        public ReentrantLock removeMutexRef(String name) {
            return mutexRefs.remove(name);
        }

        /*
        public ReentrantLock getMutexRef(String name) {
            return mutexRefs.get(name);
        }
        */

        public ReentrantLock getOrAddMutex(String name) {
            // see ref cache
            var mutex = mutexRefs.get(name);
            if (null != mutex)
                return mutex;

            // alloc
            mutex = mutexes.computeIfAbsent(name, (k) -> new ReentrantLock());
            mutexRefs.put(name, mutex);
            return mutex;
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
                    var mutex = This.removeMutexRef(r.Argument.getName());
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
}
