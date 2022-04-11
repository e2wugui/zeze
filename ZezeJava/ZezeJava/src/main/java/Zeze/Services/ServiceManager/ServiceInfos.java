package Zeze.Services.ServiceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class ServiceInfos extends Bean {
	// ServiceList maybe empty. need a ServiceName
	private String ServiceName;
	public String getServiceName() {
		return ServiceName;
	}
	private void setServiceName(String value) {
		ServiceName = value;
	}
	// sorted by ServiceIdentity
	private final ArrayList<ServiceInfo> _ServiceInfoListSortedByIdentity = new ArrayList<> ();
	public ArrayList<ServiceInfo> getServiceInfoListSortedByIdentity() {
		return _ServiceInfoListSortedByIdentity;
	}
	private long SerialId;
	public long getSerialId() {
		return SerialId;
	}
	public void setSerialId(long value) {
		SerialId = value;
	}

	public ServiceInfo Insert(ServiceInfo info)
	{
		int index = Collections.binarySearch(_ServiceInfoListSortedByIdentity, info, ServiceInfoIdentityComparer);
		if (index >= 0)
			_ServiceInfoListSortedByIdentity.set(index, info);
		else
			_ServiceInfoListSortedByIdentity.add(~index, info);
		return info;
	}

	public ServiceInfo Remove(ServiceInfo info)
	{
		int index = Collections.binarySearch(_ServiceInfoListSortedByIdentity, info, ServiceInfoIdentityComparer);
		if (index >= 0) {
			info = _ServiceInfoListSortedByIdentity.get(index);
			_ServiceInfoListSortedByIdentity.remove(index);
			return info;
		}
		return null;
	}

	public ServiceInfo findServiceInfoByIdentity(String identity) {
		return get(identity);
	}
	public ServiceInfo findServiceInfoByServerId(int serverId) {
		return findServiceInfoByIdentity(String.valueOf(serverId));
	}

	public ServiceInfos() {
	}

	public ServiceInfos(String serviceName) {
		setServiceName(serviceName);
	}

	private static final Comparator<ServiceInfo> ServiceInfoIdentityComparer = Comparator.comparing(ServiceInfo::getServiceIdentity);

	public ServiceInfo get(String identity) {
		var cur = new ServiceInfo(getServiceName(), identity);
		int index = Collections.binarySearch(_ServiceInfoListSortedByIdentity, cur, ServiceInfoIdentityComparer);
		if (index >= 0) {
			return _ServiceInfoListSortedByIdentity.get(index);
		}
		return null;
	}

	public ServiceInfos(String serviceName, ServiceManagerServer.ServerState state, long serialId) {
		ServiceName = serviceName;
		for (var e : state.getServiceInfos().entrySet()) {
			_ServiceInfoListSortedByIdentity.add(e.getValue());
		}
		SerialId = serialId;
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setServiceName(bb.ReadString());
		getServiceInfoListSortedByIdentity().clear();
		for (int c = bb.ReadInt(); c > 0; --c) {
			var service = new ServiceInfo();
			service.Decode(bb);
			getServiceInfoListSortedByIdentity().add(service);
		}
		setSerialId(bb.ReadLong());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteString(getServiceName());
		bb.WriteInt(getServiceInfoListSortedByIdentity().size());
		for (var service : getServiceInfoListSortedByIdentity()) {
			service.Encode(bb);
		}
		bb.WriteLong(getSerialId());
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
		sb.append(getServiceName()).append("=").append(" SerialId=").append(SerialId);
		sb.append("[");
		for (var e : getServiceInfoListSortedByIdentity()) {
			sb.append(e.getServiceIdentity());
			sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}
}
