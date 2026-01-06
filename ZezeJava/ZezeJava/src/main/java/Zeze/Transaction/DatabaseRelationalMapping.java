package Zeze.Transaction;

import java.util.Map;
import org.jetbrains.annotations.NotNull;

public interface DatabaseRelationalMapping {
	@NotNull Database.Table openRelationalTable(@NotNull String name);
	Map<String, String> getSqlTypeMap();

	// 提供默认值，一般关系数据库都有这个。
	default String getKeyStringType() {
		return "VARCHAR(256)";
	}
}
