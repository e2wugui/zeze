package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BSubscribeInfo implements Serializable {
	private @NotNull String serviceName;
	private long version;
	private transient @Nullable Object localState;

	public BSubscribeInfo(@NotNull IByteBuffer bb) {
		serviceName = "";
		decode(bb);
	}

	public BSubscribeInfo(@NotNull String name) {
		this(name, 0, null);
	}

	public BSubscribeInfo(@NotNull String name, long version) {
		this(name, version, null);
	}

	public BSubscribeInfo(@NotNull String name, long version, @Nullable Object state) {
		serviceName = name;
		this.version = version;
		localState = state;
	}

	public @NotNull String getServiceName() {
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
	public void decode(@NotNull IByteBuffer bb) {
		serviceName = bb.ReadString();
		version = bb.ReadLong();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
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
	public @NotNull String toString() {
		return serviceName + ':' + version;
	}
}
