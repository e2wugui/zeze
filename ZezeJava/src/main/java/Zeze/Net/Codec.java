package Zeze.Net;

public interface Codec {
	void update(byte c) throws CodecException;

	void update(byte[] data, int off, int len) throws CodecException;

	void flush() throws CodecException;
}
