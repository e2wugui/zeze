package UnitTest.Zeze.Net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import Zeze.Net.ByteBufferAllocator;
import Zeze.Net.OutputBuffer;
import Zeze.Util.IdentityHashSet;
import junit.framework.TestCase;
import org.junit.Assert;

/**
 * 100%覆盖测试OutputBuffer,随机暴力黑盒测试验证
 */
public class TestOutputBuffer extends TestCase {
	static class Alloc implements ByteBufferAllocator {
		final IdentityHashSet<ByteBuffer> allocSet = new IdentityHashSet<>();
		final int cap;
		int allocCount;

		Alloc(int cap) {
			Assert.assertTrue(cap > 0);
			this.cap = cap;
		}

		public int getInUseCount() {
			return allocSet.size();
		}

		@Override
		public ByteBuffer alloc() {
			var bb = ByteBuffer.allocate(cap);
			Assert.assertNotNull(bb);
			allocSet.add(bb);
			allocCount++;
			return bb;
		}

		@Override
		public void free(ByteBuffer bb) {
			Assert.assertNotNull(bb);
			Assert.assertTrue(allocSet.remove(bb));
		}
	}

	static class Channel implements GatheringByteChannel {
		final Zeze.Serialize.ByteBuffer sink = Zeze.Serialize.ByteBuffer.Allocate();
		int remaining;

		public int getRemaining() {
			return remaining;
		}

		public void setRemaining(int remaining) {
			this.remaining = remaining;
		}

		public Zeze.Serialize.ByteBuffer getSink() {
			return sink;
		}

		@Override
		public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
			int left = remaining;
			sink.EnsureWrite(left);
			for (int i = 0; i < length && left > 0; i++) {
				var bb = srcs[offset + i];
				int n = bb.remaining();
				bb.get(sink.Bytes, sink.WriteIndex, Math.min(left, n));
				n -= bb.remaining();
				sink.WriteIndex += n;
				left -= n;
			}
			int n = remaining - left;
			remaining = left;
			return n;
		}

		@Override
		public long write(ByteBuffer[] srcs) throws IOException {
			return write(srcs, 0, srcs.length);
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			int n = src.remaining();
			sink.EnsureWrite(remaining);
			src.get(sink.Bytes, sink.WriteIndex, Math.min(remaining, n));
			n -= src.remaining();
			sink.WriteIndex += n;
			remaining -= n;
			return n;
		}

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public void close() {
		}
	}

	public void test() throws IOException {
		long seed = System.currentTimeMillis();
		System.out.println(getClass().getSimpleName() + ": seed = " + seed);
		var r = new java.util.Random(seed);
		for (int cap = 1; cap <= 4; cap++) {
			var a = new Alloc(cap);
			var ob = new OutputBuffer(a);
			var bb = Zeze.Serialize.ByteBuffer.Allocate();

			var c = new Channel();
			for (int i = 0; i < 1_000_000; i++) {
				switch (r.nextInt(4)) {
				case 0:
					var b = (byte)r.nextInt();
					if (r.nextBoolean())
						ob.put(b);
					else
						ob.update(b);
					bb.WriteByte(b);
					break;
				case 1:
					var bs = new byte[r.nextInt(16)];
					r.nextBytes(bs);
					if (r.nextBoolean())
						ob.put(bs);
					else
						ob.update(bs);
					bb.Append(bs);
					break;
				case 2:
					c.setRemaining(r.nextInt(16));
					//noinspection StatementWithEmptyBody
					while (ob.writeTo(c) > 0 && c.getRemaining() > 0)
						;
					break;
				case 3:
					var n = ob.getBufferCount();
					if (ob.size() > 0)
						assertTrue(n > 0);
					else
						assertEquals(0, n);
					break;
				}
			}
			if (ob.size() > 0) {
				c.setRemaining(ob.size());
				//noinspection StatementWithEmptyBody
				while (ob.writeTo(c) > 0 && c.getRemaining() > 0)
					;
			}

			Assert.assertEquals(0, a.getInUseCount());
			Assert.assertEquals(0, ob.size());
			Assert.assertEquals(0, ob.getBufferCount());
			Assert.assertEquals(bb.size(), c.getSink().size());
			Assert.assertEquals(bb, c.getSink());

			ob.put(new byte[20]);
			c.setRemaining(7);
			//noinspection StatementWithEmptyBody
			while (ob.writeTo(c) > 0 && c.getRemaining() > 0)
				;
			Assert.assertEquals(13, ob.size());
			ob.close();
			Assert.assertEquals(0, ob.size());
			Assert.assertEquals(0, ob.getBufferCount());
		}
	}
}
