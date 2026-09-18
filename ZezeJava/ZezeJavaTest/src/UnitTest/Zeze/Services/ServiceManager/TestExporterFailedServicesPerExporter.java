package UnitTest.Zeze.Services.ServiceManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import sun.misc.Unsafe;

import Zeze.Component.Threading;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.BAllocateIdArgument;
import Zeze.Services.ServiceManager.BAllocateIdResult;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.Exporter;
import Zeze.Services.ServiceManager.IExporter;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-68 回归：failedServices 记账键是"服务名"但成败判定是"某导出器对某服务的一次
 * 导出"——同事件内 E1 失败 add(X)、后位 E2 成功无条件 remove(X)，E1 的补偿记账被洗掉，
 * 进程内永不再补偿（nginx 配置无限期陈旧）。
 * 修复后记账键改为（导出器,服务名）二元组：E1 只记/只清自己的，后续事件自动补偿。
 */
@Fast
public class TestExporterFailedServicesPerExporter {

	/** 仅提供onEdit所需subscribeStates的空壳agent（不启动任何网络/线程）。 */
	private static final class StubAgent extends AbstractAgent {
		@Override
		protected void allocate(AutoKey autoKey, int pool) {
		}

		@Override
		protected boolean allocateAsync(String globalName, int allocCount,
										 ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
			return false;
		}

		@Override
		public void start() {
		}

		@Override
		public void waitReady() {
		}

		@Override
		public void editService(BEditService arg) {
		}

		@Override
		public CompletableFuture<List<SubscribeState>> subscribeServicesAsync(BSubscribeArgument info) {
			return CompletableFuture.completedFuture(List.of());
		}

		@Override
		public void unSubscribeService(BUnSubscribeArgument arg) {
		}

		@Override
		public boolean setServerLoad(BServerLoad load) {
			return false;
		}

		@Override
		public Threading getThreading() {
			return null;
		}

		@Override
		public void close() {
		}
	}

	/** 可控失败的eAll导出器：登记在failServices中的服务下一次导出抛IOException（一次性）。 */
	private static final class RecordingExporter implements IExporter {
		final List<String> exported = new CopyOnWriteArrayList<>();
		final Set<String> failServices = ConcurrentHashMap.newKeySet();

		@Override
		public Type getType() {
			return Type.eAll;
		}

		@Override
		public void exportAll(String serviceName, BServiceInfosVersion all) throws Exception {
			if (failServices.remove(serviceName))
				throw new IOException("simulated export fail: " + serviceName);
			exported.add(serviceName);
		}
	}

	/** 绕过需要Config/Agent的构造器：直接装配agent/exports/failedServices（onEdit只依赖这三者）。 */
	private static Exporter newExporter(AbstractAgent agent, List<IExporter> exports) throws Exception {
		var theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		theUnsafeField.setAccessible(true);
		var exporter = (Exporter)((Unsafe)theUnsafeField.get(null)).allocateInstance(Exporter.class);
		for (var name : new String[]{"agent", "exports", "failedServices"}) {
			var f = Exporter.class.getDeclaredField(name);
			f.setAccessible(true);
			switch (name) {
			case "agent" -> f.set(exporter, agent);
			case "exports" -> f.set(exporter, exports);
			case "failedServices" -> f.set(exporter, new ConcurrentHashMap<>());
			}
		}
		return exporter;
	}

	@Test
	public void testMixedFailureKeepsPerExporterAccounting() throws Exception {
		var agent = new StubAgent();
		agent.getSubscribeStates().put("svcA", new AbstractAgent.SubscribeState(new BSubscribeInfo("svcA", 0)));
		agent.getSubscribeStates().put("svcB", new AbstractAgent.SubscribeState(new BSubscribeInfo("svcB", 0)));
		var e1 = new RecordingExporter(); // 前位（如NginxConfig，本地IO故障域）
		var e2 = new RecordingExporter(); // 后位（如NginxHttp，远端HTTP故障域）
		var exporter = newExporter(agent, List.of(e1, e2));
		var onEdit = Exporter.class.getDeclaredMethod("onEdit", BEditService.class);
		onEdit.setAccessible(true);

		// 事件1：svcA注册；E1失败、E2成功——E1的记账不得被E2的成功洗掉
		e1.failServices.add("svcA");
		var edit1 = new BEditService();
		edit1.getAdd().add(new BServiceInfo("svcA", "id1", 0));
		onEdit.invoke(exporter, edit1);
		Assertions.assertEquals(List.of(), e1.exported, "E1失败：无成功导出");
		Assertions.assertEquals(List.of("svcA"), e2.exported, "E2成功导出svcA");

		// 事件2：仅svcB注册（svcA自身不再变化）——E1必须补偿重导svcA，E2只导svcB
		var edit2 = new BEditService();
		edit2.getAdd().add(new BServiceInfo("svcB", "id2", 0));
		onEdit.invoke(exporter, edit2);
		Assertions.assertEquals(new HashSet<>(List.of("svcA", "svcB")), new HashSet<>(e1.exported),
			"E1补偿重导svcA（修复前：记账被E2洗掉，永不再补偿）");
		Assertions.assertEquals(List.of("svcA", "svcB"), e2.exported,
			"E2不重复补偿自己已成功的svcA");
	}
}
