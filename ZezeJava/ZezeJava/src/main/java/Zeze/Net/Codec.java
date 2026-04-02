package Zeze.Net;

import java.io.Closeable;
import org.jetbrains.annotations.NotNull;

public interface Codec extends Closeable {
	void update(byte c) throws CodecException;

	/**
	 * @param data 方法外绝对不能持有data引用! 也就是只能在方法内读data里的数据
	 */
	default void update(byte @NotNull [] data, int off, int len) throws CodecException {
		for (len += off; off < len; off++)
			update(data[off]);
	}

	default void update(byte @NotNull [] data) throws CodecException {
		update(data, 0, data.length);
	}

	default void flush() throws CodecException {
	}

	@Override
	default void close() {
	}
}
