package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

/** 记录锁池：按 TableKey 获取/复用 Lockey，保证相同键全进程拿到同一把锁。 */
public class Locks extends Zeze.Util.Locks<Lockey> {
	public @NotNull Lockey get(@NotNull TableKey tKey) {
		return super.get(new Lockey(tKey));
	}

	public boolean contains(@NotNull TableKey tKey) {
		return super.contains(new Lockey(tKey));
	}
}
