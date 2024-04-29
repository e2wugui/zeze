package Zeze.Services.ServiceManager;

import java.util.Collections;
import java.util.Comparator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.FewModifyList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BServiceInfos implements Serializable {
	public static final Comparator<BServiceInfo> comparer = (si1, si2) -> {
		var id1 = si1.getServiceIdentity();
		var id2 = si2.getServiceIdentity();
		if (id1.isEmpty() || id1.charAt(0) == '@' || id2.isEmpty() || id2.charAt(0) == '@')
			return id1.compareTo(id2);
		return Long.compare(Long.parseLong(id1), Long.parseLong(id2));
	};

	private final FewModifyList<BServiceInfo> sortedIdentities = new FewModifyList<>(); // sorted by ServiceIdentity

	public @NotNull FewModifyList<BServiceInfo> getSortedIdentities() {
		return sortedIdentities;
	}

	/**
	 * @return old BServiceInfo
	 */
	public @Nullable BServiceInfo insert(@NotNull BServiceInfo info) {
		int index = Collections.binarySearch(sortedIdentities, info, comparer);
		if (index >= 0) {
			var exist = sortedIdentities.get(index);
			sortedIdentities.set(index, info);
			return exist;
		}
		sortedIdentities.add(~index, info);
		return null;
	}

	public @Nullable BServiceInfo remove(@NotNull BServiceInfo info) {
		int index = Collections.binarySearch(sortedIdentities, info, comparer);
		return index >= 0 ? sortedIdentities.remove(index) : null;
	}

	public @Nullable BServiceInfo findServiceInfo(@NotNull BServiceInfo info) {
		int index = Collections.binarySearch(sortedIdentities, info, comparer);
		return index >= 0 ? sortedIdentities.get(index) : null;
	}

	public @Nullable BServiceInfo findServiceInfoByIdentity(@NotNull String identity) {
		return findServiceInfo(new BServiceInfo("", identity, 0)); // 比较时只看identity
	}

	public @Nullable BServiceInfo findServiceInfoByServerId(int serverId) {
		return findServiceInfoByIdentity(String.valueOf(serverId));
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		sortedIdentities.clear();
		for (int c = bb.ReadUInt(); c > 0; --c)
			sortedIdentities.add(new BServiceInfo(bb));
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteUInt(sortedIdentities.size());
		for (var service : sortedIdentities) {
			service.encode(bb);
		}
	}

	private static int _PRE_ALLOC_SIZE_ = 32;

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
		return "BServiceInfos" + sortedIdentities;
	}
}
