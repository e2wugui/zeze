package Zeze.Services.ServiceManager;

import java.util.HashMap;
import java.util.TreeMap;
import Zeze.Builtin.ServiceManagerWithRaft.BServerState;
import Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoKeyRocks;
import Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoRocks;
import Zeze.Builtin.ServiceManagerWithRaft.BServiceInfosVersionRocks;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.Bean;
import Zeze.Util.LongHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BServiceInfosVersion extends Bean {
	private final LongHashMap<BServiceInfos> infosVersion = new LongHashMap<>(); // key:version

	public BServiceInfosVersion() {
	}

	private void copyAndSortIdentityMap(long version, @NotNull HashMap<String, BServiceInfo> identityMap) {
		var infos = new BServiceInfos();
		infos.getSortedIdentities().addAll(identityMap.values());
		infos.getSortedIdentities().sort(BServiceInfos.comparer);
		infosVersion.put(version, infos);
	}

	public BServiceInfosVersion(long hopeVersion, @NotNull ServiceManagerServer.ServiceState state) {
		if (hopeVersion != 0) {
			var identityMap = state.getServiceInfos().get(hopeVersion);
			if (null != identityMap)
				copyAndSortIdentityMap(hopeVersion, identityMap);
		} else {
			for (var e : state.getServiceInfos().entrySet()) {
				copyAndSortIdentityMap(e.getKey(), e.getValue());
			}
		}
	}

	private static BServiceInfos newSortedBServiceInfos(String serviceName, BServiceInfosVersionRocks identityMap) {
		var result = new BServiceInfos();
		var sortedMap = new TreeMap<BServiceInfoKeyRocks, BServiceInfoRocks>();
		for (var info : identityMap.getServiceInfos().entrySet())
			sortedMap.put(new BServiceInfoKeyRocks(serviceName, info.getKey()), info.getValue());
		for (var rocks : sortedMap.values()) {
			result.getSortedIdentities().add(new BServiceInfo(rocks.getServiceName(), rocks.getServiceIdentity(),
					rocks.getVersion(), rocks.getPassiveIp(), rocks.getPassivePort(), rocks.getExtraInfo()));
		}
		return result;
	}

	public BServiceInfosVersion(long hopeVersion, BServerState state) {
		var serviceName = state.getServiceName();
		if (hopeVersion != 0) {
			var identityMap = state.getServiceInfosVersion().get(hopeVersion);
			if (identityMap != null)
				infosVersion.put(hopeVersion, newSortedBServiceInfos(serviceName, identityMap));
		} else {
			for (var e : state.getServiceInfosVersion().entrySet())
				infosVersion.put(e.getKey(), newSortedBServiceInfos(serviceName, e.getValue()));
		}
	}

	public @Nullable BServiceInfos getInfos(long version) {
		return infosVersion.get(version);
	}

	public @NotNull LongHashMap<BServiceInfos>.Iterator getInfosIterator() {
		return infosVersion.iterator();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteUInt(infosVersion.size());
		for (var it = infosVersion.iterator(); it.moveToNext(); ) {
			bb.WriteLong(it.key());
			it.value().encode(bb);
		}
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		for (var i = bb.ReadUInt(); i > 0; --i) {
			var version = bb.ReadLong();
			var infos = new BServiceInfos();
			infos.decode(bb);
			infosVersion.put(version, infos);
		}
	}

	private static int _PRE_ALLOC_SIZE_ = 2048;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}
}
