package Zeze.Services;

import Zeze.Net.Binary;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestSubscribeSnapshotChanges {
	private static void checkReplacement(BServiceInfo old, BServiceInfo replacement) {
		var state = new AbstractAgent.SubscribeState(new BSubscribeInfo("service"));
		state.onRegister(old);
		var snapshot = new BServiceInfosVersion();
		snapshot.getOrAddInfos(replacement.getVersion()).insert(replacement);
		var edits = new BEditService();
		state.onFirstCommit(snapshot, edits);
		assertEquals(1, edits.getRemove().size());
		assertSame(old, edits.getRemove().getFirst());
		assertEquals(1, edits.getAdd().size());
		assertSame(replacement, edits.getAdd().getFirst());
		assertSame(replacement, state.findServiceInfoByIdentity("1"));
	}

	@Test
	public void changedAddressWithdrawsPreviousEndpoint() {
		checkReplacement(new BServiceInfo("service", "1", 2, "old", 1234),
				new BServiceInfo("service", "1", 2, "new", 5678));
	}

	@Test
	public void changedMetadataWithdrawsPreviousRegistration() {
		checkReplacement(new BServiceInfo("service", "1", 2, "host", 1234, new Binary("old")),
				new BServiceInfo("service", "1", 2, "host", 1234, new Binary("new")));
	}

	@Test
	public void unchangedSnapshotDoesNotWithdrawLiveEndpoint() {
		var state = new AbstractAgent.SubscribeState(new BSubscribeInfo("service"));
		state.onRegister(new BServiceInfo("service", "1", 2, "host", 1234));
		var snapshot = new BServiceInfosVersion();
		snapshot.getOrAddInfos(2).insert(new BServiceInfo("service", "1", 2, "host", 1234));
		var edits = new BEditService();
		state.onFirstCommit(snapshot, edits);
		assertTrue(edits.getRemove().isEmpty());
		assertEquals(1, edits.getAdd().size());
	}
}
