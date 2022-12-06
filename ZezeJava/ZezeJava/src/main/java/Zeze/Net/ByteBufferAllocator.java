package Zeze.Net;

import java.nio.ByteBuffer;

public interface ByteBufferAllocator {
	public static final int DEFAULT_SIZE = 32 * 1024;

	default ByteBuffer alloc() {
		return ByteBuffer.allocateDirect(DEFAULT_SIZE);
	}

	default void free(@SuppressWarnings("unused") ByteBuffer bb) {
	}
}
