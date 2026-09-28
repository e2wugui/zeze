package Zeze.Onz;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.ServiceConf;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static Zeze.Onz.Fnd20GcOnzFastSupport.*;

/**
 * FND24 审视波守卫：redoTimer 轮转游标的起点扫描必须按 RocksDB 的无符号字节序比较
 * （Arrays.compareUnsigned）。快照列表是 commitIndex 迭代器（bytewise/无符号 key 序）
 * 的顺序，若用有符号的 Arrays.compare：首个差异字节跨 0x80 边界时（tid …7F vs …80）
 * 两序不一致，扫描会把无符号序更大的记录判为 ≤ 游标而越过它、绕回头部——预算耗尽的
 * 轮次下该记录每轮轮不到，onz-05 要根治的尾部饥饿原样回归（正确性无损：重发幂等、
 * 删除有复核，纯收敛性缺陷）。
 * <p>
 * 形态：@Fast 自包含（进程内 SM + OnzServer，对齐 Fnd20GcOnzFastSupport 先例，不复用
 * 其桩是因为要记录 Commit 到达顺序——IntSupplier 观测不到 tid）。反射预置 redoResumeKey
 * 后驱动一轮 redoTimer，断言桩参与方收到的 Commit 顺序 = 无符号序的"游标之后→回卷"。
 */
@Fast
public class TestOnzRedoRotationCursor {
	// 857段：与Fnd19-Fnd21系Fast类各自的RocksDB目录/SM端口错开（857/51857），桩参与方51877。
	private static final int ServerId = 857;
	private static final int SmPort = 51857;
	private static final int StubPort = 51877;

	// 同高位、末字节跨0x80边界的三连tid：RocksDB key无符号序 A(…7E) < B(…7F) < C(…80)。
	private static final long TidA = 0x5CA3F0000000007EL;
	private static final long TidB = 0x5CA3F0000000007FL;
	private static final long TidC = 0x5CA3F00000000080L;

	// 桩参与方按到达顺序记录Commit的tid（Direct派发在IO线程，redo逐条发送+等待应答，
	// 到达顺序=记录处理顺序，确定性）。
	private final ConcurrentLinkedQueue<Long> commitArrivals = new ConcurrentLinkedQueue<>();

	private ServiceManagerServer sm;
	private Agent registerAgent;
	private Service stubService;
	private OnzServer onzServer;

