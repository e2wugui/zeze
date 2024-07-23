package Zeze.Util;

import org.jetbrains.annotations.NotNull;

public interface Lockey<Subclass> extends Comparable<Subclass> {
	@NotNull Subclass alloc();
}
