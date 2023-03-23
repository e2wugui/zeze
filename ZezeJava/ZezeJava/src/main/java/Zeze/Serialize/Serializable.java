package Zeze.Serialize;

import java.sql.SQLException;
import java.util.ArrayList;

public interface Serializable {
	void encode(ByteBuffer bb);

	void decode(ByteBuffer bb);

	default int preAllocSize() {
		return 16;
	}

	default void preAllocSize(int size) {
	}

	default void decodeResultSet(ArrayList<String> parents, java.sql.ResultSet rs) throws SQLException {
		throw new UnsupportedOperationException();
	}

	default void encodeSQLStatement(ArrayList<String> parents, SQLStatement st) {
		throw new UnsupportedOperationException();
	}
}
