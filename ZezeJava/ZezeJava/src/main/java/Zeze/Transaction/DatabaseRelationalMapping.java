package Zeze.Transaction;

import java.util.Map;

/** 关系映射数据库接口：打开关系表并提供类型映射与 string key 列类型。 */
public interface DatabaseRelationalMapping {
	Database.Table openRelationalTable(String name);
	Map<String, String> getSqlTypeMap();

	// 提供默认值，一般关系数据库都有这个。
	default String getKeyStringType() {
		return "VARCHAR(256)";
	}
}
