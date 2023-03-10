package Zeze.Serialize;

public interface Serializable {
	void encode(ByteBuffer bb);

	void decode(ByteBuffer bb);

	default int preAllocSize() {
		return 16;
	}

	default void preAllocSize(int size) {
	}
}
