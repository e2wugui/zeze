package Zeze.Services.ServiceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import Zeze.Component.Threading;
import Zeze.Config;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Util.Task;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestServiceReplay {
	private static final class TestAgent extends AbstractAgent {
		final ServiceReplay replay = new ServiceReplay(this::register, "TestServiceReplay");
		final List<String> calls = Collections.synchronizedList(new ArrayList<>());
		CompletableFuture<List<SubscribeState>> response = new CompletableFuture<>();
		boolean failRegister;
		boolean failSubscribe;

		TestAgent() {
			Task.tryInitThreadPool();
			config = new Config();
			subscribeStates.put("service", new SubscribeState(new BSubscribeInfo("service", 0)));
		}

		void reconnect() {
			replay.run();
		}

		private void register() {
			calls.add("register");
			if (failRegister)
				throw new IllegalStateException("register");
		}

		@Nullable Future<?> retryTask() throws Exception {
			var field = ServiceReplay.class.getDeclaredField("retryTask");
			field.setAccessible(true);
			return (Future<?>)field.get(replay);
		}

		@Override
		public CompletableFuture<List<SubscribeState>> subscribeServicesAsync(BSubscribeArgument arg) {
			calls.add("subscribe");
			assertEquals("service", arg.subs.get(0).getServiceName());
			if (failSubscribe)
				throw new IllegalStateException("subscribe");
			return response;
		}

		@Override public void close() { replay.close(); }
		@Override public void start() { }
		@Override public void waitReady() { }
		@Override protected void allocate(AutoKey key, int pool) { throw new UnsupportedOperationException(); }
		@Override protected boolean allocateAsync(String name, int count,
				ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
			throw new UnsupportedOperationException();
		}
		@Override public void editService(BEditService arg) { throw new UnsupportedOperationException(); }
		@Override public void unSubscribeService(BUnSubscribeArgument arg) { throw new UnsupportedOperationException(); }
		@Override public boolean setServerLoad(BServerLoad load) { throw new UnsupportedOperationException(); }
		@Override public @Nullable Threading getThreading() { return null; }
	}

	@Test
	void registrationMustSucceedBeforeSubscriptions() throws Exception {
		try (var agent = new TestAgent()) {
			agent.failRegister = true;
			agent.reconnect();
			assertEquals(List.of("register"), agent.calls);
			assertNotNull(agent.retryTask());
		}
	}

	@Test
	void synchronousAndAsynchronousSubscriptionFailuresBothRetry() throws Exception {
		try (var agent = new TestAgent()) {
			agent.failSubscribe = true;
			agent.reconnect();
			assertEquals(List.of("register", "subscribe"), agent.calls);
			assertNotNull(agent.retryTask());
		}
		try (var agent = new TestAgent()) {
			agent.reconnect();
			assertNull(agent.retryTask());
			agent.response.completeExceptionally(new IllegalStateException("async subscribe"));
			assertNotNull(agent.retryTask());
		}
	}

	@Test
	void retryClearsItselfBeforeReplayingAndCloseRejectsLateFailures() throws Exception {
		try (var agent = new TestAgent()) {
			agent.failRegister = true;
			agent.reconnect();
			var first = agent.retryTask();
			assertNotNull(first);
			agent.reconnect();
			assertSame(first, agent.retryTask());
			((Runnable)first).run(); // 直接执行真实延迟任务，验证失败可以登记下一次重试。
			var second = agent.retryTask();
			assertNotNull(second);
			assertNotSame(first, second);
			agent.close();
			assertTrue(second.isCancelled());
			agent.reconnect();
			assertNull(agent.retryTask());
		}
		try (var agent = new TestAgent()) {
			agent.reconnect();
			agent.close();
			agent.response.completeExceptionally(new IllegalStateException("late subscribe"));
			assertNull(agent.retryTask());
		}
	}

	@Test
	void concurrentFailuresScheduleOneRetry() throws Exception {
		try (var agent = new TestAgent(); var executor = Executors.newFixedThreadPool(8)) {
			agent.failRegister = true;
			var ready = new CountDownLatch(8);
			var go = new CountDownLatch(1);
			var results = new ArrayList<Future<Future<?>>>();
			for (int i = 0; i < 8; i++) {
				results.add(executor.submit(() -> {
					ready.countDown();
					assertTrue(go.await(5, TimeUnit.SECONDS));
					agent.reconnect();
					return agent.retryTask();
				}));
			}
			assertTrue(ready.await(5, TimeUnit.SECONDS));
			go.countDown();
			var first = results.get(0).get(5, TimeUnit.SECONDS);
			assertNotNull(first);
			for (var result : results)
				assertSame(first, result.get(5, TimeUnit.SECONDS));
		}
	}

	@Test
	void editsNormalizeRemovalAndEndpointReplacement() {
		try (var agent = new TestAgent()) {
			var original = new BServiceInfo("service", "1", 0, "127.0.0.1", 1);
			agent.subscribeStates.get("service").onRegister(original);
			var replacement = new BServiceInfo("service", "1", 0, "127.0.0.1", 2);
			var edit = new BEditService();
			edit.getRemove().add(original);
			edit.getRemove().add(new BServiceInfo("unsubscribed", "2"));
			edit.getAdd().add(replacement);
			agent.applyServiceEdit(edit);
			assertTrue(edit.getRemove().isEmpty());
			assertSame(replacement, agent.subscribeStates.get("service").findServiceInfoByIdentity("1"));
			var next = new BServiceInfo("service", "1", 0, "127.0.0.1", 3);
			var changed = new BEditService();
			changed.getAdd().add(next);
			agent.applyServiceEdit(changed);
			assertEquals(List.of(replacement), changed.getRemove());
			assertSame(next, agent.subscribeStates.get("service").findServiceInfoByIdentity("1"));
		}
	}

	@Test
	void emptySubscriptionSetDoesNotSendAndSuspectCannotDowngradeVersion() throws Exception {
		try (var agent = new TestAgent()) {
			agent.subscribeStates.clear();
			agent.reconnect();
			assertEquals(List.of("register"), agent.calls);
			assertNull(agent.retryTask());
			var dead = new ArrayList<Integer>();
			agent.setOnSuspect(dead::add);
			var version = java.util.Objects.requireNonNull(agent.getConfig()).getAppMainVersion();
			agent.notifySuspect(7, version + 1);
			assertTrue(dead.isEmpty());
			agent.notifySuspect(8, version);
			assertEquals(List.of(8), dead);
			agent.setOnSuspect(__ -> { throw new IllegalStateException("callback"); });
			assertDoesNotThrow(() -> agent.notifySuspect(9, version));
		}
	}
}
