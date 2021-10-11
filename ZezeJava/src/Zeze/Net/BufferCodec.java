package Zeze.Net;

import java.io.*;

/** 
 用来接收 Codec 结果。
*/
public final class BufferCodec implements Codec {
	private Zeze.Serialize.ByteBuffer Buffer = Zeze.Serialize.ByteBuffer.Allocate();
	public Zeze.Serialize.ByteBuffer getBuffer() {
		return Buffer;
	}

	public BufferCodec() {

	}

	public BufferCodec(Zeze.Serialize.ByteBuffer buffer) {
		Buffer = buffer;
	}

	public void close() throws IOException {
	}

	public void flush() {
	}

	public void update(byte c) {
		getBuffer().Append(c);
	}

	public void update(byte[] data, int off, int len) {
		getBuffer().Append(data, off, len);
	}
}