package Zeze.Net;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;

// 非线程安全,通常只能在selector线程调用
public final class OutputBuffer implements Codec, Closeable {
	private final ByteBufferAllocator allocator;
	private final ArrayDeque<ByteBuffer> buffers = new ArrayDeque<>();
	private final ByteBuffer[] outputs = new ByteBuffer[2];
	private ByteBuffer head, tail; // head <- buffers <- tail
	private int tailPos;
	private int size;

	public OutputBuffer(ByteBufferAllocator allocator) {
		this.allocator = allocator;
		tail = allocator.alloc();
	}

	@Override
	public void close() {
		if (head != null) {
			allocator.free(head);
			head = null;
		}
		for (ByteBuffer bb; (bb = buffers.pollFirst()) != null; )
			allocator.free(bb);
		allocator.free(tail);
		tail = null;
	}

	public int size() {
		return size;
	}

	public void put(byte[] src) {
		put(src, 0, src.length);
	}

	public void put(byte[] src, int offset, int length) {
		if (length > 0) {
			for (size += length; ; ) {
				var tail = this.tail;
				int left = tail.remaining();
				if (left >= length) {
					tail.put(src, offset, length);
					break;
				}
				tail.put(src, offset, left);
				pushAndAllocTail();
				offset += left;
				length -= left;
			}
		}
	}

	public void put(byte b) {
		size++;
		int left = tail.remaining();
		if (left <= 0)
			pushAndAllocTail();
		tail.put(b);
	}

	private void pushAndAllocTail() {
		var tail = this.tail;
		tail.limit(tail.position());
		tail.position(tailPos);
		buffers.addLast(tail);
		this.tail = allocator.alloc();
		tailPos = 0;
	}

	public long writeTo(SocketChannel channel) throws IOException {
		long r;
		var head = this.head;
		if (head == null && (head = buffers.pollFirst()) == null) { // head和队列都没有buffer了,只需要输出tail
			var tail = this.tail;
			int writePos = tail.position();
			if (writePos <= tailPos) // tail没有数据
				return 0;
			tail.limit(writePos);
			tail.position(tailPos);
			r = channel.write(tail);
			int newTailPos = tail.position();
			if (newTailPos >= writePos) // tail全部输出完
				newTailPos = writePos = 0;
			tailPos = newTailPos;
			tail.position(writePos);
			tail.limit(tail.capacity());
		} else {
			this.head = head;
			var next = buffers.peekFirst();
			if (next == null) {
				var tail = this.tail;
				if (tail.position() == 0) { // 队列只有对头,且tail没有数据
					r = channel.write(head);
					if (!head.hasRemaining()) { // 队头已经输出完
						allocator.free(head);
						this.head = null;
					}
				} else { // 队列只有对头,且tail有数据
					outputs[0] = head;
					outputs[1] = tail;
					int writePos = tail.position();
					tail.limit(writePos);
					tail.position(0);
					r = channel.write(outputs);
					if (!head.hasRemaining()) { // 队头已经输出完
						allocator.free(head);
						this.head = null;
						int newTailPos = tail.position();
						if (newTailPos >= writePos) // tail全部输出完
							newTailPos = writePos = 0;
						tailPos = newTailPos;
						tail.position(writePos);
					} else
						tail.position(writePos);
					tail.limit(tail.capacity());
				}
			} else { // 输出head和队头的2个buffers
				outputs[0] = head;
				outputs[1] = next;
				r = channel.write(outputs);
				if (!head.hasRemaining()) { // 第1个输出完了
					allocator.free(head);
					this.head = null;
					if (!next.hasRemaining()) { // 第2个也输出完了
						allocator.free(next);
						buffers.removeFirst();
					}
				}
			}
		}
		if (r > 0)
			size -= r;
		return r;
	}

	@Override
	public void update(byte c) {
		put(c);
	}

	@Override
	public void update(byte[] data, int off, int len) {
		put(data, off, len);
	}

	@Override
	public void flush() {
	}
}
