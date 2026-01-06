package Zeze.Transaction;

import java.util.Map;
import org.jetbrains.annotations.NotNull;

public interface DatabaseRelationalMapping {
	@NotNull Database.Table openRelationalTable(@NotNull String name);
	Map<String, String> getSqlTypeMap();
}
