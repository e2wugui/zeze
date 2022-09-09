package Zeze.Services.ServiceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class BServiceInfos extends Bean {
	private static final Comparator<BServiceInfo> Comparer = (si1, si2) -> {
		String id1 = si1.getServiceIdentity();
		String id2 = si2.getServiceIdentity();
		int c = Integer.compare(id1.length(), id2.length());
		return c != 0 ? c : id1.compareTo(id2);
	};

	// ServiceList maybe empty. need a ServiceName
	private String ServiceName;
	// sorted by ServiceIdentity
	private final ArrayList<BServiceInfo> _ServiceInfoListSortedByIdentity = new ArrayList<>();
	private long SerialId;

	public BServiceInfos() {
	}

	public BServiceInfos(String serviceName) {
		ServiceName = serviceName;
	}

	public BServiceInfos(String serviceName, ServiceManagerServer.ServerState state, long serialId) {
		ServiceName = serviceName;
		state.getServiceInfos(_ServiceInfoListSortedByIdentity);
		_ServiceInfoListSortedByIdentity.sort(Comparer);
		SerialId = serialId;
	}

	public String getServiceName() {
		return ServiceName;
	}

	public ArrayList<BServiceInfo> getServiceInfoListSortedByIdentity() {
		return _ServiceInfoListSortedByIdentity;
	}

	public long getSerialId() {
		return SerialId;
	}

	public void Insert(BServiceInfo info) {
		int index = Collections.binarySearch(_ServiceInfoListSortedByIdentity, info, Comparer);
		if (index >= 0)
			_ServiceInfoListSortedByIdentity.set(index, info);
		else
			_ServiceInfoListSortedByIdentity.add(~index, info);
	}

	public BServiceInfo Remove(BServiceInfo info) {
		int index = Collections.binarySearch(_ServiceInfoListSortedByIdentity, info, Comparer);
		return index >= 0 ? _ServiceInfoListSortedByIdentity.remove(index) : null;
	}

	public BServiceInfo findServiceInfo(BServiceInfo info) {
		int index = Collections.binarySearch(_ServiceInfoListSortedByIdentity, info, Comparer);
		return index >= 0 ? _ServiceInfoListSortedByIdentity.get(index) : null;
	}

	public BServiceInfo findServiceInfoByIdentity(String identity) {
		return findServiceInfo(new BServiceInfo(ServiceName, identity));
	}

	public BServiceInfo findServiceInfoByServerId(int serverId) {
		return findServiceInfoByIdentity(String.valueOf(serverId));
	}

	@Override
	public void Decode(ByteBuffer bb) {
		ServiceName = bb.ReadString();
		_ServiceInfoListSortedByIdentity.clear();
		for (int c = bb.ReadInt(); c > 0; --c) {
			var service = new BServiceInfo();
			service.Decode(bb);
			_ServiceInfoListSortedByIdentity.add(service);
		}
		SerialId = bb.ReadLong();
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteString(ServiceName);
		bb.WriteInt(_ServiceInfoListSortedByIdentity.size());
		for (var service : _ServiceInfoListSortedByIdentity) {
			service.Encode(bb);
		}
		bb.WriteLong(SerialId);
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
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void ResetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(ServiceName).append(" Version=").append(SerialId);
		sb.append("[");
		for (var e : _ServiceInfoListSortedByIdentity) {
			sb.append(e.getServiceIdentity());
			sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}
}