	@BeforeEach
	public void before(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys"));
		Fnd19GcOnzTestSupport.deleteRecursively(Path.of("CommitOnzServer" + ServerId));

		sm = new ServiceManagerServer(null, SmPort, new Config(), "autokeys/fnd24-rot-" + ServerId);
		try {
			// 桩参与方先就位再构造OnzServer：其zeze1代理的初始订阅快照即含该"Onz"服务
			//（同步EditService注册，先于OnzServer构造完成，无传播等待窗口——Fnd20先例）。
			stubService = new Service("Fnd24RotStubParticipant", new Config());
			stubService.AddFactoryHandle(Zeze.Builtin.Onz.Commit.TypeId_,
					new Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.Commit::new, r -> {
						commitArrivals.add(r.Argument.getOnzTid());
						r.SendResult(); // 0应答：redo全0→复核删除，本轮完成（游标按语义清空）
						return 0;
					}, Zeze.Transaction.TransactionLevel.None, Zeze.Transaction.DispatchMode.Direct));
			stubService.newServerSocket("127.0.0.1", StubPort, null);
			stubService.start();

			registerAgent = new Agent(new Config());
			registerAgent.getClient().getConfig().addConnector(new Connector("127.0.0.1", SmPort));
			registerAgent.start();
			registerAgent.waitReady();
			registerAgent.registerService(new BServiceInfo("Onz", "857", 0, "127.0.0.1", StubPort));

			// zeze1配置必须是文件（OnzServer构造只收路径）；myConfig可程序化构造。
			var zeze1Xml = tempDir.resolve("fnd24rot-zeze1.xml");
			Files.writeString(zeze1Xml, """
					<?xml version="1.0" encoding="utf-8"?>
					<zeze ServerId="%d">
						<ServiceConf Name="Zeze.Services.ServiceManager.Agent">
							<Connector HostNameOrAddress="127.0.0.1" Port="%d"/>
						</ServiceConf>
					</zeze>
					""".formatted(ServerId + 10, SmPort));
			var myConfig = new Config();
			myConfig.setServerId(ServerId);
			var sc = new ServiceConf();
			sc.addConnector(new Connector("127.0.0.1", SmPort, false));
			myConfig.getServiceConfMap().put(Agent.defaultServiceName, sc);

			onzServer = new OnzServer("zeze1=" + zeze1Xml, myConfig);
			onzServer.start();

			// 参与方地址发现就绪（注册先于OnzServer构造，通常零等待；慢机兜底轮询）。
			var deadline = System.currentTimeMillis() + 30_000;
			for (;;) {
				try {
					onzServer.getZezeInstance("zeze1");
					break;
				} catch (RuntimeException e) {
					Assertions.assertTrue(System.currentTimeMillis() < deadline, "zeze1参与方地址未就绪");
					Thread.sleep(100);
				}
			}
		} catch (Throwable ex) {
			after(); // 半途构造的资源best-effort回收（对齐OnzServer构造失败自身的全有或全无形态）
			throw ex;
		}
	}

	@AfterEach
	public void after() throws Exception {
		if (onzServer != null) {
			try {
				onzServer.stop();
			} catch (Exception e) {
			}
		}
		if (registerAgent != null) {
			try {
				registerAgent.stop();
			} catch (Exception e) {
			}
		}
		if (stubService != null) {
			try {
				stubService.stop();
			} catch (Exception e) {
			}
		}
		if (sm != null) {
			try {
				sm.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 游标预置为 B(…7F)（模拟上一轮预算耗尽于该记录）：本轮必须从无符号序第一个大于
	 * 游标的 C(…80) 开始，回卷后 A(…7E)、B(…7F)。有符号比较（Arrays.compare）把 …80
	 * 判小于 …7F，扫描越过 C 绕回头部，顺序变为 A、B、C——…80 被推迟到轮尾，预算耗尽
	 * 轮次下即恒饥饿（红态形态）。
	 */
	@Test
	@Timeout(90)
	public void testRotationCursorScansInUnsignedKeyOrder() throws Exception {
		// 三条eCommitting孤儿（procedure参与方zeze1→桩）：无年龄闸、登记表无记录，全部进快照。
		writeOrphanRecords(onzServer, TidA, AbstractOnz.eCommitting, "zeze1");
		writeOrphanRecords(onzServer, TidB, AbstractOnz.eCommitting, "zeze1");
		writeOrphanRecords(onzServer, TidC, AbstractOnz.eCommitting, "zeze1");

		setRedoResumeKey(onzServer, keyOf(TidB));
		invokeRedoTimer(onzServer);

		Assertions.assertEquals(List.of(TidC, TidA, TidB), List.copyOf(commitArrivals),
				"轮转起点必须按RocksDB无符号key序定位（…80>…7F，从C开始回卷A、B）；"
						+ "有符号比较把…80判小于…7F，扫描绕回头部致A先发——预算耗尽轮次下…80恒饥饿（onz-05回归）");
		Assertions.assertEquals(0, count(tableOf(onzServer, "commitIndex")),
				"全0应答：本轮收敛删除（防形态漂移的辅助断言）");
		Assertions.assertNull(redoResumeKeyOf(onzServer), "预算内走完全部候选：游标清空（下轮从头）");
	}

	/** 反射缝：预置轮转游标（私有实例字段；先例见Fnd19GcOnzTestSupport对私有线程字段的直取）。 */
	private static void setRedoResumeKey(OnzServer onzServer, byte[] key) throws Exception {
		var f = redoResumeKeyField();
		f.set(onzServer, key);
	}

	private static Object redoResumeKeyOf(OnzServer onzServer) throws Exception {
		return redoResumeKeyField().get(onzServer);
	}

	private static Field redoResumeKeyField() throws NoSuchFieldException {
		var f = OnzServer.class.getDeclaredField("redoResumeKey");
		f.setAccessible(true);
		return f;
	}
}
