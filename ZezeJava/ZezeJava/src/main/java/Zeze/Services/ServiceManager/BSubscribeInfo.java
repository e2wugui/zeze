package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import org.jspecify.annotations.Nullable;

/** 订阅信息bean：服务名、订阅过滤版本（0=全部版本）与本地附着状态。 */
public final class BSubscribeInfo implements Serializable {
	private String serviceName;
	private long version;
	private transient @Nullable Object localState;

	public BSubscribeInfo(IByteBuffer bb) {
		serviceName = "";
		decode(bb);
	}

	public BSubscribeInfo(String name) {
		this(name, 0, null);
	}

	public BSubscribeInfo(String name, long version) {
		this(name, version, null);
	}

	public BSubscribeInfo(String name, long version, @Nullable Object state) {
		serviceName = name;
		this.version = version;
		localState = state;
	}

	public String getServiceName() {
		return serviceName;
	}

	public long getVersion() {
		return version;
	}

	public @Nullable Object getLocalState() {
		return localState;
	}

	public void setLocalState(@Nullable Object value) {
		localState = value;
	}

	@Override
	public void decode(IByteBuffer bb) {
		serviceName = bb.ReadString();
		version = bb.ReadLong();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(serviceName);
		bb.WriteLong(version);
	}

	@Override
	public int hashCode() {
		return serviceName.hashCode() * 31 + Long.hashCode(version);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		var that = (BSubscribeInfo)o;
		return version == that.version && serviceName.equals(that.serviceName);
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
	public String toString() {
		return serviceName + ':' + version;
	}
}
