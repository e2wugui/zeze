package Zeze.Serialize;

public interface Serializable {
	void Encode(ByteBuffer bb);

	void Decode(ByteBuffer bb);

	default int getPreAllocSize() {
		return 16;
	}

	default void setPreAllocSize(int size) {
	}
}
