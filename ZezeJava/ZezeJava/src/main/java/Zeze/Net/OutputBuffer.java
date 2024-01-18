package Zeze.Net;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.util.ArrayDeque;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 非线程安全,通常只能在selector线程调用
public final class OutputBuffer implements Codec, Closeable {
	private final @NotNull ByteBufferAllocator allocator;
	private final ArrayDeque<ByteBuffer> buffers = new ArrayDeque<>();
	private final ByteBuffer[] outputs = new ByteBuffer[2];
	private @Nullable ByteBuffer head, tail; // head <- buffers <- tail
	private int tailPos;
	private int size;

	public OutputBuffer(@NotNull ByteBufferAllocator allocator) {
		this.allocator = allocator;
	}

	@Override
	public void close() {
		if (head != null) {
			allocator.free(head);
			head = null;
		}
		for (ByteBuffer bb; (bb = buffers.pollFirst()) != null; )
			allocator.free(bb);
		if (tail != null) {
			allocator.free(tail);
			tail = null;
			tailPos = 0;
		}
		size = 0;
	}

	public int getBufferCount() {
		int bufCount = buffers.size();
		if (bufCount > 0)
			bufCount++; // add tail
		else if (tail != null)
			bufCount = 1;
		if (head != null)
			bufCount++;
		return bufCount;
	}

	public int size() {
		return size;
	}

	public void put(byte @NotNull [] src) {
		put(src, 0, src.length);
	}

	public void put(byte @NotNull [] src, int offset, int length) {
		if (length > 0) {
			var tail = this.tail;
			if (tail == null)
				this.tail = tail = allocator.alloc();
			for (size += length; ; ) {
				int left = tail.remaining();
				if (left >= length) {
					tail.put(src, offset, length);
					break;
				}
				tail.put(src, offset, left);
				tail = pushAndAllocTail();
				offset += left;
				length -= left;
			}
		}
	}

	public void put(byte b) {
		size++;
		var tail = this.tail;
		if (tail == null)
			this.tail = tail = allocator.alloc();
		else if (tail.remaining() <= 0)
			tail = pushAndAllocTail();
		tail.put(b);
	}

	private @NotNull ByteBuffer pushAndAllocTail() {
		var tail = this.tail;
		//noinspection DataFlowIssue
		tail.limit(tail.position());
		tail.position(tailPos);
		buffers.addLast(tail);
		this.tail = tail = allocator.alloc();
		tailPos = 0;
		return tail;
	}

	public long writeTo(@NotNull GatheringByteChannel channel) throws IOException {
		long r;
		var head = this.head;
		if (head == null && (head = buffers.pollFirst()) == null) { // head和队列都没有buffer了,只需要输出tail
			var tail = this.tail;
			if (tail == null)
				return 0; // tail没有数据
			int writePos = tail.position();
			tail.limit(writePos);
			tail.position(tailPos);
			r = channel.write(tail);
			int newTailPos = tail.position();
			if (newTailPos >= writePos) { // tail全部输出完
				this.tail = null;
				tailPos = 0;
				allocator.free(tail);
			} else {
				tailPos = newTailPos;
				tail.position(writePos);
				tail.limit(tail.capacity());
			}
		} else {
			this.head = head;
			var next = buffers.peekFirst();
			if (next == null) {
				var tail = this.tail; // 队列不空时,tail肯定不为null
				outputs[0] = head;
				outputs[1] = tail;
				//noinspection DataFlowIssue
				int writePos = tail.position();
				tail.limit(writePos);
				tail.position(0);
				r = channel.write(outputs);
				if (!head.hasRemaining()) { // 队头已经输出完
					allocator.free(head);
					this.head = null;
					int newTailPos = tail.position();
					if (newTailPos >= writePos) { // tail全部输出完
						this.tail = null;
						tailPos = 0;
						allocator.free(tail);
					} else {
						tailPos = newTailPos;
						tail.position(writePos);
						tail.limit(tail.capacity());
					}
				} else {
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
			size -= (int)r;
		return r;
	}

	@Override
	public void update(byte c) {
		put(c);
	}

	@Override
	public void update(byte @NotNull [] data, int off, int len) {
		put(data, off, len);
	}
}
