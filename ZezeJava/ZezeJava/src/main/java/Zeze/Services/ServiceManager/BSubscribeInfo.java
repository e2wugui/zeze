package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BSubscribeInfo extends Bean {
	private @NotNull String serviceName = "";
	private long version;
	private @Nullable Object localState;

	public BSubscribeInfo(@NotNull IByteBuffer bb) {
		decode(bb);
	}

	public BSubscribeInfo(@NotNull String name) {
		serviceName = name;
	}

	public BSubscribeInfo(@NotNull String name, long version) {
		serviceName = name;
		this.version = version;
	}

	public BSubscribeInfo(@NotNull String name, long version, @Nullable Object state) {
		serviceName = name;
		this.version = version;
		localState = state;
	}

	public long getVersion() {
		return version;
	}

	public @NotNull String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String value) {
		serviceName = value;
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
		this.version = bb.ReadLong();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteString(serviceName);
		bb.WriteLong(version);
	}

	@Override
	public @NotNull String toString() {
		return serviceName;
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
}
