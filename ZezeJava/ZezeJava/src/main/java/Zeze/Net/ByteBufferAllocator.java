package Zeze.Net;

import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * NIO ByteBuffer 的分配/回收接口（供缓冲池实现）。
 */
public interface ByteBufferAllocator {
	@NotNull ByteBuffer alloc();

	void free(@NotNull ByteBuffer bb);
}
