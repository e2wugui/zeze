package Zeze.Net;

public interface Codec {
	void update(byte c) throws CodecException;

	/**
	 * @param data 方法外绝对不能持有data引用! 也就是只能在方法内读data里的数据
	 */
	void update(byte[] data, int off, int len) throws CodecException;

	void flush() throws CodecException;
}
