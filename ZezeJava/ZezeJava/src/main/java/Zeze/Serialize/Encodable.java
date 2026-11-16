package Zeze.Serialize;

import java.util.ArrayList;

/** A value that can write its existing binary and SQL representations. */
public interface Encodable {
	void encode(ByteBuffer bb);

	default long typeId() {
		return 0;
	}

	default int preAllocSize() {
		return 16;
	}

	default void preAllocSize(int size) {
	}

	default void encodeSQLStatement(ArrayList<String> parents, SQLStatement st) {
		throw new UnsupportedOperationException();
	}
}
