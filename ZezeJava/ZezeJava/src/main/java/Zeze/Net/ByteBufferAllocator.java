package Zeze.Net;

import java.nio.ByteBuffer;

/**
 * NIO ByteBuffer 的分配/回收接口（供缓冲池实现）。
 */
public interface ByteBufferAllocator {
	ByteBuffer alloc();

	void free(ByteBuffer bb);
}
