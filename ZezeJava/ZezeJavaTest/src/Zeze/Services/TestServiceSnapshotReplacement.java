package Zeze.Services;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManager.BSubscribeResult;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestServiceSnapshotReplacement {
	private static ByteBuffer snapshot(long version, String identity) {
		var infos = new BServiceInfosVersion();
		infos.getOrAddInfos(version).insert(new BServiceInfo("service", identity, version));
		var bb = ByteBuffer.Allocate();
		infos.encode(bb);
		return bb;
	}

	@Test
	public void decodingNewSnapshotRemovesWithdrawnVersions() {
		var infos = new BServiceInfosVersion(snapshot(9, "1"));
		infos.decode(snapshot(2, "2"));
		assertNull(infos.getInfos(9));
		var newest = infos.getNewestInfos();
		assertNotNull(newest);
		assertNotNull(newest.findServiceInfoByIdentity("2"));
		assertNull(newest.findServiceInfoByIdentity("1"));
	}

	@Test
	public void emptySnapshotRemovesEveryVersion() {
		var infos = new BServiceInfosVersion(snapshot(9, "1"));
		var empty = ByteBuffer.Allocate();
		new BServiceInfosVersion().encode(empty);
		infos.decode(empty);
		assertNull(infos.getNewestInfos());
		assertFalse(infos.getInfosIterator().moveToNext());
	}

	@Test
	public void editMessageReplacesBothLists() {
		var edit = new BEditService();
		edit.getRemove().add(new BServiceInfo("old", "1"));
		edit.getAdd().add(new BServiceInfo("old", "2"));
		var replacement = new BEditService();
		replacement.getAdd().add(new BServiceInfo("new", "3"));
		var bb = ByteBuffer.Allocate();
		replacement.encode(bb);
		edit.decode(bb);
		assertTrue(edit.getRemove().isEmpty());
		assertEquals(replacement.getAdd(), edit.getAdd());
		bb.Reset();
		new BEditService().encode(bb);
		edit.decode(bb);
		assertTrue(edit.getAdd().isEmpty());
	}

	@Test
	public void subscribeRequestReplacesOldSubscriptions() {
		var arg = new BSubscribeArgument();
		arg.subs.add(new BSubscribeInfo("old", 9));
		var replacement = new BSubscribeArgument();
		replacement.subs.add(new BSubscribeInfo("new", 2));
		var bb = ByteBuffer.Allocate();
		replacement.encode(bb);
		arg.decode(bb);
		assertEquals(replacement.subs, arg.subs);
		bb.Reset();
		new BSubscribeArgument().encode(bb);
		arg.decode(bb);
		assertTrue(arg.subs.isEmpty());
	}

	@Test
	public void unsubscribeRequestReplacesOldNames() {
		var arg = new BUnSubscribeArgument();
		arg.serviceNames.add("old");
		var replacement = new BUnSubscribeArgument();
		replacement.serviceNames.add("new");
		var bb = ByteBuffer.Allocate();
		replacement.encode(bb);
		arg.decode(bb);
		assertEquals(replacement.serviceNames, arg.serviceNames);
		bb.Reset();
		new BUnSubscribeArgument().encode(bb);
		arg.decode(bb);
		assertTrue(arg.serviceNames.isEmpty());
	}

	@Test
	public void subscribeResponseReplacesOldServices() {
		var result = new BSubscribeResult();
		result.map.put("old", new BServiceInfosVersion(snapshot(9, "1")));
		var replacement = new BSubscribeResult();
		replacement.map.put("new", new BServiceInfosVersion(snapshot(2, "2")));
		var bb = ByteBuffer.Allocate();
		replacement.encode(bb);
		result.decode(bb);
		assertEquals(replacement.map.keySet(), result.map.keySet());
		bb.Reset();
		new BSubscribeResult().encode(bb);
		result.decode(bb);
		assertTrue(result.map.isEmpty());
	}
}
