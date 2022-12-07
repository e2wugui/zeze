package Zeze.Net;

import java.nio.ByteBuffer;

public interface ByteBufferAllocator {
	ByteBuffer alloc();

	void free(ByteBuffer bb);
}
