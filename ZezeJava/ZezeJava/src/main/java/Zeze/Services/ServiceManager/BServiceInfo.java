package Zeze.Services.ServiceManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import org.jspecify.annotations.Nullable;

/**
 * 服务信息bean：服务名+identity（equals/key语义）、版本、被动地址与扩展信息。
 */
public final class BServiceInfo implements Serializable, Comparable<BServiceInfo> {
	/**
	 * 服务名，比如"GameServer"
	 */
	private String serviceName = "";

	/**
	 * 服务id，对于 Zeze.Application，一般就是 Config.ServerId.
	 * 这里使用类型 string 是为了更好的支持扩展。
	 */
	private String serviceIdentity = "";
	private long version;

	/**
	 * 服务ip-port，如果没有，保持空和0.
	 */
	private String passiveIp = "";
	private int passivePort;

	// 服务扩展信息，可选。
	private Binary extraInfo = Binary.Empty;

	// ServiceManager用来存储服务器的SessionId。算是一个优化吧。
	private transient @Nullable Long sessionId;

	public String getServiceName() {
		return serviceName;
	}

	public String getServiceIdentity() {
		return serviceIdentity;
	}

	public String getPassiveIp() {
		return passiveIp;
	}

	public @Nullable Long getSessionId() {
		return sessionId;
	}

	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}

	public long getVersion() {
		return version;
	}

	public void setPassiveIp(String value) {
		passiveIp = value;
	}

	public int getPassivePort() {
		return passivePort;
	}

	public void setPassivePort(int value) {
		passivePort = value;
	}

	public Binary getExtraInfo() {
		return extraInfo;
	}

	public void setExtraInfo(@Nullable Binary value) {
		extraInfo = value != null ? value : Binary.Empty;
	}

	public BServiceInfo(IByteBuffer bb) {
		decode(bb);
	}

	public BServiceInfo(String name, String identity) {
		this(name, identity, 0, null, 0, null);
	}

	public BServiceInfo(String name, String identity, long version) {
		this(name, identity, version, null, 0, null);
	}

	public BServiceInfo(String name, String identity, long version, @Nullable String ip) {
		this(name, identity, version, ip, 0, null);
	}

	public BServiceInfo(String name, String identity, long version, @Nullable String ip, int port) {
		this(name, identity, version, ip, port, null);
	}

	public BServiceInfo(String name, String identity, long version, @Nullable String ip, int port,
						@Nullable Binary extraInfo) {
		serviceName = name;
		serviceIdentity = identity;
		this.version = version;
		if (ip != null)
			passiveIp = ip;
		passivePort = port;
		if (extraInfo != null)
			this.extraInfo = extraInfo;
	}

	@Override
	public void decode(IByteBuffer bb) {
		serviceName = bb.ReadString();
		serviceIdentity = bb.ReadString();
		version = bb.ReadLong();
		passiveIp = bb.ReadString();
		passivePort = bb.ReadInt();
		extraInfo = bb.ReadBinary();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(serviceName);
		bb.WriteString(serviceIdentity);
		bb.WriteLong(version);
		bb.WriteString(passiveIp);
		bb.WriteInt(passivePort);
		bb.WriteBinary(extraInfo);
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
	public int hashCode() {
		return serviceName.hashCode() * 31 + serviceIdentity.hashCode();
	}

	/**
	 * 比较完整信息是否相等(除了sessionId)
	 */
	public boolean fullEquals(BServiceInfo other) {
		return serviceName.equals(other.serviceName)
				&& serviceIdentity.equals(other.serviceIdentity)
				&& version == other.version
				&& passiveIp.equals(other.passiveIp)
				&& passivePort == other.passivePort
				&& extraInfo.equals(other.extraInfo);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof BServiceInfo other) {
			// 版本分类存储，同一个集合的版本肯定相等，这里不需要判断版本。
			return serviceName.equals(other.serviceName) && serviceIdentity.equals(other.serviceIdentity);
		}
		return false;
	}

	@Override
	public int compareTo(BServiceInfo o) {
		int c = serviceName.compareTo(o.serviceName);
		return c != 0 ? c : serviceIdentity.compareTo(o.serviceIdentity);
	}

	@Override
	public String toString() {
		return "BServiceInfo{serviceName='" + serviceName
				+ "', serviceIdentity='" + serviceIdentity
				+ "', version=" + version
				+ ", passiveIp='" + passiveIp
				+ "', passivePort=" + passivePort
				+ ", extraInfo=" + extraInfo + '}';
	}
}
