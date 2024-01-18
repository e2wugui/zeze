package Zeze.Net;

import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * 用来接收 Codec 结果。
 */
public final class BufferCodec extends ByteBuffer implements Codec {
	public BufferCodec() {
		super(1024);
	}

	public BufferCodec(int capacity) {
		super(capacity);
	}

	public BufferCodec(@NotNull ByteBuffer buffer) {
		super(buffer.Bytes, buffer.ReadIndex, buffer.WriteIndex);
	}

	public @NotNull ByteBuffer getBuffer() {
		return this;
	}

	@Override
	public void update(byte c) {
		WriteByte(c);
	}

	@Override
	public void update(byte @NotNull [] data, int off, int len) {
		Append(data, off, len);
	}
}
