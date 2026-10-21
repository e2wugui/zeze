package Zeze.Services;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.AbstractAsyncLock;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Isolated;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

/** Uses real GCM start/stop, sockets and AsyncLock; no Application or database is needed. */
@Fast
@Isolated // The synchronous implementation is a process-wide singleton.
public class TestGcmRestartKeepsCurrentOwnership {
    private static final Binary KEY = new Binary("restart-owned-key");

    private static final class Client extends Service {
        Client() {
            super("TestGcmRestartKeepsCurrentOwnership.Client", new Config());
            AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
                    Login::new, null, TransactionLevel.None, DispatchMode.Direct));
            AddFactoryHandle(Acquire.TypeId_, new ProtocolFactoryHandle<>(
                    Acquire::new, null, TransactionLevel.None, DispatchMode.Direct));
            AddFactoryHandle(Reduce.TypeId_, new ProtocolFactoryHandle<>(
                    Reduce::new, rpc -> {
                        // Current owner keeps its permission: another writer must be denied.
                        rpc.Result.globalKey = rpc.Argument.globalKey;
                        rpc.Result.state = GlobalCacheManagerConst.StateModify;
                        rpc.SendResult();
                        return 0;
                    }, TransactionLevel.None, DispatchMode.Direct));
        }

        AsyncSocket connect(int port, int serverId) throws Exception {
            var connector = new Connector("127.0.0.1", port, false);
            getConfig().addConnector(connector);
            start();
            var socket = connector.WaitReady();
            var login = new Login();
            login.Argument.serverId = serverId;
            login.SendForWait(socket, 5000).get();
            assertEquals(0, login.getResultCode());
            return socket;
        }
    }

    private static Object field(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return requireNonNull(field.get(target));
    }

    private static int start(Object gcm) throws Exception {
        if (gcm instanceof GlobalCacheManagerAsyncServer async)
            async.start(InetAddress.getLoopbackAddress(), 0, new Config());
        else
            ((GlobalCacheManagerServer)gcm).start(InetAddress.getLoopbackAddress(), 0, new Config());
        return ((InetSocketAddress)requireNonNull(((TcpSocket)field(gcm, "serverSocket")).getLocalAddress())).getPort();
    }

    private static void stop(Object gcm) throws Exception {
        if (gcm instanceof GlobalCacheManagerAsyncServer async)
            async.stop();
        else
            ((GlobalCacheManagerServer)gcm).stop();
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentHashMap<Binary, Object> global(Object gcm) throws Exception {
        return (ConcurrentHashMap<Binary, Object>)field(gcm, "global");
    }

    private static Acquire acquire(AsyncSocket socket) {
        var acquire = new Acquire(KEY, GlobalCacheManagerConst.StateModify);
        acquire.SendForWait(socket, 5000).get();
        return acquire;
    }

    @Test
    @Timeout(30)
    public void oldReleaseMustNotEraseTheNewGenerationCacheState() throws Exception {
        verifyOldRelease(true, false);
    }

    @Test
    @Timeout(30)
    public void oldReleaseMustNotPermitASecondModifyOwner() throws Exception {
        verifyOldRelease(false, false);
    }

    @Test
    @Timeout(30)
    public void oldSyncReleaseMustNotEraseTheNewGenerationCacheState() throws Exception {
        verifyOldRelease(true, true);
    }

    @Test
    @Timeout(30)
    public void oldSyncReleaseMustNotPermitASecondModifyOwner() throws Exception {
        verifyOldRelease(false, true);
    }

    private static void verifyOldRelease(boolean checkMapping, boolean sync) throws Exception {
        Task.tryInitThreadPool();
        Object gcm = sync ? GlobalCacheManagerServer.getInstance() : new GlobalCacheManagerAsyncServer();
        ((GlobalCacheManagerServer.GCMConfig)field(gcm, "gcmConfig")).initialCapacity = 31;
        var oldClient = new Client();
        var currentClient = new Client();
        var contender = new Client();
        var holding = new CountDownLatch(1);
        var releaseLock = new CountDownLatch(1);
        var finished = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var oldSocket = oldClient.connect(start(gcm), 1);
                assertEquals(0, acquire(oldSocket).getResultCode());
                var oldState = requireNonNull(global(gcm).get(KEY));
                var oldLock = sync ? oldState : field(oldState, "lock");
                var holdingTask = executor.submit(() -> {
                    if (sync) {
                        var lock = (ReentrantLock)oldLock;
                        lock.lock();
                        try {
                            holding.countDown();
                            assertTrue(releaseLock.await(15, TimeUnit.SECONDS));
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        ((AbstractAsyncLock)oldLock).enter(() -> {
                            holding.countDown();
                            assertTrue(releaseLock.await(15, TimeUnit.SECONDS));
                        });
                    }
                    return null;
                });
                assertTrue(holding.await(5, TimeUnit.SECONDS));

                var holder = requireNonNull(((LongConcurrentHashMap<?>)field(gcm, "sessions")).get(1));
                Future<?> releaseTask = null;
                if (sync) {
                    //noinspection JavaReflectionMemberAccess - holder is the private runtime CacheHolder type.
                    var release = GlobalCacheManagerServer.class.getDeclaredMethod(
                            "release", holder.getClass(), Binary.class, boolean.class);
                    release.setAccessible(true);
                    releaseTask = executor.submit(() -> {
                        try {
                            release.invoke(gcm, holder, KEY, false);
                        } finally {
                            finished.countDown();
                        }
                        return null;
                    });
                    var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                    while (!((ReentrantLock)oldLock).hasQueuedThreads() && System.nanoTime() < deadline)
                        Thread.onSpinWait();
                    assertTrue(((ReentrantLock)oldLock).hasQueuedThreads());
                } else {
                    var future = new GlobalCacheManagerAsyncServer.CountDownFuture();
                    future.createOne();
                    future.then(__ -> finished.countDown());
                    //noinspection JavaReflectionMemberAccess - holder is the private runtime CacheHolder type.
                    var release = GlobalCacheManagerAsyncServer.class.getDeclaredMethod(
                            "releaseAsync", holder.getClass(), Binary.class,
                            GlobalCacheManagerAsyncServer.CountDownFuture.class);
                    release.setAccessible(true);
                    // The real daemon/Login cleanup primitive queues behind the occupied key lock.
                    release.invoke(gcm, holder, KEY, future);
                }

                stop(gcm);
                var newPort = start(gcm);
                var currentSocket = currentClient.connect(newPort, 2);
                assertEquals(0, acquire(currentSocket).getResultCode());
                var currentState = requireNonNull(global(gcm).get(KEY));
                assertNotSame(oldState, currentState);

                releaseLock.countDown();
                holdingTask.get(5, TimeUnit.SECONDS);
                assertTrue(finished.await(5, TimeUnit.SECONDS));
                if (releaseTask != null)
                    releaseTask.get(5, TimeUnit.SECONDS);
                if (checkMapping)
                    assertSame(currentState, global(gcm).get(KEY),
                            "old release must keep the new generation key and its Modify owner");
                var contenderSocket = contender.connect(newPort, 3);
                assertEquals(GlobalCacheManagerConst.AcquireModifyFailed, acquire(contenderSocket).getResultCode(),
                        "the current owner's refused Reduce must prevent a second writer");
            } finally {
                releaseLock.countDown();
                oldClient.stop();
                currentClient.stop();
                contender.stop();
                stop(gcm);
            }
        }
    }
}
