package Zeze.History;

import org.jetbrains.annotations.NotNull;

public interface IApplyDatabase {
	@NotNull IApplyTable open(@NotNull String tableName);
}
