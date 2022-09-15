package Zeze.Serialize;

public interface Serializable {
	void encode(ByteBuffer bb);

	void decode(ByteBuffer bb);

	@Deprecated // 暂时兼容
	default void Encode(ByteBuffer bb) {
		encode(bb);
	}

	@Deprecated // 暂时兼容
	default void Decode(ByteBuffer bb) {
		decode(bb);
	}

	default int preAllocSize() {
		return 16;
	}

	default void preAllocSize(int size) {
	}
}
