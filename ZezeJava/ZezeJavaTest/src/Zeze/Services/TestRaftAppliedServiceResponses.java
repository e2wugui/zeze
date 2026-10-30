package Zeze.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import Zeze.Config;
import Zeze.Raft.RaftRpc;
import Zeze.Serialize.Serializable;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Procedure;
import Zeze.Util.ConcurrentHashMapOrdered;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRaftAppliedServiceResponses {
	private static final class Fixture implements AutoCloseable {
		final Path xml;
		final ServiceManagerAgentWithRaft agent;
		final Zeze.Raft.Agent transport;

		Fixture() throws Exception {
			Task.tryInitThreadPool();
			xml = Files.createTempFile("applied-service-response", ".xml");
			Files.writeString(xml, """
					<raft Name="applied-service-response">
						<node Host="127.0.0.1" Port="19623"/>
						<node Host="127.0.0.1" Port="19624"/>
					</raft>
					""");
			var config = Config.load();
			assertNotNull(config.getServiceManagerConf());
			config.getServiceManagerConf().setRaftXml(xml.toString());
			config.getServiceManagerConf().setSessionName("applied-service-response");
			agent = new ServiceManagerAgentWithRaft(config);
			var field = ServiceManagerAgentWithRaft.class.getDeclaredField("raftClient");
			field.setAccessible(true);
			transport = (Zeze.Raft.Agent)field.get(agent);
			// 未启动网络：由测试交付单次应答，关闭自动重发驱动以固定完成时序。
			field = Zeze.Raft.Agent.class.getDeclaredField("resendTask");
			field.setAccessible(true);
			((Future<?>)field.get(transport)).cancel(false);
		}

		@SuppressWarnings("unchecked")
		TaskCompletionSource<Boolean> login() throws Exception {
			var field = ServiceManagerAgentWithRaft.class.getDeclaredField("loginFuture");
			field.setAccessible(true);
			return (TaskCompletionSource<Boolean>)field.get(agent);
		}

		@SuppressWarnings("unchecked")
		<T extends RaftRpc<?, ?>> T pending(Class<T> type) throws Exception {
			var field = Zeze.Raft.Agent.class.getDeclaredField("pending");
			field.setAccessible(true);
			for (var request : (ConcurrentHashMapOrdered<Long, RaftRpc<?, ?>>)field.get(transport))
				if (type.isInstance(request))
					return type.cast(request);
			throw new AssertionError("missing request: " + type);
		}

		@Override
		public void close() throws Exception {
			try {
				agent.close();
			} finally {
				Files.deleteIfExists(xml);
			}
		}
	}

	private static <A extends Serializable, R extends Serializable> void respond(
			RaftRpc<A, R> request, RaftRpc<A, R> response, long code) throws Exception {
		response.setRequest(false);
		response.setResultCode(code);
		assertNotNull(request.getResponseHandle());
		request.getResponseHandle().handle(response); // 真实Raft.Agent.sendHandle保留并交付结果码。
		assertEquals(code, request.getResultCode());
	}

	@Test
	void appliedLoginCompletesReadiness() throws Exception {
		try (var f = new Fixture()) {
			assertNotNull(f.transport.getOnSetLeader());
			f.transport.getOnSetLeader().run(f.transport);
			var request = f.pending(Zeze.Builtin.ServiceManagerWithRaft.Login.class);
			respond(request, new Zeze.Builtin.ServiceManagerWithRaft.Login(), Procedure.RaftApplied);
			assertEquals(Boolean.TRUE, f.login().getNow(), "已提交登录的去重应答必须解除登录等待");
		}
	}

	@Test
	void appliedAllocationDeliversOriginalGrant() throws Exception {
		try (var f = new Fixture()) {
			var future = f.agent.allocateTidCacheFuture("grant");
			var request = f.pending(Zeze.Builtin.ServiceManagerWithRaft.AllocateId.class);
			var response = new Zeze.Builtin.ServiceManagerWithRaft.AllocateId();
			response.Result.setStartId(123);
			response.Result.setCount(16);
			respond(request, response, Procedure.RaftApplied);
			assertTrue(future.isDone());
			assertEquals(123, future.getNonNull().next());
		}
	}

	@Test
	void appliedSubscriptionPublishesOriginalSnapshot() throws Exception {
		try (var f = new Fixture()) {
			f.login().setResult(true);
			var arg = new BSubscribeArgument();
			arg.subs.add(new BSubscribeInfo("service", 7));
			var future = f.agent.subscribeServicesAsync(arg);
			var request = f.pending(Zeze.Builtin.ServiceManagerWithRaft.Subscribe.class);
			var response = new Zeze.Builtin.ServiceManagerWithRaft.Subscribe();
			var infos = new BServiceInfosVersion();
			infos.getOrAddInfos(7).insert(new BServiceInfo("service", "1", 7));
			response.Result.map.put("service", infos);
			respond(request, response, Procedure.RaftApplied);
			assertTrue(future.isDone());
			var state = future.join().getFirst();
			assertSame(state, f.agent.getSubscribeStates().get("service"));
			assertNotNull(state.getServiceInfos(7));
			assertNotNull(state.findServiceInfoByIdentity("1"));
		}
	}

	@Test
	void rejectedAllocationDoesNotDeliverResultPayload() throws Exception {
		try (var f = new Fixture()) {
			var future = f.agent.allocateTidCacheFuture("grant");
			var request = f.pending(Zeze.Builtin.ServiceManagerWithRaft.AllocateId.class);
			var response = new Zeze.Builtin.ServiceManagerWithRaft.AllocateId();
			response.Result.setStartId(123);
			response.Result.setCount(16);
			respond(request, response, Procedure.ErrorRequestId);
			assertTrue(future.isCompletedExceptionally());
		}
	}

	@Test
	void rejectedSubscriptionDoesNotPublishResultPayload() throws Exception {
		try (var f = new Fixture()) {
			f.login().setResult(true);
			var arg = new BSubscribeArgument();
			arg.subs.add(new BSubscribeInfo("service", 7));
			var future = f.agent.subscribeServicesAsync(arg);
			var request = f.pending(Zeze.Builtin.ServiceManagerWithRaft.Subscribe.class);
			var response = new Zeze.Builtin.ServiceManagerWithRaft.Subscribe();
			response.Result.map.put("service", new BServiceInfosVersion());
			respond(request, response, Procedure.ErrorRequestId);
			assertThrows(CompletionException.class, future::join);
			assertTrue(f.agent.getSubscribeStates().isEmpty());
		}
	}
}
