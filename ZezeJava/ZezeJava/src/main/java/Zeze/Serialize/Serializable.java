package Zeze.Serialize;

import java.sql.SQLException;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

public interface Serializable {
	void encode(@NotNull ByteBuffer bb);

	void decode(@NotNull IByteBuffer bb);

	default long typeId() {
		return 0;
	}

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
