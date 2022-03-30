package Zeze.Net;

import Zeze.Serialize.ByteBuffer;

/**
 * 用来接收 Codec 结果。
 */
public final class BufferCodec implements Codec {
	private final ByteBuffer Buffer;

	public BufferCodec() {
		Buffer = ByteBuffer.Allocate(1024);
	}

	public BufferCodec(ByteBuffer buffer) {
		Buffer = buffer;
	}

	public ByteBuffer getBuffer() {
		return Buffer;
	}

	@Override
	public void update(byte c) {
		Buffer.Append(c);
	}

	@Override
	public void update(byte[] data, int off, int len) {
		Buffer.Append(data, off, len);
	}

	@Override
	public void flush() {
	}

	public void close() {
	}
}
