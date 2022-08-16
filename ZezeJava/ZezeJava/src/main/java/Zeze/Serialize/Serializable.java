package Zeze.Serialize;

public interface Serializable {
	void Encode(ByteBuffer bb);

	void Decode(ByteBuffer bb);

	default int preAllocSize() {
		return 16;
	}

	default void preAllocSize(int size) {
	}
}
