package Zeze.Services.ServiceManager;

import java.util.HashMap;
import Zeze.Builtin.ServiceManagerWithRaft.BServerState;
import Zeze.Builtin.ServiceManagerWithRaft.BServiceInfosVersionRocks;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.LongHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BServiceInfosVersion implements Serializable {
	private final LongHashMap<BServiceInfos> infosVersion = new LongHashMap<>(); // key:version
	private transient final @Nullable BServiceInfos newestInfos;

	public BServiceInfosVersion() {
		newestInfos = null;
	}

	public BServiceInfosVersion(@NotNull ByteBuffer bb) {
		decode(bb);
		newestInfos = findNewestInfos();
	}

	public BServiceInfosVersion(long hopeVersion, @NotNull ServiceManagerServer.ServiceState state) {
		if (hopeVersion != 0) {
			var identityMap = state.getServiceInfos().get(hopeVersion);
			if (identityMap != null)
				copyAndSortIdentityMap(hopeVersion, identityMap);
		} else {
			for (var e : state.getServiceInfos().entrySet())
				copyAndSortIdentityMap(e.getKey(), e.getValue());
		}
		newestInfos = findNewestInfos();
	}

	public BServiceInfosVersion(long hopeVersion, @NotNull BServerState state) {
		if (hopeVersion != 0) {
			var identityMap = state.getServiceInfosVersion().get(hopeVersion);
			if (identityMap != null)
				copyAndSortIdentityMap(hopeVersion, identityMap);
		} else {
			for (var e : state.getServiceInfosVersion().entrySet())
				copyAndSortIdentityMap(e.getKey(), e.getValue());
		}
		newestInfos = findNewestInfos();
	}

	private void copyAndSortIdentityMap(long version, @NotNull HashMap<String, BServiceInfo> identityMap) {
		if (!identityMap.isEmpty()) {
			var infos = new BServiceInfos();
			infos.getSortedIdentities().addAll(identityMap.values());
			infos.getSortedIdentities().sort(BServiceInfos.comparer);
			infosVersion.put(version, infos);
		}
	}

	private void copyAndSortIdentityMap(long version, @NotNull BServiceInfosVersionRocks identityMap) {
		if (identityMap.getServiceInfos().size() > 0) {
			var infos = new BServiceInfos();
			for (var info : identityMap.getServiceInfos().values()) {
				infos.getSortedIdentities().add(new BServiceInfo(info.getServiceName(), info.getServiceIdentity(),
						info.getVersion(), info.getPassiveIp(), info.getPassivePort(), info.getExtraInfo()));
			}
			infos.getSortedIdentities().sort(BServiceInfos.comparer);
			infosVersion.put(version, infos);
		}
	}

	private @Nullable BServiceInfos findNewestInfos() {
		BServiceInfos resultInfos = null;
		var maxVersion = Long.MIN_VALUE;
		for (var it = infosVersion.iterator(); it.moveToNext(); ) {
			if (maxVersion <= it.key()) {
				maxVersion = it.key();
				resultInfos = it.value();
			}
		}
		return resultInfos;
	}

	public @Nullable BServiceInfos getInfos(long version) {
		return infosVersion.get(version);
	}

	public @NotNull LongHashMap<BServiceInfos>.Iterator getInfosIterator() {
		return infosVersion.iterator();
	}

	public @Nullable BServiceInfos getNewestInfos() {
		return newestInfos;
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
			if (!infos.getSortedIdentities().isEmpty())
				infosVersion.put(version, infos);
		}
	}

	private static int _PRE_ALLOC_SIZE_ = 64;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@Override
	public @NotNull String toString() {
		return "BServiceInfosVersion" + infosVersion;
	}
}
