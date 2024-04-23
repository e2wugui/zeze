package Zeze.Services.ServiceManager;

import java.util.HashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

public class BServiceInfosVersion extends Bean {
	private final HashMap<Long, BServiceInfos> infosVersion = new HashMap<>();

	public HashMap<Long, BServiceInfos> getInfosVersion() {
		return infosVersion;
	}

	public BServiceInfosVersion() {

	}

	private void copyAndSortIdentityMap(long version, HashMap<String, BServiceInfo> identityMap) {
		var infos = new BServiceInfos();
		infos.getServiceInfoListSortedByIdentity().addAll(identityMap.values());
		infos.getServiceInfoListSortedByIdentity().sort(BServiceInfos.Comparer);
		infosVersion.put(version, infos);
	}

	public BServiceInfosVersion(long hopeVersion, ServiceManagerServer.ServiceState state) {
		if (hopeVersion > 0) {
			var identityMap = state.getServiceInfos().get(hopeVersion);
			if (null != identityMap)
				copyAndSortIdentityMap(hopeVersion, identityMap);
		} else {
			for (var e : state.getServiceInfos().entrySet()) {
				copyAndSortIdentityMap(e.getKey(), e.getValue());
			}
		}
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteInt(infosVersion.size());
		for (var e : infosVersion.entrySet()) {
			bb.WriteLong(e.getKey());
			e.getValue().encode(bb);
		}
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		for (var i = bb.ReadInt(); i > 0; --i) {
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