package Zeze.Serialize;

import java.util.ArrayList;

/** 可编码对象：支持写入自身的二进制与 SQL 表示。 */
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
