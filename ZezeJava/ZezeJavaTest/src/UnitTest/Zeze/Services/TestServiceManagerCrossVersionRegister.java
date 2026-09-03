package UnitTest.Zeze.Services;

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Net.AsyncSocket;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfos;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManager.EditService;
import Zeze.Services.ServiceManagerServer;
import harness.Fast;

/**
 * FND-S2-3：同 identity（name+id）跨版本重注册的"幽灵桶残留"。
 * <p>
 * BEditService 声明的 AddOrUpdate/remove 语义以 name+id 为 key：同 identity 重注册到新版本时，
 * 旧版本桶必须同步清理（服务端 ServiceState.addAndCollectNotify/removeAndCollectNotify、
 * 客户端 SubscribeState.onRegister/onUnRegister 两端一致），否则实例下线后订阅者仍看到死地址，
 * findNewestInfos 回落时幽灵成为最新版本查询结果。
 * <p>
 * 纯构造性单元测试：客户端直接构造 SubscribeState；服务端直接构造 ServiceState
 * （无订阅者时 collectNotify 不会触达 serviceManager.server，可传 null，不起网络）。
 */
@Fast
public class TestServiceManagerCrossVersionRegister {
	private static final String serviceName = "UnitTest.S23.Service";

	private static AbstractAgent.SubscribeState newSubscribeState() {
		var state = new AbstractAgent.SubscribeState(new BSubscribeInfo(serviceName));
		// 模拟订阅瞬间服务列表为空。
		state.onFirstCommit(new BServiceInfosVersion(), new BEditService());
		return state;
	}

	// 无订阅者：addAndCollectNotify/removeAndCollectNotify 的通知循环不会执行，
	// 不会解引用 serviceManager.server，可传 null 构造纯数据状态。
	private static ServiceManagerServer.ServiceState newServiceState() {
		//noinspection ConstantConditions
		return new ServiceManagerServer.ServiceState(null, serviceName);
	}

	private static void assertBucketEmpty(BServiceInfos bucket, String msg) {
		Assertions.assertTrue(bucket == null || bucket.getSortedIdentities().isEmpty(), msg);
	}

	// ------------------------- 客户端（SubscribeState） -------------------------

	@Test
	public void testClientCrossVersionReregisterCleansOldBucket() {
		var state = newSubscribeState();
		var info5 = new BServiceInfo(serviceName, "1", 5, "127.0.0.1", 1005);
		var info6 = new BServiceInfo(serviceName, "1", 6, "127.0.0.1", 1006);

		Assertions.assertNull(state.onRegister(info5), "首次注册没有可替换的旧记录");
		var replaced = state.onRegister(info6);
		Assertions.assertSame(info5, replaced, "跨版本重注册必须报告被替换的旧版本记录");

		assertBucketEmpty(state.getServiceInfosVersion().getInfos(5), "跨版本重注册后旧版本桶不能残留identity");
		var newest = state.findNewestInfos();
		Assertions.assertNotNull(newest);
		Assertions.assertSame(info6, newest.findServiceInfoByIdentity("1"));
		Assertions.assertSame(info6, state.findServiceInfoByIdentity("1"));
	}

	@Test
	public void testClientDowngradeReregisterFallsBackToOlderBucket() {
		var state = newSubscribeState();
		var info5 = new BServiceInfo(serviceName, "1", 5, "127.0.0.1", 1005);
		var info6 = new BServiceInfo(serviceName, "1", 6, "127.0.0.1", 1006);

		state.onRegister(info6);
		Assertions.assertSame(info6, state.onRegister(info5), "降版本重注册同样报告被替换的旧记录");
		assertBucketEmpty(state.getServiceInfosVersion().getInfos(6), "降版本重注册后高版本桶不能残留identity");

		// 跨版本重注册后 findNewestInfos 回落到最新的非空桶。
		var newest = state.findNewestInfos();
		Assertions.assertNotNull(newest, "newest必须回落到最新非空桶");
		Assertions.assertSame(info5, newest.findServiceInfoByIdentity("1"));
	}

	@Test
	public void testClientUnRegisterAfterCrossVersionReregisterNoGhost() {
		var state = newSubscribeState();
		var info5 = new BServiceInfo(serviceName, "1", 5, "127.0.0.1", 1005);
		var info6 = new BServiceInfo(serviceName, "1", 6, "127.0.0.1", 1006);

		state.onRegister(info5);
		state.onRegister(info6);
		Assertions.assertTrue(state.onUnRegister(info6), "注销最后一次注册必须生效");

		Assertions.assertNull(state.findNewestInfos(), "注销后不能有任何非空版本桶（幽灵）");
		Assertions.assertNull(state.findServiceInfoByIdentity("1"));
		for (var it = state.getServiceInfosVersion().getInfosIterator(); it.moveToNext(); )
			Assertions.assertTrue(it.value().getSortedIdentities().isEmpty(),
					"identity=" + it.key() + " 的版本桶必须为空");
	}

