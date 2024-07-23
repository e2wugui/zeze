package Zeze.Net;

import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public interface ByteBufferAllocator {
	@NotNull ByteBuffer alloc();

	void free(@NotNull ByteBuffer bb);
}
