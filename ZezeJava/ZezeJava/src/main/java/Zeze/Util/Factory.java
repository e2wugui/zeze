package Zeze.Util;

import org.jetbrains.annotations.NotNull;

/** 工厂产出值：null即违约（需要"可能缺席"语义用Optional或哨兵对象表达，不用null）。 */
@FunctionalInterface
public interface Factory<T> {
	@NotNull T create();
}
