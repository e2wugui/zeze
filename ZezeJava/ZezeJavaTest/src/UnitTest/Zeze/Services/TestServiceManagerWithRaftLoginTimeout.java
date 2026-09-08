package UnitTest.Zeze.Services;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Config;
import Zeze.Services.ServiceManagerAgentWithRaft;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Util.Task;
import harness.Fast;

/**
 * S1-4：SM-raft Agent waitLoginReady 的 login 超时必须是活超时。
 * <p>
 * await(remaining) 超时仅返回 false 不设置结果；旧实现超时后仍调用 get()，
 * 未完成的 TaskCompletionSource 上 get() 无限期 park，deadline 检查不可达——
 * SM raft 集群不可达（分区/全宕/配置错）时，subscribeService/editService/waitReady
 * 永久阻塞而非在 LoginTimeout 后抛 IllegalStateException("login timeout.")。
 * <p>
 * 只构造 agent 不 start()：不建立任何连接，raftOnSetLeader 永不触发，
 * loginFuture 永不完成，纯本地复现。外层用限时 get 防测试悬挂。
 */
@Fast
public class TestServiceManagerWithRaftLoginTimeout {
	// 端口仅用于构造RaftConfig，未start不监听不连接，选独立区间避免干扰。
	private static final String raftXmlString = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="s1_4_sm_login_timeout">
				<node Host="127.0.0.1" Port="19601"/>
				<node Host="127.0.0.1" Port="19602"/>
			</raft>
			""";
	private static Path raftXmlFile;
	private static ExecutorService workers;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		raftXmlFile = Files.createTempFile("s1_4_sm_raft", ".xml");
		Files.writeString(raftXmlFile, raftXmlString);
		workers = Executors.newCachedThreadPool();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		workers.shutdownNow();
		Files.deleteIfExists(raftXmlFile);
	}

	private static ServiceManagerAgentWithRaft newAgent(long loginTimeout) throws Exception {
		var config = Config.load();
		config.getServiceManagerConf().setRaftXml(raftXmlFile.toString());
		config.getServiceManagerConf().setSessionName("UnitTest.S1_4.LoginTimeout");
		config.getServiceManagerConf().setLoginTimeout(loginTimeout);
		return new ServiceManagerAgentWithRaft(config);
	}

	private static Future<Throwable> subscribeInWorker(ServiceManagerAgentWithRaft agent) {
		return workers.submit(() -> {
			try {
				agent.subscribeService(new BSubscribeInfo("UnitTest.S1_4.Svc"));
				return null;
			} catch (Throwable e) {
				return e;
			}
		});
	}

	@Test
	@Timeout(60)
	public void testLoginTimeoutMustThrowNotHang() throws Exception {
		var agent = newAgent(1000);
		try {
			var f = subscribeInWorker(agent);
			try {
				// begin从submit起算：deadline锚定在worker进入等待（≈submit+ε），脚手架耗时
				// （submit到get之间的调度）不影响elapsed与deadline的相对关系——负载把这段
				// 拖过deadline时，get后起算会elapsed≈0误判"提前失败"。
				long begin = System.currentTimeMillis();
				// 修复前这里永久park，get超时后f.cancel(true)中断线程才勉强返回——
				// 断言ex非空即失败。修复后约1s返回IllegalStateException。
				var ex = f.get(30, TimeUnit.SECONDS);
				long elapsed = System.currentTimeMillis() - begin;
				Assertions.assertNotNull(ex, "subscribeService must fail when SM raft unreachable");
				Assertions.assertInstanceOf(IllegalStateException.class, ex, "expect login timeout, but: " + ex);
				Assertions.assertEquals("login timeout.", ex.getMessage());
				Assertions.assertTrue(elapsed >= 900, "must not fail before loginTimeout, elapsed=" + elapsed);
				// 防挂起护栏（非精确时序）：负载下唤醒/调度延迟可远超1s，放宽到25s，
				// 真挂起由get(30s)与@Timeout(60)双兜底。
				Assertions.assertTrue(elapsed < 25_000, "must fail near loginTimeout(1s), elapsed=" + elapsed);
			} finally {
				f.cancel(true);
			}
		} finally {
			agent.close();
		}
	}

	@Test
	@Timeout(60)
	public void testReplacedFutureKeepWaitingUntilDeadline() throws Exception {
		var agent = newAgent(3000);
		try {
			var f = subscribeInWorker(agent);
			try {
				// begin从submit起算（理由同test1）：deadline锚定在worker进入等待的~3s后，
				// 脚手架（sleep+invoke）耗时与elapsed解耦，负载下脚手架拖过deadline时
				// invoke后起算会elapsed≈0误判"cancel被当超时立即失败"。
				long begin = System.currentTimeMillis();
				// 工作线程进入等待后，模拟raftOnSetLeader里的startNewLogin：
				// cancel旧future并替换。被cancel不是失败，必须重读最新future继续等到deadline。
				Thread.sleep(500);
				Method startNewLogin = ServiceManagerAgentWithRaft.class.getDeclaredMethod("startNewLogin");
				startNewLogin.setAccessible(true);
				startNewLogin.invoke(agent);

				var ex = f.get(30, TimeUnit.SECONDS);
				long elapsed = System.currentTimeMillis() - begin;
				Assertions.assertNotNull(ex, "subscribeService must fail when SM raft unreachable");
				Assertions.assertInstanceOf(IllegalStateException.class, ex, "expect login timeout, but: " + ex);
				Assertions.assertEquals("login timeout.", ex.getMessage());
				// cancel发生在~0.5s，deadline在~3s：立即失败说明cancel被误当超时，违反重读语义。
				Assertions.assertTrue(elapsed >= 2000, "must keep waiting on replaced future until deadline, elapsed=" + elapsed);
				// 防挂起护栏（非精确时序）：负载下唤醒延迟可远超3s，放宽到25s，
				// 真挂起由get(30s)与@Timeout(60)双兜底。
				Assertions.assertTrue(elapsed < 25_000, "must fail near deadline(3s), elapsed=" + elapsed);
			} finally {
				f.cancel(true);
			}
		} finally {
			agent.close();
		}
	}
}
