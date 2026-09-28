package Zeze.Services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Services.HandshakeClient;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManager.EditService;
import Zeze.Services.ServiceManager.Subscribe;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND16 svc-01 红绿钉板（非raft版）：SM EditService/Subscribe 入口上限——本端口与
 * AllocateId（FND15 svc-01）同面无认证，serviceName/identity 此前零校验零上限，
 * serviceStates 行壳零 remove（S1 复核升级确认）。
 * EditService 实为 Rpc（服务端 SendResult）：拒绝形态=错误码应答（ErrorRequestId），
 * 客户端注册应答工厂后 SendForWait 断言。
 * 用例3（空壳逐出）走单元级反射（全量1024次注册的行为级钉板成本不成比例）。
 * raft 侧（字段校验/每会话/唯一名空壳扫除/SetServerLoad requireSession+get化）由
 * TestServiceManagerWithRaft* 全家回归覆盖合法路径+机制核查。
 */
@Fast
public class TestFnd16Svc01SmEditSubscribeLimits {
	// 固定端口契约见TestTakeoverIdentifySuspect；26114避开26110-26113。
	private static final int PORT = 26114;
	private static ServiceManagerServer sm;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys"));
		sm = new ServiceManagerServer(null, PORT, new Zeze.Config(), "autokeys/fnd16-svc01b");
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (sm != null)
			sm.stop();
	}

	private static final class Client extends HandshakeClient {
		Client(String name) {
			super(name, new Zeze.Config());
			// 应答工厂必须注册（不注册会被UnknownProtocol关闭连接，族内Raft测试同款经验）。
			AddFactoryHandle(EditService.TypeId_, new ProtocolFactoryHandle<>(
					EditService::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Subscribe.TypeId_, new ProtocolFactoryHandle<>(
					Subscribe::new, null, TransactionLevel.None, DispatchMode.Direct));
		}

		AsyncSocket connect() throws Exception {
			for (int attempt = 1; ; ++attempt) {
				var connector = new Connector("127.0.0.1", PORT, false);
				getConfig().addConnector(connector);
				start();
				try {
					return connector.WaitReady();
				} catch (Exception e) {
					if (!(e instanceof java.util.concurrent.TimeoutException) || attempt >= 6)
						throw e;
					getConfig().removeConnector(connector);
					connector.stop();
					//noinspection BusyWait
					Thread.sleep(200);
				}
			}
		}
	}

	private static BServiceInfo info(String serviceName, String identity) {
		return new BServiceInfo(serviceName, identity, 1, "127.0.0.1", 1005);
	}

	/** EditService实为Rpc：发送并断言resultCode。 */
	private static long editOf(AsyncSocket sock, BEditService arg) throws Exception {
		var rpc = new EditService(arg);
		Assertions.assertTrue(rpc.SendForWait(sock, 10_000).await(10_000), "edit await");
		Assertions.assertFalse(rpc.isTimeout(), "edit timeout");
		return rpc.getResultCode();
	}

	@Test
	@Timeout(120)
	public void testFieldSizeRejected() throws Exception {
		var reg = new Client("UnitTest.Fnd16Svc01.Reg");
		try {
			var sock = reg.connect();

			// 基线：合法注册 rc=0。
			var okEdit = new BEditService();
			okEdit.getAdd().add(info("fnd16svc1.ok", "11"));
			Assertions.assertEquals(0, editOf(sock, okEdit), "合法注册必须成功（基线自证）");

			// 超长serviceName（129B>128B）与超长identity（'@'+200B）必须错误码拒绝。
			var badName = new BEditService();
			badName.getAdd().add(info("x".repeat(129), "12"));
			Assertions.assertEquals(Procedure.ErrorRequestId, editOf(sock, badName),
					"超长serviceName必须拒绝");

			var badIdentity = new BEditService();
			badIdentity.getAdd().add(info("fnd16svc1.bad", "@" + "y".repeat(200)));
			Assertions.assertEquals(Procedure.ErrorRequestId, editOf(sock, badIdentity),
					"超长identity必须拒绝");

			// 超大批量（>128/批）拒绝。
			var bigBatch = new BEditService();
			for (var i = 0; i < 129; i++)
				bigBatch.getAdd().add(info("fnd16svc1-big-" + i, Integer.toString(3000 + i)));
			Assertions.assertEquals(Procedure.ErrorRequestId, editOf(sock, bigBatch),
					"超大批量必须拒绝");

			// 拒绝后服务端零状态变更：合法通道仍可用。
			var again = new BEditService();
			again.getAdd().add(info("fnd16svc1.after-reject", "13"));
			Assertions.assertEquals(0, editOf(sock, again), "拒绝后合法注册仍可用");
		} finally {
			reg.stop();
		}
	}

	@Test
	@Timeout(120)
	public void testPerSessionRegistersLimit() throws Exception {
		var reg = new Client("UnitTest.Fnd16Svc01.RegL");
		try {
			var sock = reg.connect();
			// 每会话上限64：64名全部成功（覆盖式重注册不占名额自证一次），第65名拒绝。
			for (var base = 0; base < 64; base += 32) {
				var edit = new BEditService();
				for (var i = base; i < base + 32; i++)
					edit.getAdd().add(info("fnd16svc1-cap-" + i, Integer.toString(2000 + i)));
				Assertions.assertEquals(0, editOf(sock, edit), "限额内注册必须成功");
			}
			// 覆盖式重注册（同名同identity）不占新名额：重复第1名仍成功。
			var overwrite = new BEditService();
			overwrite.getAdd().add(info("fnd16svc1-cap-0", "2000"));
			Assertions.assertEquals(0, editOf(sock, overwrite), "覆盖式重注册不受限");

			var overflow = new BEditService();
			overflow.getAdd().add(info("fnd16svc1-cap-overflow", "2999"));
			Assertions.assertEquals(Procedure.ErrorRequestId, editOf(sock, overflow),
					"第65个唯一名必须被每会话上限拒绝");
		} finally {
			reg.stop();
		}
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, ServiceManagerServer.ServiceState> serviceStates() throws Exception {
		var f = ServiceManagerServer.class.getDeclaredField("serviceStates");
		f.setAccessible(true);
		return (ConcurrentHashMap<String, ServiceManagerServer.ServiceState>)f.get(sm);
	}

	@Test
	@Timeout(60)
	public void testEvictIdleServiceStateUnit() throws Exception {
		var states = serviceStates();
		try {
			states.clear();
			// 3个空壳+1个非空壳（含注册）+1个目标名占位。
			for (var i = 0; i < 3; i++)
				states.put("fnd16svc1-idle-" + i, new ServiceManagerServer.ServiceState(sm, "fnd16svc1-idle-" + i));
			states.put("fnd16svc1-live", new ServiceManagerServer.ServiceState(sm, "fnd16svc1-live"));
			states.get("fnd16svc1-live").getServiceInfos().put(1L, new HashMap<>()); // 非空壳：有注册
			states.put("fnd16svc1-target", new ServiceManagerServer.ServiceState(sm, "fnd16svc1-target"));

			var evict = ServiceManagerServer.class.getDeclaredMethod("evictIdleServiceState", String.class);
			evict.setAccessible(true);

			// 逐出：排除目标名，只能逐空壳；返回true且恰移除一个。
			Assertions.assertTrue((boolean)evict.invoke(sm, "fnd16svc1-target"), "必须逐出空壳成功");
			Assertions.assertEquals(4, states.size(), "恰移除一个空壳");
			Assertions.assertTrue(states.containsKey("fnd16svc1-live"), "非空壳不得被逐");
			Assertions.assertTrue(states.containsKey("fnd16svc1-target"), "目标名不得被逐（本轮要使用）");

			// 清空剩余空壳后：无可逐返回false（满员拒绝路径）。
			states.keySet().removeIf(k -> k.startsWith("fnd16svc1-idle-"));
			Assertions.assertFalse((boolean)evict.invoke(sm, "fnd16svc1-target"), "无空壳可逐时返回false（拒绝路径）");
			Assertions.assertEquals(2, states.size());
		} finally {
			states.clear(); // 单元级直填的测试数据清场
		}
	}
}
