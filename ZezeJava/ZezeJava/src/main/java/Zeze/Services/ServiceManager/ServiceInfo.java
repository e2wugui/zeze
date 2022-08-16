package Zeze.Services.ServiceManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class ServiceInfo extends Bean implements Comparable<ServiceInfo> {
	/**
	 * 服务名，比如"GameServer"
	 */
	private String ServiceName;

	/**
	 * 服务id，对于 Zeze.Application，一般就是 Config.ServerId.
	 * 这里使用类型 string 是为了更好的支持扩展。
	 */
	private String ServiceIdentity;

	/**
	 * 服务ip-port，如果没有，保持空和0.
	 */
	private String PassiveIp = "";
	private int PassivePort = 0;

	// 服务扩展信息，可选。
	private Binary ExtraInfo = Binary.Empty;

	// ServiceManager用来存储服务器的SessionId。算是一个优化吧。
	public Long SessionId;

	public String getServiceName() {
		return ServiceName;
	}

	private void setServiceName(String value) {
		ServiceName = value;
	}

	public String getServiceIdentity() {
		return ServiceIdentity;
	}

	private void setServiceIdentity(String value) {
		ServiceIdentity = value;
	}

	public String getPassiveIp() {
		return PassiveIp;
	}

	public void setPassiveIp(String value) {
		PassiveIp = value;
	}

	public int getPassivePort() {
		return PassivePort;
	}

	public void setPassivePort(int value) {
		PassivePort = value;
	}

	public Binary getExtraInfo() {
		return ExtraInfo;
	}

	public void setExtraInfo(Binary value) {
		ExtraInfo = value;
	}

	public ServiceInfo() {
	}

	public ServiceInfo(String name, String identity, String ip, int port) {
		this(name, identity, ip, port, null);
	}

	public ServiceInfo(String name, String identity, String ip) {
		this(name, identity, ip, 0, null);
	}

	public ServiceInfo(String name, String identity) {
		this(name, identity, null, 0, null);
	}

	public ServiceInfo(String name, String identity, String ip, int port, Binary extraInfo) {
		setServiceName(name);
		setServiceIdentity(identity);
		if (ip != null) {
			setPassiveIp(ip);
		}
		setPassivePort(port);
		if (extraInfo != null) {
			setExtraInfo(extraInfo);
		}
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setServiceName(bb.ReadString());
		setServiceIdentity(bb.ReadString());
		setPassiveIp(bb.ReadString());
		setPassivePort(bb.ReadInt());
		setExtraInfo(bb.ReadBinary());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteString(getServiceName());
		bb.WriteString(getServiceIdentity());
		bb.WriteString(getPassiveIp());
		bb.WriteInt(getPassivePort());
		bb.WriteBinary(getExtraInfo());
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
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + getServiceName().hashCode();
		result = prime * result + getServiceIdentity().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof ServiceInfo) {
			var other = (ServiceInfo)obj;
			return getServiceName().equals(other.getServiceName())
					&& getServiceIdentity().equals(other.getServiceIdentity());
		}
		return false;
	}

	@Override
	public int compareTo(ServiceInfo o) {
		int c = ServiceName.compareTo(o.ServiceName);
		if (c != 0)
			return c;

		return ServiceIdentity.compareTo(o.ServiceIdentity);
	}

	@Override
	public String toString() {
		return "ServiceInfo{" + "ServiceName='" + ServiceName + '\'' + ", ServiceIdentity='" + ServiceIdentity + '\'' +
				", PassiveIp='" + PassiveIp + '\'' + ", PassivePort=" + PassivePort + ", ExtraInfo=" + ExtraInfo + '}';
	}
}
