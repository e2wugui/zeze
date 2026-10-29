package Zeze.Services;

import static java.util.Objects.requireNonNull;

import Zeze.Builtin.ServiceManagerWithRaft.Edit;
import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe;
import Zeze.Services.ServiceManager.BServiceInfo;
import harness.Fast;
import harness.ForkedJvm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceLock;

@Fast
@ForkedJvm
@ResourceLock("sm-session-cluster")
public class TestServiceManagerRaftSessionOwnership {

	@Test
	@Timeout(150)
	void currentConnectionCanLoginAgainAndPublish() throws Exception {
		try (var cluster = new SmRaftTestCluster()) {
			var current = cluster.login("SM.Ownership.Current");
			var retry = new Login();
			retry.Argument.setSessionName("SM.Ownership.Current");
			Assertions.assertEquals(0, current.call(retry), "same connection Login retry remains compatible");
			var info = new BServiceInfo("SM.Ownership.CurrentService", "23", 0, "127.0.0.1", 7723);
			var add = new Edit();
			add.Argument.getAdd().add(info);
			Assertions.assertEquals(0, current.call(add));
			var snapshot = current.subscribe(info.getServiceName()).Result.map.get(info.getServiceName());
			Assertions.assertNotNull(requireNonNull(snapshot.getInfos(0)).findServiceInfoByIdentity("23"));
			var unsubscribe = new UnSubscribe();
			unsubscribe.Argument.serviceNames.add(info.getServiceName());
			Assertions.assertEquals(0, current.call(unsubscribe));
		}
	}

	@Test
	@Timeout(150)
	void oldConnectionCannotRemoveReplacementRegistration() throws Exception {
		try (var cluster = new SmRaftTestCluster()) {
			var old = cluster.login("SM.Ownership.Publisher");
			var replacement = cluster.login("SM.Ownership.Publisher");
			var observer = cluster.login("SM.Ownership.Observer");
			var info = new BServiceInfo("SM.Ownership.Service", "17", 0, "127.0.0.1", 7717);
			var add = new Edit();
			add.Argument.getAdd().add(info);
			Assertions.assertEquals(0, replacement.call(add));
			Assertions.assertNotNull(requireNonNull(observer.subscribe(info.getServiceName()).Result.map
					.get(info.getServiceName()).getInfos(0)).findServiceInfoByIdentity("17"));

			// 同名重连已经提交，旧 TCP 仍在或其旧队列请求随后执行时都必须拒绝。
			var remove = new Edit();
			remove.Argument.getRemove().add(info);
			long staleCode = old.call(remove);
			var snapshot = observer.subscribe(info.getServiceName()).Result.map.get(info.getServiceName());
			var bucket = snapshot.getInfos(0);
			Assertions.assertAll(
					() -> Assertions.assertEquals(ServiceManagerWithRaft.ErrorNotLogin, staleCode),
					() -> Assertions.assertTrue(bucket != null && bucket.findServiceInfoByIdentity("17") != null,
							"old connection must not delete the replacement's published route"));
		}
	}

	@Test
	@Timeout(150)
	void oldConnectionCannotUnsubscribeReplacement() throws Exception {
		try (var cluster = new SmRaftTestCluster()) {
			var old = cluster.login("SM.Ownership.Subscriber");
			var replacement = cluster.login("SM.Ownership.Subscriber");
			replacement.subscribe("SM.Ownership.Feed");
			var unsubscribe = new UnSubscribe();
			unsubscribe.Argument.serviceNames.add("SM.Ownership.Feed");
			Assertions.assertEquals(ServiceManagerWithRaft.ErrorNotLogin, old.call(unsubscribe),
					"superseded connection must not remove the replacement's subscription");
		}
	}
}
