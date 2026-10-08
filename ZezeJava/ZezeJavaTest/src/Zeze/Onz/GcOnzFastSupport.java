package Zeze.Onz;

import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.ServiceConf;
import Zeze.Onz.OnzServer;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/**
 * FND20 Gc系Onz @Fast自包含脚手架（TestFnd20GcC01-C03）：进程内ServiceManagerServer +
 * 程序化myConfig + 临时zeze1配置文件构造协调者OnzServer，不依赖外部SM/GCM进程与
 * demo.App集群（对齐TestLateProtocolAfterClose的进程内SM先例）。serverId用850段、
 * 端口用518xx段——@Fast类并行时与其他测试类的本地目录/端口互不冲突。
 * 索引/点表直注（writeIndexOnly形态，TestFnd19GcC02示范的合法操作）与反射驱动
 * redoTimer复用Fnd19GcOnzTestSupport的既有助手。
 */
final class GcOnzFastSupport {
	private GcOnzFastSupport() {
	}

	/** 每测试类一个实例（serverId/smPort错开）：close逆序释放，best-effort逐项容错。 */
	static final class FastFixture implements AutoCloseable {
		final ServiceManagerServer sm;
		final OnzServer onzServer;
		/** GC-C03专用：向SM注册"Onz"服务的桩参与方代理（null=无桩）。 */
		final Agent registerAgent;
		/** GC-C03专用：应答Commit的桩参与方服务（null=无桩）。 */
		final Service stubService;

		FastFixture(ServiceManagerServer sm, OnzServer onzServer, Agent registerAgent, Service stubService) {
			this.sm = sm;
			this.onzServer = onzServer;
			this.registerAgent = registerAgent;
			this.stubService = stubService;
		}

