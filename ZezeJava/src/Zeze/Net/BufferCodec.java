package Zeze.Net;

import Zeze.Serialize.*;
import Zeze.*;
import java.io.*;

/** 
 用来接收 Codec 结果。
*/
public final class BufferCodec implements Codec {
	private Serialize.ByteBuffer Buffer = Serialize.ByteBuffer.Allocate();
	public Serialize.ByteBuffer getBuffer() {
		return Buffer;
	}

	public BufferCodec() {

	}

	public BufferCodec(Serialize.ByteBuffer buffer) {
		Buffer = buffer;
	}

	public void close() throws IOException {
	}

	public void flush() {
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void update(byte c)
	public void update(byte c) {
		getBuffer().Append(c);
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void update(byte[] data, int off, int len)
	public void update(byte[] data, int off, int len) {
		getBuffer().Append(data, off, len);
	}
}