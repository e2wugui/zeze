package UnitTest.Zeze.Misc;

import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfos;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Util.TaskCompletionSource;
import demo.App;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestServiceManager {
	@Test
	public void testServiceInfos() {
		var infos = new BServiceInfos();
		infos.insert(new BServiceInfo("TestBase", "1", 0));
		infos.insert(new BServiceInfo("TestBase", "3", 0));
		infos.insert(new BServiceInfo("TestBase", "2", 0));
		var it = infos.getSortedIdentities().iterator();
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals("1", it.next().getServiceIdentity());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals("2", it.next().getServiceIdentity());
		Assert.assertTrue(it.hasNext());
		Assert.assertEquals("3", it.next().getServiceIdentity());
		Assert.assertFalse(it.hasNext());
	}

	@Before
	public void before() throws Exception {
		App.Instance.Start();
	}

	@After
	public void after() throws Exception {
		//App.Instance.Stop();
	}

	TaskCompletionSource<Integer> future;

	@Test
	public void test1() {
		if (null == App.Instance.Zeze.getServiceManager())
			return; // disable

		var serviceName = "TestServiceManager";
		future = new TaskCompletionSource<>();

		var agent = App.Instance.Zeze.getServiceManager();
		agent.registerService(new BServiceInfo(serviceName, "1", 0, "127.0.0.1", 1234));
		agent.setOnChanged((state) -> {
			System.out.println("OnChanged 1:" + state);
			this.future.setResult(0);
		});
		agent.setOnSetServerLoad((load) -> {
			System.out.println("OnSetLoad " + load);
			this.future.setResult(0);
		});
		agent.subscribeService(new BSubscribeInfo(serviceName));
		var load = new BServerLoad();
		load.ip = "127.0.0.1";
		load.port = 1234;
		System.out.println("WaitOnSetLoad");
		agent.setServerLoad(load);
		future.await();

		System.out.println("RegisterService 2");
		future = new TaskCompletionSource<>();
		System.out.println("WaitOnChanged 2");
		agent.registerService(new BServiceInfo(serviceName, "2"));
		future.await();

		var state = agent.getSubscribeStates().get(serviceName);
		Object anyState = this;
		state.setIdentityLocalState("1", anyState);
		state.setIdentityLocalState("2", anyState);
		state.setIdentityLocalState("3", anyState);

		System.out.println("RegisterService 3");
		future = new TaskCompletionSource<>();
		System.out.println("WaitOnChanged 3");
		agent.registerService(new BServiceInfo(serviceName, "3"));
		future.await();
	}
}
