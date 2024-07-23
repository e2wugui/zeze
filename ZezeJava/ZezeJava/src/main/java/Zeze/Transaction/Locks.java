package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

public class Locks extends Zeze.Util.Locks<Lockey> {
	public @NotNull Lockey get(@NotNull TableKey tKey) {
		return super.get(new Lockey(tKey));
	}

	public boolean contains(@NotNull TableKey tKey) {
		return super.contains(new Lockey(tKey));
	}
}
