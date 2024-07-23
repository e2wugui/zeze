package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TableWalkHandleRaw {
	boolean handle(byte @NotNull [] key, byte @NotNull [] value) throws Exception;
}
