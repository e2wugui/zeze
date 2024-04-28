package Zeze.Services.ServiceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BServiceInfos extends Bean {
	public static final Comparator<BServiceInfo> comparer = (si1, si2) -> {
		var id1 = si1.getServiceIdentity();
		var id2 = si2.getServiceIdentity();
		if (id1.isEmpty() || id1.charAt(0) == '@' || id2.isEmpty() || id2.charAt(0) == '@')
			return id1.compareTo(id2);
		return Long.compare(Long.parseLong(id1), Long.parseLong(id2));
	};

	// ServiceList maybe empty. need a ServiceName
	private @NotNull String serviceName = "";
	// sorted by ServiceIdentity
	private final ArrayList<BServiceInfo> serviceInfoListSortedByIdentity = new ArrayList<>();

	public BServiceInfos() {
	}

	public BServiceInfos(@NotNull String serviceName) {
		this.serviceName = serviceName;
	}

	public @NotNull String getServiceName() {
		return serviceName;
	}

	public void setServiceName(@NotNull String serviceName) {
		this.serviceName = serviceName;
	}

	public @NotNull ArrayList<BServiceInfo> getServiceInfoListSortedByIdentity() {
		return serviceInfoListSortedByIdentity;
	}

	/**
	 * @return old BServiceInfo
	 */
	public @Nullable BServiceInfo insert(@NotNull BServiceInfo info) {
		int index = Collections.binarySearch(serviceInfoListSortedByIdentity, info, comparer);
		if (index >= 0) {
			var exist = serviceInfoListSortedByIdentity.get(index);
			serviceInfoListSortedByIdentity.set(index, info);
			return exist;
		}
		serviceInfoListSortedByIdentity.add(~index, info);
		return null;
	}

	public @Nullable BServiceInfo remove(@NotNull BServiceInfo info) {
		int index = Collections.binarySearch(serviceInfoListSortedByIdentity, info, comparer);
		return index >= 0 ? serviceInfoListSortedByIdentity.remove(index) : null;
	}

	public @Nullable BServiceInfo findServiceInfo(@NotNull BServiceInfo info) {
		int index = Collections.binarySearch(serviceInfoListSortedByIdentity, info, comparer);
		return index >= 0 ? serviceInfoListSortedByIdentity.get(index) : null;
	}

	public @Nullable BServiceInfo findServiceInfoByIdentity(@NotNull String identity) {
		return findServiceInfo(new BServiceInfo(serviceName, identity, 0));
	}

	public @Nullable BServiceInfo findServiceInfoByServerId(int serverId) {
		return findServiceInfoByIdentity(String.valueOf(serverId));
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		serviceName = bb.ReadString();
		serviceInfoListSortedByIdentity.clear();
		for (int c = bb.ReadUInt(); c > 0; --c)
			serviceInfoListSortedByIdentity.add(new BServiceInfo(bb));
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteString(serviceName);
		bb.WriteUInt(serviceInfoListSortedByIdentity.size());
		for (var service : serviceInfoListSortedByIdentity) {
			service.encode(bb);
		}
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@Override
	protected void initChildrenRootInfo(@NotNull Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void initChildrenRootInfoWithRedo(@NotNull Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		sb.append(serviceName);
		sb.append("[");
		for (var e : serviceInfoListSortedByIdentity) {
			sb.append(e.getServiceIdentity());
			sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}
}
