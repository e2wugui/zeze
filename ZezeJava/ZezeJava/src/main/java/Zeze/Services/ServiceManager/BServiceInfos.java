package Zeze.Services.ServiceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class BServiceInfos extends Bean {
	private static final Comparator<BServiceInfo> Comparer = (si1, si2) -> {
		String id1 = si1.getServiceIdentity();
		String id2 = si2.getServiceIdentity();
		return id1.compareTo(id2);
	};

	// ServiceList maybe empty. need a ServiceName
	public String serviceName;
	// sorted by ServiceIdentity
	public final ArrayList<BServiceInfo> serviceInfoListSortedByIdentity = new ArrayList<>();
	public long serialId;

	public BServiceInfos() {
	}

	public BServiceInfos(String serviceName) {
		this.serviceName = serviceName;
	}

	public BServiceInfos(String serviceName, ServiceManagerServer.ServiceState state, long serialId) {
		this.serviceName = serviceName;
		state.getServiceInfos(serviceInfoListSortedByIdentity);
		serviceInfoListSortedByIdentity.sort(Comparer);
		this.serialId = serialId;
	}

	public String getServiceName() {
		return serviceName;
	}

	public ArrayList<BServiceInfo> getServiceInfoListSortedByIdentity() {
		return serviceInfoListSortedByIdentity;
	}

	public long getSerialId() {
		return serialId;
	}

	public void insert(BServiceInfo info) {
		int index = Collections.binarySearch(serviceInfoListSortedByIdentity, info, Comparer);
		if (index >= 0)
			serviceInfoListSortedByIdentity.set(index, info);
		else
			serviceInfoListSortedByIdentity.add(~index, info);
	}

	public BServiceInfo remove(BServiceInfo info) {
		int index = Collections.binarySearch(serviceInfoListSortedByIdentity, info, Comparer);
		return index >= 0 ? serviceInfoListSortedByIdentity.remove(index) : null;
	}

	public BServiceInfo findServiceInfo(BServiceInfo info) {
		int index = Collections.binarySearch(serviceInfoListSortedByIdentity, info, Comparer);
		return index >= 0 ? serviceInfoListSortedByIdentity.get(index) : null;
	}

	public BServiceInfo findServiceInfoByIdentity(String identity) {
		return findServiceInfo(new BServiceInfo(serviceName, identity));
	}

	public BServiceInfo findServiceInfoByServerId(int serverId) {
		return findServiceInfoByIdentity(String.valueOf(serverId));
	}

	@Override
	public void decode(IByteBuffer bb) {
		serviceName = bb.ReadString();
		serviceInfoListSortedByIdentity.clear();
		for (int c = bb.ReadInt(); c > 0; --c) {
			var service = new BServiceInfo();
			service.decode(bb);
			serviceInfoListSortedByIdentity.add(service);
		}
		serialId = bb.ReadLong();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(serviceName);
		bb.WriteInt(serviceInfoListSortedByIdentity.size());
		for (var service : serviceInfoListSortedByIdentity) {
			service.encode(bb);
		}
		bb.WriteLong(serialId);
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
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void initChildrenRootInfoWithRedo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(serviceName).append(" Version=").append(serialId);
		sb.append("[");
		for (var e : serviceInfoListSortedByIdentity) {
			sb.append(e.getServiceIdentity());
			sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}
}
