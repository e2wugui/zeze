package Zeze.Net;

import Zeze.Serialize.ByteBuffer;

/**
 * 用来接收 Codec 结果。
 */
public final class BufferCodec implements Codec {
	private final ByteBuffer buffer;

	public BufferCodec() {
		buffer = ByteBuffer.Allocate(1024);
	}

	public BufferCodec(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	@Override
	public void update(byte c) {
		buffer.Append(c);
	}

	@Override
	public void update(byte[] data, int off, int len) {
		buffer.Append(data, off, len);
	}

	@Override
	public void flush() {
	}

	public void close() {
	}
}
