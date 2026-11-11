package Zeze.Onz;

import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.ServiceConf;
import Zeze.Onz.OnzServer;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Task;

/**
 * FND21 Gc系Onz @Fast自包含脚手架（TestFnd21GcC01/C02）：进程内ServiceManagerServer +
 * 桩参与方（Net.Service监听并以指定服务名向SM注册——模拟各集群Onz.start()的注册侧，
 * GC-C01(FND21)后共享SM部署按集群唯一名注册）+ 临时配置文件构造协调者OnzServer
 * （C01走三参共享SM构造器、C02走两参独立SM构造器），不依赖外部SM/GCM进程与demo.App
 * 集群（对齐Fnd20GcOnzFastSupport先例）。serverId用890-893段、端口31890-51894段——
 * @Fast类并行时与其他测试类的本地目录/端口互不冲突。
 */
final class GcOnzFastStubSupport {
	private GcOnzFastStubSupport() {
	}

	/** 桩参与方：Net.Service监听port并以smServiceName向SM注册（Onz.start()注册侧同型）。 */
	static final class StubParticipant implements AutoCloseable {
		final Service service;
		final Agent agent;

		StubParticipant(int smPort, String smServiceName, String identity, String listenName,
						int port, @Nullable Consumer<Service> beforeListen) throws Exception {
			service = new Service(listenName, new Config());
			if (beforeListen != null)
				beforeListen.accept(service); // 协议工厂先于socket监听注册
			service.newServerSocket("127.0.0.1", port, null);
			service.start();
			agent = new Agent(new Config());
			agent.getClient().getConfig().addConnector(new Connector("127.0.0.1", smPort));
			agent.start();
			agent.waitReady();
			agent.registerService(new BServiceInfo(smServiceName, identity, 0, "127.0.0.1", port));
		}

		@Override
		public void close() {
			try {
				agent.stop();
			} catch (Exception ignore) {
			}
			try {
				service.stop();
			} catch (Exception ignore) {
			}
		}
	}

	/** 桩参与方描述：smServiceName（SM注册名，共享模式=集群唯一名）+ 监听端口 + 可选协议工厂装配。 */
	static final class StubSpec {
		final String smServiceName;
		final String identity;
		final int port;
		final @Nullable Consumer<Service> beforeListen;

		StubSpec(String smServiceName, String identity, int port, @Nullable Consumer<Service> beforeListen) {
			this.smServiceName = smServiceName;
			this.identity = identity;
			this.port = port;
			this.beforeListen = beforeListen;
		}
	}

	/** 协调者夹具：close逆序释放（onzServer→桩→SM），best-effort逐项容错。 */
	static final class OnzFixture implements AutoCloseable {
		final ServiceManagerServer sm;
		/** 进程内SM的实际端口（带外选口定值）：夹具建立后须再向SM注册额外参与方时使用
		 * （TestRedoFailAgedSettle自愈用例的Commit桩晚注册上线）。 */
		final int smPort;
		final OnzServer onzServer;
		final StubParticipant[] stubs;

		OnzFixture(ServiceManagerServer sm, int smPort, OnzServer onzServer, StubParticipant[] stubs) {
			this.sm = sm;
			this.smPort = smPort;
			this.onzServer = onzServer;
			this.stubs = stubs;
		}

		@Override
		public void close() {
			try {
				onzServer.stop();
			} catch (Exception ignore) {
				// best-effort：与OnzServer.stop自身的分步容错口径一致
			}
			for (var stub : stubs) {
				try {
					stub.close();
				} catch (Exception ignore) {
				}
			}
			try {
				sm.close();
			} catch (Exception ignore) {
			}
		}
	}

	/**
	 * 共享SM协调者（OnzServer三参构造器，GC-C01(FND21)的修复路径）：各桩以集群唯一名注册
	 * 进同一SM，specialZezeNames逐名与之配对——协调者构造时逐名订阅、getZezeInstance按名解析。
	 */
	static OnzFixture startSharedOnzServer(int serverId, Path tempDir, int sharedConfigServerId,
										   String specialZezeNames, StubSpec... stubSpecs) throws Exception {
		var smPort = allocateSmPort();
		var clusterXml = writeClusterXml(tempDir.resolve("gcstub-shared.xml"), sharedConfigServerId, smPort);
		return buildCoordinator(serverId, smPort, true, clusterXml, specialZezeNames, stubSpecs);
	}

