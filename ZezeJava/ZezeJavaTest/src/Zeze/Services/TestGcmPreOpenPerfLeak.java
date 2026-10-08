package Zeze.Services;

import javax.xml.parsers.DocumentBuilderFactory;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Services.GlobalCacheManagerAsyncServer;
import Zeze.Services.GlobalCacheManagerServer;
import Zeze.Util.Task;

/**
 * FND17 svc-01回归：GCM同步/异步版start()的pre-open段（Config.parseCustomize /
 * new ServerService / AddFactoryHandle，open=true之前）抛出时，stop()以!open早退，
 * 先行创建的GlobalCacheManagerPerf的1s周期报告任务永久泄漏（唯一取消点close()不可达）。
 * 修复后perf创建移到open之后（try区内，失败路径既有stop()收尾覆盖）。
 * 构造性场景：向config.customizes注入带非法整型属性的GlobalCacheManager元素，
 * parseCustomize的Integer.parseInt抛NumberFormatException（反射观察泄漏态）——
 * 修复前本用例红（perf残留非null），修复后绿（perf为null）。
 */
@Fast
public class TestGcmPreOpenPerfLeak {
	private static final int PORT_SYNC = 19741; // @Fast固定端口独占
	private static final int PORT_ASYNC = 19742;

	@Test
	public final void testSyncPreOpenFailureNoPerfLeak() throws Exception {
		Task.tryInitThreadPool();
		var ctor = GlobalCacheManagerServer.class.getDeclaredConstructor(); // 单例类，私有构造
		ctor.setAccessible(true);
		var gcm = ctor.newInstance();
		Assertions.assertThrows(NumberFormatException.class, () -> gcm.start(null, PORT_SYNC, badConfig()));
		Assertions.assertNull(perfOf(gcm), "pre-open失败不得泄漏perf定时器（修复前非null，本用例红）");
		Assertions.assertFalse(openOf(gcm), "pre-open失败后open仍为false");
	}

	@Test
	public final void testAsyncPreOpenFailureNoPerfLeak() throws Exception {
		Task.tryInitThreadPool();
		var gcm = new GlobalCacheManagerAsyncServer();
		Assertions.assertThrows(NumberFormatException.class, () -> gcm.start(null, PORT_ASYNC, badConfig()));
		Assertions.assertNull(perfOf(gcm), "pre-open失败不得泄漏perf定时器（修复前非null，本用例红）");
		Assertions.assertFalse(openOf(gcm), "pre-open失败后open仍为false");
	}

	/** GlobalCacheManager定制节带非法整型属性：parseCustomize内Integer.parseInt必抛。 */
	private static Zeze.Config badConfig() throws Exception {
		var conf = new Zeze.Config();
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var e = doc.createElement("GlobalCacheManager");
		e.setAttribute("InitialCapacity", "not-a-number");
		conf.getCustomizes().put("GlobalCacheManager", e);
		return conf;
	}

	private static Object perfOf(Object gcm) throws Exception {
		var field = gcm.getClass().getDeclaredField("perf");
		field.setAccessible(true);
		return field.get(gcm);
	}

	private static boolean openOf(Object gcm) throws Exception {
		var field = gcm.getClass().getDeclaredField("open");
		field.setAccessible(true);
		return field.getBoolean(gcm);
	}
}
