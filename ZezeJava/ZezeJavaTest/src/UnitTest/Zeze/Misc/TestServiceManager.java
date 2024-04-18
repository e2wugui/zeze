package UnitTest.Zeze.Misc;

import java.nio.charset.StandardCharsets;
import Zeze.Net.Binary;
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
		var infos = new BServiceInfos("TestBase");
		infos.insert(new BServiceInfo("TestBase", "1"));
		infos.insert(new BServiceInfo("TestBase", "3"));
		infos.insert(new BServiceInfo("TestBase", "2"));
		Assert.assertEquals("TestBase Version=0[1,2,3,]", infos.toString());
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
		agent.registerService(serviceName, "1", "127.0.0.1", 1234);
		agent.setOnChanged((state) -> {
			System.out.println("OnChanged: " + state.getServiceInfos());
			this.future.setResult(0);
		});
		agent.setOnSetServerLoad((load) -> {
			System.out.println("OnSetLoad " + load);
			this.future.setResult(0);
		});
		agent.subscribeService(serviceName, BSubscribeInfo.SubscribeTypeSimple);
		var load = new BServerLoad();
		load.ip = "127.0.0.1";
		load.port = 1234;
		System.out.println("WaitOnSetLoad");
		agent.setServerLoad(load);
		future.await();

		future = new TaskCompletionSource<>();
		agent.setOnUpdate((state, info) -> {
			System.out.println("OnUpdate: " + info);
			this.future.setResult(0);
		});
		System.out.println("WaitOnUpdate");
		agent.updateService(serviceName, "1", "1.1.1.1", 1, new Binary("extra info".getBytes(StandardCharsets.UTF_8)));
		future.await();

		System.out.println("RegisterService 2");
		future = new TaskCompletionSource<>();
		System.out.println("WaitOnChanged 2");
		agent.registerService(serviceName, "2");
		future.await();

		var state = agent.getSubscribeStates().get(serviceName);
		Object anyState = this;
		state.setIdentityLocalState("1", anyState);
		state.setIdentityLocalState("2", anyState);
		state.setIdentityLocalState("3", anyState);

		System.out.println("RegisterService 3");
		future = new TaskCompletionSource<>();
		System.out.println("WaitOnChanged 3");
		agent.registerService(serviceName, "3");
		future.await();
	}
}
