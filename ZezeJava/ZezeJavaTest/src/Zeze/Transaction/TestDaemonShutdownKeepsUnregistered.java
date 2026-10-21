package Zeze.Transaction;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.MappedByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Daemon;
import Zeze.Util.FastLock;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Isolated;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

/** Isolated because selecting ProcessDaemon uses the process-wide daemon port property. */
@Fast
@Isolated
public class TestDaemonShutdownKeepsUnregistered {
    private static Object field(Object target, String name) throws Exception {
        var f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return requireNonNull(f.get(target));
    }

    private static long activeTimeSlot(Object processDaemon) throws Exception {
        var lock = (FastLock)field(processDaemon, "channelLock");
        lock.lock();
        try {
            var mmap = (MappedByteBuffer)field(processDaemon, "mmap");
            var bytes = new byte[8];
            mmap.duplicate().get(0, bytes);
            return ByteBuffer.Wrap(bytes).ReadLong8();
        } finally {
            lock.unlock();
        }
    }

    @Test
    @Timeout(20)
    public void lateHeartbeatDuringStopMustNotRestoreAnUnregisteredSlot() throws Exception {
        Task.tryInitThreadPool();
        var inKeepAlive = new CountDownLatch(1);
        var finishKeepAlive = new CountDownLatch(1);
        var conf = new Config();
        conf.setNoDatabase(true);
        conf.setServiceManager("disable");
        var app = new Application("TestDaemonShutdownKeepsUnregistered", conf);
        var agent = new GlobalAgentBase(app) {
            @Override
            protected void cancelPending() {
            }

            @Override
            public void keepAlive() {
                inKeepAlive.countDown();
                try {
                    assertTrue(finishKeepAlive.await(10, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
        };
        agent.setActiveTime(System.currentTimeMillis() - agent.getConfig().serverKeepAliveIdleTimeout - 1);
        try (var peer = new DatagramSocket(0, InetAddress.getLoopbackAddress());
             var executor = Executors.newSingleThreadExecutor()) {
            var oldPort = System.setProperty(Daemon.propertyNamePort, String.valueOf(peer.getLocalPort()));
            AchillesHeelDaemon daemon;
            try {
                daemon = new AchillesHeelDaemon(app, new GlobalAgentBase[]{agent});
            } finally {
                if (oldPort == null)
                    System.clearProperty(Daemon.propertyNamePort);
                else
                    System.setProperty(Daemon.propertyNamePort, oldPort);
            }
            var appDaemon = Application.class.getDeclaredField("achillesHeelDaemon");
            appDaemon.setAccessible(true);
            appDaemon.set(app, daemon);
            try {
                var pd = field(daemon, "pd");
                daemon.setProcessDaemonActiveTime(agent, agent.getActiveTime());
                assertEquals(agent.getActiveTime(), activeTimeSlot(pd), "live heartbeats still publish normally");
                daemon.start();
                assertTrue(inKeepAlive.await(5, TimeUnit.SECONDS));
                var stopped = executor.submit(daemon::stopAndJoin);
                var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (activeTimeSlot(pd) != -1 && System.nanoTime() < deadline)
                    Thread.onSpinWait();
                assertEquals(-1, activeTimeSlot(pd), "stop must first unregister the mapped heartbeat slot");

                // Same path as a late Login/Acquire/KeepAlive success callback. Application.stop
                // keeps this daemon installed until stopAndJoin returns, while GCM is still open.
                agent.setActiveTime(System.currentTimeMillis());
                assertEquals(-1, activeTimeSlot(pd), "an old callback must not reactivate the stopping process");
                finishKeepAlive.countDown();
                stopped.get(5, TimeUnit.SECONDS);
            } finally {
                finishKeepAlive.countDown();
                daemon.stopAndJoin();
                appDaemon.set(app, null);
            }
        }
    }
}