	@Test
	public void testClientUnRegisterIsIdentityKeyed() {
		var state = newSubscribeState();
		state.onRegister(new BServiceInfo(serviceName, "1", 5, "127.0.0.1", 1005));

		// 注销参数版本不匹配（如默认version=0）也按name+id收敛，与BEditService.remove声明一致。
		Assertions.assertTrue(state.onUnRegister(new BServiceInfo(serviceName, "1")));
		Assertions.assertNull(state.findServiceInfoByIdentity("1"));
		Assertions.assertFalse(state.onUnRegister(new BServiceInfo(serviceName, "1")),
				"重复注销不存在的identity返回false");
	}

	@Test
	public void testClientSameVersionUpdateBehaviorUnchanged() {
		var state = newSubscribeState();
		var infoA = new BServiceInfo(serviceName, "1", 5, "127.0.0.1", 1005);
		var infoB = new BServiceInfo(serviceName, "1", 5, "127.0.0.2", 2005);

		Assertions.assertNull(state.onRegister(infoA), "首次注册返回null");
		Assertions.assertSame(infoA, state.onRegister(infoB), "同版本更新仍返回被替换的旧记录");
		Assertions.assertNull(state.onRegister(infoB), "无变化的更新仍返回null");

		var bucket = state.getServiceInfosVersion().getInfos(5);
		Assertions.assertNotNull(bucket);
		Assertions.assertEquals(1, bucket.getSortedIdentities().size());
		Assertions.assertSame(infoB, bucket.getSortedIdentities().get(0));
	}

	// ------------------------- 服务端（ServiceState） -------------------------

	@Test
	public void testServerCrossVersionReregisterCleansOldBucketAndSnapshot() {
		var state = newServiceState();
		var notifies = new HashMap<AsyncSocket, EditService>();
		var info5 = new BServiceInfo(serviceName, "1", 5, "127.0.0.1", 1005);
		var info6 = new BServiceInfo(serviceName, "1", 6, "127.0.0.1", 1006);
		info5.setSessionId(7L);
		info6.setSessionId(7L);

		state.addAndCollectNotify(info5, notifies);
		state.addAndCollectNotify(info6, notifies);

		var buckets = state.getServiceInfos();
		Assertions.assertTrue(buckets.get(5L) == null || buckets.get(5L).isEmpty(),
				"跨版本重注册后服务端旧版本桶不能残留identity");
		Assertions.assertSame(info6, buckets.get(6L).get("1"));

		// 会话关闭：Session.registers以name+id为key只保留最后一次注册（info6），注销后全部桶清空。
		state.removeAndCollectNotify(info6, 7L, notifies);
		Assertions.assertTrue(buckets.get(6L).isEmpty());

		// 新订阅者的快照（遍历全部桶构造）不含幽灵。
		var snapshot = new BServiceInfosVersion(0, state);
		Assertions.assertNull(snapshot.getNewestInfos(), "快照不能包含幽灵版本桶");
		Assertions.assertNull(snapshot.getInfos(5));
		Assertions.assertNull(snapshot.getInfos(6));
	}

	@Test
	public void testServerRemoveIsIdentityKeyed() {
		var state = newServiceState();
		var notifies = new HashMap<AsyncSocket, EditService>();
		var info5 = new BServiceInfo(serviceName, "1", 5, "127.0.0.1", 1005);
		info5.setSessionId(7L);
		state.addAndCollectNotify(info5, notifies);

		// 注销参数版本不匹配（version=0）也按name+id收敛到全部版本桶。
		state.removeAndCollectNotify(new BServiceInfo(serviceName, "1"), 7L, notifies);
		Assertions.assertTrue(state.getServiceInfos().get(5L).isEmpty());
	}

	@Test
	public void testServerRemoveHonorsSessionOwnership() {
		var state = newServiceState();
		var notifies = new HashMap<AsyncSocket, EditService>();
		var infoNew = new BServiceInfo(serviceName, "1", 5, "127.0.0.1", 1005);
		infoNew.setSessionId(9L);
		state.addAndCollectNotify(infoNew, notifies);

		// 旧会话(7)的注销不能删除新会话(9)已AddOrUpdate的注册（此时忽略旧连接的注销）。
		state.removeAndCollectNotify(new BServiceInfo(serviceName, "1", 5), 7L, notifies);
		Assertions.assertSame(infoNew, state.getServiceInfos().get(5L).get("1"),
				"新会话的注册必须在旧会话的注销后存活");

		// 属主会话注销才移除。
		state.removeAndCollectNotify(infoNew, 9L, notifies);
		Assertions.assertTrue(state.getServiceInfos().get(5L).isEmpty());
	}
}
