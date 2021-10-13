package Zeze.Services.ServiceManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;

public final class ServiceInfo extends Zeze.Transaction.Bean implements Comparable<ServiceInfo>{
	/** 
	 服务名，比如"GameServer"
	*/
	private String ServiceName;
	public String getServiceName() {
		return ServiceName;
	}
	private void setServiceName(String value) {
		ServiceName = value;
	}

	/** 
	 服务id，对于 Zeze.Application，一般就是 Config.AutoKeyLocalId.
	 这里使用类型 string 是为了更好的支持扩展。
	*/
	private String ServiceIdentity;
	public String getServiceIdentity() {
		return ServiceIdentity;
	}
	private void setServiceIdentity(String value) {
		ServiceIdentity = value;
	}

	/** 
	 服务ip-port，如果没有，保持空和0.
	*/
	private String PassiveIp = "";
	public String getPassiveIp() {
		return PassiveIp;
	}
	private void setPassiveIp(String value) {
		PassiveIp = value;
	}
	private int PassivePort = 0;
	public int getPassivePort() {
		return PassivePort;
	}
	private void setPassivePort(int value) {
		PassivePort = value;
	}

	// 服务扩展信息，可选。
	private Binary ExtraInfo = Binary.Empty;
	public Binary getExtraInfo() {
		return ExtraInfo;
	}
	private void setExtraInfo(Binary value) {
		ExtraInfo = value;
	}

	// ServiceManager或者ServiceManager.Agent用来保存本地状态，不是协议一部分，不会被系列化。
	// 算是一个简单的策略，不怎么优美。一般仅设置一次，线程保护由使用者自己管理。
	private Object LocalState;
	public Object getLocalState() {
		return LocalState;
	}
	public void setLocalState(Object value) {
		LocalState = value;
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

	public ServiceInfo(String name, String identity, String ip, int port, Binary extrainfo) {
		setServiceName(name);
		setServiceIdentity(identity);
		if (!ip.equals(null)) {
			setPassiveIp(ip);
		}
		setPassivePort(port);
		if (extrainfo != null) {
			setExtraInfo(extrainfo);
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

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
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
}
