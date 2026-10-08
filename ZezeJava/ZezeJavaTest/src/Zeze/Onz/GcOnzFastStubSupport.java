package Zeze.Onz;

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
						int port, Consumer<Service> beforeListen) throws Exception {
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
		final Consumer<Service> beforeListen;

		StubSpec(String smServiceName, String identity, int port, Consumer<Service> beforeListen) {
			this.smServiceName = smServiceName;
			this.identity = identity;
			this.port = port;
			this.beforeListen = beforeListen;
		}
	}

	/** 协调者夹具：close逆序释放（onzServer→桩→SM），best-effort逐项容错。 */
	static final class OnzFixture implements AutoCloseable {
		final ServiceManagerServer sm;
		final OnzServer onzServer;
		final StubParticipant[] stubs;

		OnzFixture(ServiceManagerServer sm, OnzServer onzServer, StubParticipant[] stubs) {
			this.sm = sm;
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
	static OnzFixture startSharedOnzServer(int serverId, int smPort, Path tempDir, int sharedConfigServerId,
										   String specialZezeNames, StubSpec... stubSpecs) throws Exception {
		var clusterXml = writeClusterXml(tempDir.resolve("fnd21gc-shared.xml"), sharedConfigServerId, smPort);
		return buildCoordinator(serverId, smPort, true, clusterXml, specialZezeNames, stubSpecs);
	}

	/** 独立SM协调者（OnzServer两参构造器，C02用；zezes串"名=配置"形态，单集群指向进程内SM）。 */
	static OnzFixture startNonSharedOnzServer(int serverId, int smPort, Path tempDir, String clusterName,
											  int clusterConfigServerId, StubSpec... stubSpecs) throws Exception {
		var clusterXml = writeClusterXml(tempDir.resolve("fnd21gc-cluster.xml"), clusterConfigServerId, smPort);
		return buildCoordinator(serverId, smPort, false, clusterXml, clusterName + "=" + clusterXml, stubSpecs);
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

		var sm = new ServiceManagerServer(null, smPort, new Config(), "autokeys/fnd21-gc-" + serverId);
		var stubs = new StubParticipant[stubSpecs.length];
		try {
			for (int i = 0; i < stubSpecs.length; i++) {
				var spec = stubSpecs[i];
				stubs[i] = new StubParticipant(smPort, spec.smServiceName, spec.identity,
						"fnd21gc-stub-" + spec.smServiceName, spec.port, spec.beforeListen);
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
			return new OnzFixture(sm, onzServer, stubs);
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
