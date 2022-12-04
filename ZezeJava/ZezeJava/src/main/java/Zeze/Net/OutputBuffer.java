package Zeze.Net;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;

/**
 * 输出专用缓冲
 *
 * <p>
 * 使用 ByteBuffer 当作输出缓冲的操作系列：flip, write, compact。 其中 compact
 * 操作需要把未写完的数据移动到开头，当缓冲比较大的时，影响效率。
 * 
 * <p>
 * 这个类实现了输出缓冲所需的最基本操作，尽量避免大块内存拷贝。基本操作如下：
 * <ol>
 * <li>put 操作：在缓冲中添加数据
 * <li>output 操作：把数据写到网络流中.
 * <li>get 操作：从缓冲中提取数据
 * </ol>
 */
public final class OutputBuffer {
	public static final int DEFAULT_PIECE = 524288; // default piece = 512k
	public static final int MAX_SYSTEM_PIECE = 2 * 1024 * 1024; // 2M
	private int size = 0; // number of bytes in this buffer
	private final int piece; // size of piece

	private final ArrayDeque<ByteBuffer> outputs = new ArrayDeque<ByteBuffer>(); // ready for get. no compact.
	private ByteBuffer tail = null; // ready for put. have some data.
	private final ArrayDeque<ByteBuffer> buffers = new ArrayDeque<ByteBuffer>(); // ready for put. is empty.

	public OutputBuffer() {
		this(DEFAULT_PIECE);
	}

	public OutputBuffer(int piece) {
		this.piece = Helper.roudup(piece);
	}

	/**
	 * This method transfers the bytes remaining in the given source buffer into
	 * this buffer.
	 * 
	 * @see java.nio.ByteBuffer
	 * @param src ByteBuffer. MUST NOT BE DIRECT
	 * @return this
	 */
	public OutputBuffer put(ByteBuffer src) {
		this.size += src.remaining();
		while (src.remaining() > 0) {
			// allocate
			if (null == tail) {
				tail = buffers.pollFirst();
				if (null == tail)
					tail = ByteBuffer.allocateDirect(piece);
			}
			// check size and put
			int len = Math.min(tail.remaining(), src.remaining());
			if (tail.put((ByteBuffer) src.slice().limit(len)).remaining() == 0) {
				tail.flip(); // ready for output
				outputs.addLast(tail);
				tail = null;
			}
			src.position(src.position() + len); // fetch position
		}
		return this;
	}

	public OutputBuffer put(byte[] src) {
		return put(ByteBuffer.wrap(src, 0, src.length));
	}

	public OutputBuffer put(byte[] src, int offset, int length) {
		return put(ByteBuffer.wrap(src, offset, length));
	}

	/**
	 * This method transfers bytes from this buffer into the given destination
	 * channel.
	 * 
	 * @param channel channel
	 * @return this
	 * @throws IOException channel IOException
	 */
	public OutputBuffer output(SocketChannel channel) throws IOException {
		if (null != tail) {
			/*
			 * 此时 tail 可能没有满。有如下几种处理方式： 1 本次 output 忽略 tail 当 outputs
			 * 里面已经有足够的数据可输出时，先不处理tail。 这种方式需要定义多少数据才是足够的。比较麻烦。
			 *
			 * 2 处理并恢复tail 把 tail 加入到 outputs 中； write；输出到 channel 中。 tail =
			 * pollLast().compact(); 恢复tail。[目前采用这个方法。]
			 *
			 * 3 处理tail 处理但不恢复tail。参考2。 这种方法极限情况下，会导致outputs中的所有ByteBuffer都是未写满的，太浪费空间
			 */
			tail.flip(); // ready for output
			outputs.addLast(tail);
			tail = null;
		}

		// 由于每个分片相当大（足够大了），这里看起来没必要调用 toArray。
		// 这么写可以处理这样的情况：
		// 1. 第一个分片剩余数据很少时，一次调用就把后面的数据一起写出去。
		// 2. 由于 tail 的处理原则，分片是没有装满的。
		// 总的来说这个实现问题挺多，修改成使用一个循环，一个个分片的写（包括tail的处理）。

		var n = MAX_SYSTEM_PIECE / DEFAULT_PIECE;
		if (MAX_SYSTEM_PIECE % DEFAULT_PIECE > 0)
			++n;
		n = Math.min(n, outputs.size());
		if (n == 0)
			return this; // done
		var outputBuffers = new ByteBuffer[n];
		var i = 0;
		for (var it = outputs.iterator(); i < n && it.hasNext(); /* nothing */)
			outputBuffers[i++] = it.next();

		this.size -= channel.write(outputBuffers);
		//this.size -= channel.write(outputs.toArray(new ByteBuffer[outputs.size()]));
		// collect empty piece
		while (!outputs.isEmpty()) {
			if (outputs.peekFirst().remaining() > 0) {
				tail = outputs.pollLast().compact(); // 恢复tail。
				break;
			}
			buffers.addLast(outputs.pollFirst().clear());
		}
		return this;
	}

	/**
	 * This method transfers bytes from this buffer into the given destination
	 * array.
	 * 
	 * @throws BufferUnderflowException If there are fewer than <tt>length</tt>
	 *                                  bytes remaining in this buffer
	 */
	public OutputBuffer get(byte[] dst, int offset, int length) {
		Helper.checkBounds(offset, length, dst.length);
		if (this.size < length)
			throw new BufferUnderflowException();

		this.size -= length;
		while (length > 0) {
			ByteBuffer first = outputs.peekFirst();
			if (null == first)
				break;
			int r = first.remaining();
			if (r > length)
				r = length;
			if (first.get(dst, offset, r).remaining() == 0)
				// outputs.removeFirst();
				buffers.add(outputs.pollFirst().flip());
			offset += r;
			length -= r;
		}
		if (length > 0) {
			// 开头已经检查过大小，而数据没有取够，意味着还有一些在 tail 中，此时 tail 肯定不是 null。
			tail.flip();
			tail.get(dst, offset, length);
			tail.compact();
		}
		return this;
	}

	public OutputBuffer get(byte[] dst) {
		return get(dst, 0, dst.length);
	}

	/**
	 * @return The number of bytes in this buffer
	 */
	public int size() {
		return this.size;
	}

	/**
	 * same as size()
	 * 
	 * @return The number of bytes in this buffer
	 */
	public int position() {
		return this.size;
	}

	/**
	 * @return size of piece
	 */
	public int piece() {
		return this.piece;
	}

	/**
	 * @return 当前已分配的内存容量。
	 */
	public int allocation() {
		int n = outputs.size() + buffers.size();
		if (null != tail)
			++n;
		return n * piece;
	}
}
