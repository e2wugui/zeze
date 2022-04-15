package Zeze.Services.ServiceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class ServiceInfos extends Bean {
	private static final Comparator<ServiceInfo> ServiceInfoIdentityComparer = Comparator.comparing(ServiceInfo::getServiceIdentity);

	// ServiceList maybe empty. need a ServiceName
	private String ServiceName;
	// sorted by ServiceIdentity
	private final ArrayList<ServiceInfo> _ServiceInfoListSortedByIdentity = new ArrayList<>();
	private long SerialId;

	public ServiceInfos() {
	}

	public ServiceInfos(String serviceName) {
		ServiceName = serviceName;
	}

	public ServiceInfos(String serviceName, ServiceManagerServer.ServerState state, long serialId) {
		ServiceName = serviceName;
		_ServiceInfoListSortedByIdentity.addAll(state.getServiceInfos().values());
		_ServiceInfoListSortedByIdentity.sort(ServiceInfoIdentityComparer);
		SerialId = serialId;
	}

	public String getServiceName() {
		return ServiceName;
	}

	public ArrayList<ServiceInfo> getServiceInfoListSortedByIdentity() {
		return _ServiceInfoListSortedByIdentity;
	}

	public long getSerialId() {
		return SerialId;
	}

	public ServiceInfo Insert(ServiceInfo info) {
		int index = Collections.binarySearch(_ServiceInfoListSortedByIdentity, info, ServiceInfoIdentityComparer);
		if (index >= 0)
			_ServiceInfoListSortedByIdentity.set(index, info);
		else
			_ServiceInfoListSortedByIdentity.add(~index, info);
		return info;
	}

	public ServiceInfo Remove(ServiceInfo info) {
		int index = Collections.binarySearch(_ServiceInfoListSortedByIdentity, info, ServiceInfoIdentityComparer);
		if (index >= 0) {
			info = _ServiceInfoListSortedByIdentity.get(index);
			_ServiceInfoListSortedByIdentity.remove(index);
			return info;
		}
		return null;
	}

	public ServiceInfo get(String identity) {
		var cur = new ServiceInfo(ServiceName, identity);
		int index = Collections.binarySearch(_ServiceInfoListSortedByIdentity, cur, ServiceInfoIdentityComparer);
		if (index >= 0) {
			return _ServiceInfoListSortedByIdentity.get(index);
		}
		return null;
	}

	public ServiceInfo findServiceInfoByIdentity(String identity) {
		return get(identity);
	}

	public ServiceInfo findServiceInfoByServerId(int serverId) {
		return findServiceInfoByIdentity(String.valueOf(serverId));
	}

	@Override
	public void Decode(ByteBuffer bb) {
		ServiceName = bb.ReadString();
		_ServiceInfoListSortedByIdentity.clear();
		for (int c = bb.ReadInt(); c > 0; --c) {
			var service = new ServiceInfo();
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
	public int getPreAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void setPreAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
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
