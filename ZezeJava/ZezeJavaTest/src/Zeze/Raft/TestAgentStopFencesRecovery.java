package Zeze.Raft;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestAgentStopFencesRecovery {

	private static final class CountingConnector extends Connector {
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch resume = new CountDownLatch(1);
		volatile boolean pause;
		volatile boolean active;
		int starts;

		CountingConnector() {
			super("127.0.0.1", 19531);
		}

		@Override
		public void start() {
			if (pause) {
				entered.countDown();
				try {
					assertTrue(resume.await(5, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					throw new AssertionError(e);
				}
			}
			starts++;
			active = true;
		}

		@Override
		public void stop() {
			active = false;
		}
	}

	private static final class Fixture implements AutoCloseable {
		final Agent agent;
		final CountingConnector connector = new CountingConnector();
		final Agent.ConnectorProxy leader = new Agent.ConnectorProxy(connector);
		final AtomicInteger callbacks = new AtomicInteger();

		Fixture(String name) throws Exception {
			Task.tryInitThreadPool();
			// 无 Application、共享内存库或网络 IO；连接 start/stop 由可控替身记录。
			var conf = RaftConfig.loadFromString("""
					<raft Name="">
					    <node Host="127.0.0.1" Port="19530"/>
					    <node Host="127.0.0.1" Port="19531"/>
					</raft>
					""");
			agent = new Agent("TestAgentStopFencesRecovery." + name, conf, new Config());
			// 禁用自动 tick，直接控制真实 resend 入口的交错顺序。
			var timer = Agent.class.getDeclaredField("resendTask");
			timer.setAccessible(true);
			((Future<?>)Objects.requireNonNull(timer.get(agent))).cancel(false);
			timer.set(agent, null);
			var config = Objects.requireNonNull(agent.getClient()).getConfig();
			config.removeConnector(Objects.requireNonNull(config.findConnector(connector.getName())));
			config.addConnector(connector);
			agent.setOnSetLeader(__ -> callbacks.incrementAndGet());
		}

		LeaderIs notification() {
			var rpc = new LeaderIs();
			rpc.Argument.setTerm(3);
			rpc.Argument.setLeaderId(connector.getName());
			rpc.Argument.setLeader(true);
			return rpc;
		}

		void deliverQueuedNotification() throws Exception {
			var method = Agent.class.getDeclaredMethod("processLeaderIs", LeaderIs.class);
			method.setAccessible(true);
			try {
				method.invoke(agent, notification());
			} catch (InvocationTargetException e) {
				throw Task.forceThrow(e.getCause());
			}
		}

		void resend() {
			try {
				var method = Agent.class.getDeclaredMethod("resend", boolean.class);
				method.setAccessible(true);
				method.invoke(agent, false);
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		}

		@Override
		public void close() throws Exception {
			connector.resume.countDown();
			agent.stop();
		}
	}

	@Test
	void lateLeaderCannotReviveStoppedAgent() throws Exception {
		try (var f = new Fixture("lateLeader")) {
			f.agent.stop();
			assertFalse(f.agent.setLeader(f.notification(), f.leader));
			assertNull(f.agent.getLeader());
			assertEquals(0, f.connector.starts);
			assertEquals(0, f.callbacks.get());
		}
	}

	@Test
	void queuedNotificationAfterStopIsHarmless() throws Exception {
		try (var f = new Fixture("queuedLeader")) {
			f.agent.stop();
			assertDoesNotThrow(f::deliverQueuedNotification);
			assertNull(f.agent.getLeader());
			assertEquals(0, f.connector.starts);
			assertEquals(0, f.callbacks.get());
		}
	}

	@Test
	void stopFencesAlreadyRunningReconnect() throws Exception {
		try (var f = new Fixture("inFlightResend")) {
			assertTrue(f.agent.setLeader(f.notification(), f.leader));
			f.connector.pause = true;
			var resending = CompletableFuture.runAsync(f::resend);
			var stopEntered = new CountDownLatch(1);
			CompletableFuture<Void> stopping = null;
			try {
				assertTrue(f.connector.entered.await(5, TimeUnit.SECONDS));
				stopping = CompletableFuture.runAsync(() -> {
					stopEntered.countDown();
					try {
						f.agent.stop();
					} catch (Exception e) {
						throw new AssertionError(e);
					}
				});
				assertTrue(stopEntered.await(5, TimeUnit.SECONDS));
				try {
					stopping.get(200, TimeUnit.MILLISECONDS);
				} catch (TimeoutException expected) {
					// 正确实现的 stop 等待当前短建连临界区，随后关闭它创建的连接。
				}
			} finally {
				f.connector.resume.countDown();
				resending.get(5, TimeUnit.SECONDS);
				if (stopping != null)
					stopping.get(5, TimeUnit.SECONDS);
			}
			assertFalse(f.connector.active, "stop 返回后，旧重发轮不能留下活跃连接");
			assertNull(f.agent.getLeader());
		}
	}

	@Test
	void liveAgentStillAcceptsLeaderAndResends() throws Exception {
		try (var f = new Fixture("liveLeader")) {
			assertTrue(f.agent.setLeader(f.notification(), f.leader));
			f.resend();
			assertSame(f.leader, f.agent.getLeader());
			assertEquals(2, f.connector.starts);
			assertEquals(1, f.callbacks.get());
		}
	}
}