	/** 独立SM协调者（OnzServer两参构造器，C02用；zezes串"名=配置"形态，单集群指向进程内SM）。 */
	static OnzFixture startNonSharedOnzServer(int serverId, Path tempDir, String clusterName,
											  int clusterConfigServerId, StubSpec... stubSpecs) throws Exception {
		var smPort = allocateSmPort();
		var clusterXml = writeClusterXml(tempDir.resolve("gcstub-cluster.xml"), clusterConfigServerId, smPort);
		return buildCoordinator(serverId, smPort, false, clusterXml, clusterName + "=" + clusterXml, stubSpecs);
	}

	/**
	 * SM端口带外发号（SM UDP端口族第四现身点的根治，2026-11-11 soak-20261111-1513 @Dbh2批再现
	 * 即实施——2026-11-08 @MySQL批第三现身点留下的修法候选）：调用方原以ServerSocket(0)预留
	 * 临时口再关闭，SM构造期TCP+UDP同号重绑——OS把同号UDP分给并行出站连接即BindException
	 * （异常早于任何DB路径，与批次后端零关联）。带外手工带31870-31889（避开本族固定口
	 * 31850-31898各值与姊妹probeConflictPort的31930-31960），**静态递增发号而非探测**：
	 * probe释放到SM真绑之间隔着构造器内的RocksDB.open（百ms窗），@Fast并行类同带竞探
	 * 会互抢（首版实锤bind 31870）；发号同JVM永不复用，跨方法重绑（Service.stop解绑异步+
	 * TIME_WAIT窗）也无从发生。带宽20，超出即本带规划被打破、fail-fast报错。
	 */
	private static final java.util.concurrent.atomic.AtomicInteger nextSmPort = new java.util.concurrent.atomic.AtomicInteger(31870);

	private static int allocateSmPort() {
		var port = nextSmPort.getAndIncrement();
		if (port > 31889)
			throw new IllegalStateException("SM port band 31870-31889 exhausted (concurrent fixtures beyond plan?)");
		return port;
	}

	private static Path writeClusterXml(Path path, int serverId, int smPort) throws Exception {
		// 集群/共享配置必须是文件（OnzServer构造只收路径）。
		Files.writeString(path, """
				<?xml version="1.0" encoding="utf-8"?>
				<zeze ServerId="%d">
					<ServiceConf Name="Zeze.Services.ServiceManager.Agent">
						<Connector HostNameOrAddress="127.0.0.1" Port="%d"/>
					</ServiceConf>
				</zeze>
				""".formatted(serverId, smPort));
		return path;
	}

	/** SM→桩（先就位，协调者订阅快照无传播等待窗口）→协调者；半途失败best-effort回滚。 */
	private static OnzFixture buildCoordinator(int serverId, int smPort, boolean shared,
											   Path clusterXml, String zezes, StubSpec... stubSpecs) throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys"));
		GcOnzE2eTestSupport.deleteRecursively(Path.of("CommitOnzServer" + serverId));

		var sm = new ServiceManagerServer(null, smPort, new Config(), "autokeys/gcstub-" + serverId);
		var stubs = new StubParticipant[stubSpecs.length];
		try {
			for (int i = 0; i < stubSpecs.length; i++) {
				var spec = stubSpecs[i];
				stubs[i] = new StubParticipant(smPort, spec.smServiceName, spec.identity,
						"gcstub-participant-" + spec.smServiceName, spec.port, spec.beforeListen);
			}

			var myConfig = new Config();
			myConfig.setServerId(serverId);
			var sc = new ServiceConf();
			sc.addConnector(new Connector("127.0.0.1", smPort, false));
			myConfig.getServiceConfMap().put(Agent.defaultServiceName, sc);

			var onzServer = shared
					? new OnzServer(clusterXml.toString(), zezes, myConfig)
					: new OnzServer(zezes, myConfig);
			onzServer.start();
			return new OnzFixture(sm, smPort, onzServer, stubs);
		} catch (Throwable ex) {
			// 半途构造的资源best-effort回滚（对齐OnzServer构造失败自身的全有或全无形态）
			for (var stub : stubs)
				if (stub != null)
					stub.close();
			sm.close();
			throw ex;
		}
	}
}
