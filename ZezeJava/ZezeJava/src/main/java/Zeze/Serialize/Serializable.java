package Zeze.Serialize;

import java.sql.SQLException;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

/** 可序列化对象：定义 Zeze 二进制编解码（encode/decode）及 SQL 装卸载的公共协议。 */
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
