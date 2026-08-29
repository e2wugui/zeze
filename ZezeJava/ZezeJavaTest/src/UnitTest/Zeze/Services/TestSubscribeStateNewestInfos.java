package UnitTest.Zeze.Services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfos;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import harness.Fast;

/**
 * S-2：BServiceInfosVersion.newestInfos 缓存永不更新。
 * <p>
 * newestInfos 是 final，只在构造时计算一次；订阅后增量推送走 SubscribeState.onRegister/onUnRegister
 * 只修改 infosVersion 桶表，findNewestInfos() 永远返回订阅瞬间的快照。
 * 纯单元测试：直接构造 SubscribeState，不依赖网络。
 */
@Fast
public class TestSubscribeStateNewestInfos {
	private static final String serviceName = "UnitTest.S2.Service";

	private static AbstractAgent.SubscribeState newState() {
		var state = new AbstractAgent.SubscribeState(new BSubscribeInfo(serviceName));
		// 模拟订阅瞬间服务列表为空（服务器端还没有任何注册）。
		state.onFirstCommit(new BServiceInfosVersion(), new BEditService());
		return state;
	}

	@Test
	public void testRegisterAfterSubscribeVisible() {
		var state = newState();
		state.onRegister(new BServiceInfo(serviceName, "77", 1));

		var newest = state.findNewestInfos();
		Assertions.assertNotNull(newest, "post-subscribe register must be visible in findNewestInfos");
		Assertions.assertNotNull(newest.findServiceInfoByIdentity("77"));
	}

	@Test
	public void testUnRegisterAfterSubscribeReflected() {
		var state = newState();
		state.onRegister(new BServiceInfo(serviceName, "77", 1));
		state.onRegister(new BServiceInfo(serviceName, "78", 1));
		state.onUnRegister(new BServiceInfo(serviceName, "77", 1));

		var newest = state.findNewestInfos();
		Assertions.assertNotNull(newest, "post-subscribe register must be visible in findNewestInfos");
		Assertions.assertNotNull(newest.findServiceInfoByIdentity("78"));
		Assertions.assertNull(newest.findServiceInfoByIdentity("77"), "unregistered identity must disappear");
	}

	@Test
	public void testNewestSkipsEmptyBucket() {
		var state = newState();
		state.onRegister(new BServiceInfo(serviceName, "77", 1));
		state.onRegister(new BServiceInfo(serviceName, "88", 2));
		var newest = state.findNewestInfos();
		Assertions.assertNotNull(newest);
		Assertions.assertNotNull(newest.findServiceInfoByIdentity("88"));

		// v2 桶被清空后（空桶仍残留在 map 中），newest 应回落到 v1，而不是指向空列表。
		state.onUnRegister(new BServiceInfo(serviceName, "88", 2));
		newest = state.findNewestInfos();
		Assertions.assertNotNull(newest, "newest must fall back to non-empty bucket");
		Assertions.assertNotNull(newest.findServiceInfoByIdentity("77"));
		Assertions.assertNull(newest.findServiceInfoByIdentity("88"));
	}

	@Test
	public void testFindServiceInfoByIdentitySeesAllVersions() {
		var state = newState();
		state.onRegister(new BServiceInfo(serviceName, "77", 1));
		state.onRegister(new BServiceInfo(serviceName, "88", 2));

		// 混布版本：低版本桶里的服务器也必须能按 identity 找到（TestEnv 等待就绪依赖此语义）。
		Assertions.assertNotNull(state.findServiceInfoByIdentity("77"));
		Assertions.assertNotNull(state.findServiceInfoByIdentity("88"));
		Assertions.assertNull(state.findServiceInfoByIdentity("99"));
	}

	// 防止编码-解码路径的快照计算被误删：初始结果非空时 findNewestInfos 仍应正确返回最高版本桶。
	@Test
	public void testInitialSnapshotStillWorks() {
		var infos = new BServiceInfosVersion();
		infos.getOrAddInfos(1L).insert(new BServiceInfo(serviceName, "77", 1));
		infos.getOrAddInfos(2L).insert(new BServiceInfo(serviceName, "88", 2));

		var state = new AbstractAgent.SubscribeState(new BSubscribeInfo(serviceName));
		state.onFirstCommit(infos, new BEditService());
		var newest = state.findNewestInfos();
		Assertions.assertNotNull(newest);
		Assertions.assertNotNull(newest.findServiceInfoByIdentity("88"));
	}
}