		@Override
		public void close() {
			try {
				onzServer.stop();
			} catch (Exception e) {
				// best-effort：与OnzServer.stop自身的分步容错口径一致
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
			try {
				sm.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 构造并启动协调者OnzServer（zeze1=进程内SM支撑的临时配置）。
	 * stubPort>0时额外起一个桩参与方：Net.Service监听stubPort并向SM注册"Onz"服务
	 * （参与方地址发现走getZezeInstance的真实路径），Commit请求按stubCommitRc应答
	 * （非0→框架回发该结果码；0→显式SendResult）。
	 */
	static FastFixture startOnzServer(int serverId, int smPort, Path tempDir,
									  int stubPort, java.util.function.IntSupplier stubCommitRc) throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys"));
		GcOnzE2eTestSupport.deleteRecursively(Path.of("CommitOnzServer" + serverId));

		var sm = new ServiceManagerServer(null, smPort, new Config(), "autokeys/fnd20-gc-" + serverId);

		Agent registerAgent = null;
		Service stubService = null;
		try {
			// 桩参与方先就位再构造OnzServer：其zeze1代理的初始订阅快照即含该"Onz"服务
			// （同步EditService注册，先于OnzServer构造完成，无传播等待窗口）。
			if (stubPort > 0) {
				stubService = new Service("Fnd20GcStubParticipant", new Config());
				stubService.AddFactoryHandle(Zeze.Builtin.Onz.Commit.TypeId_,
						new Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.Commit::new, r -> {
							var rc = stubCommitRc.getAsInt();
							if (rc == 0)
								r.SendResult(); // 框架仅在非0时回发错误码（TaskSpec契约），0需显式应答
							return rc;
						}, Zeze.Transaction.TransactionLevel.None, Zeze.Transaction.DispatchMode.Direct));
				stubService.newServerSocket("127.0.0.1", stubPort, null);
				stubService.start();

				registerAgent = new Agent(new Config());
				registerAgent.getClient().getConfig().addConnector(new Connector("127.0.0.1", smPort));
				registerAgent.start();
				registerAgent.waitReady();
				registerAgent.registerService(new BServiceInfo("Onz", "853", 0, "127.0.0.1", stubPort));
			}

			// zeze1配置必须是文件（OnzServer构造只收路径）；myConfig可程序化构造。
			var zeze1Xml = tempDir.resolve("fnd20gc-zeze1.xml");
			Files.writeString(zeze1Xml, """
					<?xml version="1.0" encoding="utf-8"?>
					<zeze ServerId="%d">
						<ServiceConf Name="Zeze.Services.ServiceManager.Agent">
							<Connector HostNameOrAddress="127.0.0.1" Port="%d"/>
						</ServiceConf>
					</zeze>
					""".formatted(serverId + 10, smPort));
			var myConfig = new Config();
			myConfig.setServerId(serverId);
			var sc = new ServiceConf();
			sc.addConnector(new Connector("127.0.0.1", smPort, false));
			myConfig.getServiceConfMap().put(Agent.defaultServiceName, sc);

			var onzServer = new OnzServer("zeze1=" + zeze1Xml, myConfig);
			onzServer.start();
			return new FastFixture(sm, onzServer, registerAgent, stubService);
		} catch (Throwable ex) {
			// 半途构造的资源best-effort回滚（对齐OnzServer构造失败自身的全有或全无形态）
			if (registerAgent != null)
				registerAgent.stop();
			if (stubService != null)
				stubService.stop();
			sm.close();
			throw ex;
		}
	}

	/** 手写孤儿决策两表（协调者崩溃残留形态）；onzs为参与方列表（空=redo无网络即收敛删除）。 */
	static void writeOrphanRecords(OnzServer onzServer, long tid, int state, String onzs) throws Exception {
		var key = keyOf(tid);
		var saved = new BSavedCommits.Data();
		if (onzs != null)
			saved.getOnzs().add(onzs);
		var bbState = ByteBuffer.Allocate();
		saved.encode(bbState);
		tableOf(onzServer, "commitPoint").put(key, java.util.Arrays.copyOf(bbState.Bytes, bbState.WriteIndex));
		var bbIndex = ByteBuffer.Allocate();
		bbIndex.WriteUInt(state);
		bbIndex.WriteLong8BE(System.currentTimeMillis() - 121_000); // ePreparing需超RedoPreparingMinAgeMs才被redo
		tableOf(onzServer, "commitIndex").put(key, java.util.Arrays.copyOf(bbIndex.Bytes, bbIndex.WriteIndex));
	}

	/** 只写索引条目（FND19 writeIndexOnly形态的带时戳版）：值原样给定（毒形态注入用）。 */
	static void writeIndexEntry(OnzServer onzServer, byte[] key, byte[] value) throws Exception {
		tableOf(onzServer, "commitIndex").put(key, value);
	}

	/** 合法索引值编码（state varint + 写入时戳），供毒条目构造完好值部分。 */
	static byte[] indexValue(int state, long ageMs) {
		var bbIndex = ByteBuffer.Allocate();
		bbIndex.WriteUInt(state);
		bbIndex.WriteLong8BE(System.currentTimeMillis() - ageMs);
		return java.util.Arrays.copyOf(bbIndex.Bytes, bbIndex.WriteIndex);
	}

	static byte[] keyOf(long tid) {
		var key = new byte[8];
		ByteBuffer.longBeHandler.set(key, 0, tid);
		return key;
	}

	@SuppressWarnings("unchecked")
	static java.util.Set<Long> dedupSet(OnzServer onzServer, String fieldName) throws Exception {
		var f = OnzServer.class.getDeclaredField(fieldName);
		f.setAccessible(true);
		return (java.util.Set<Long>)f.get(onzServer);
	}

	/** 捕获OnzServer的ERROR日志（告警去重断言依据），形态对齐TestFnd19GAD02的CaptureAppender。 */
	static final class CaptureAppender extends AbstractAppender {
		final java.util.List<LogEvent> events = new java.util.concurrent.CopyOnWriteArrayList<>();

		CaptureAppender() {
			super("fnd20gc", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			events.add(event.toImmutable());
		}

		long countErrorContaining(String substring) {
			return events.stream()
					.filter(e -> e.getLevel() == Level.ERROR)
					.filter(e -> e.getMessage().getFormattedMessage().contains(substring))
					.count();
		}
	}

	/** 挂载到OnzServer的logger（测试内finally配detach使用）。 */
	static CaptureAppender attachToOnzServerLogger() {
		var appender = new CaptureAppender();
		appender.start(); // log4j2要求appender启动后才接收事件
		((Logger)org.apache.logging.log4j.LogManager.getLogger(OnzServer.class)).addAppender(appender);
		return appender;
	}

	static void detachFromOnzServerLogger(CaptureAppender appender) {
		((Logger)org.apache.logging.log4j.LogManager.getLogger(OnzServer.class)).removeAppender(appender);
		appender.stop();
	}

	// 以下复用Fnd19GcOnzTestSupport的静态导入语法糖（tableOf/count/invokeRedoTimer/deleteRecursively
	// 为包级静态方法，同类加载域共享；startTwoClusters等e2e样板本组不触碰）。
	static RocksDatabase.Table tableOf(OnzServer onzServer, String fieldName) throws Exception {
		return GcOnzE2eTestSupport.tableOf(onzServer, fieldName);
	}

	static long count(RocksDatabase.Table table) throws Exception {
		return GcOnzE2eTestSupport.count(table);
	}

	static void invokeRedoTimer(OnzServer onzServer) throws Exception {
		GcOnzE2eTestSupport.invokeRedoTimer(onzServer);
	}
}
