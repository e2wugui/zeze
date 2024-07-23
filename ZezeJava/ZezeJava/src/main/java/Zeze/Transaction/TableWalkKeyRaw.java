package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TableWalkKeyRaw {
	boolean handle(byte @NotNull [] key) throws Exception;
}
