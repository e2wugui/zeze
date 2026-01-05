package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

public interface DatabaseRelationalMapping {
	@NotNull Database.Table openRelationalTable(@NotNull String name);
}
