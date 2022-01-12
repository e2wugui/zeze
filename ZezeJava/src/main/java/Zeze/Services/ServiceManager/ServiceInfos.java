package Zeze.Services.ServiceManager;

import java.util.ArrayList;
import java.util.Collections;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;

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
	public ServiceInfo findServiceInfoByIdentity(String identity) {
		// _ServiceInfoListSortedByIdentity 排序的，可以优化为binarySearch。
		for (var si : _ServiceInfoListSortedByIdentity) {
			if (si.getServiceIdentity().equals(identity))
				return si;
		}
		return null;
	}
	public ServiceInfo findServiceInfoByServerId(int serverId) {
		return findServiceInfoByIdentity(String.valueOf(serverId));
	}

	public ServiceInfos() {
	}

	public ServiceInfos(String serviceName) {
		setServiceName(serviceName);
	}

	public ServiceInfo get(String identity) {
		var cur = new ServiceInfo(getServiceName(), identity);
		int index = Collections.binarySearch(_ServiceInfoListSortedByIdentity, cur);
		if (index >= 0) {
			return _ServiceInfoListSortedByIdentity.get(index);
		}
		return null;
	}

	public ServiceInfos(String serviceName, Zeze.Services.ServiceManagerServer.ServerState state, long serialId) {
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

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(getServiceName()).append("=");
		sb.append("[");
		for (var e : getServiceInfoListSortedByIdentity()) {
			sb.append(e.getServiceIdentity());
			sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}
}
