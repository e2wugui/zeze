package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.NioByteBuffer;
import Zeze.Transaction.Collections.PList1;
import Zeze.Transaction.Collections.PList2;
import Zeze.Transaction.Collections.PMap1;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSet1;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestCollectionDecodeCapacity {
	private static IByteBuffer countOnly(boolean nio, int count) {
		var bb = ByteBuffer.Allocate();
		bb.WriteUInt(count);
		var bytes = bb.Copy();
		return nio ? NioByteBuffer.Wrap(bytes, bytes.length) : ByteBuffer.Wrap(bytes);
	}

	private static void assertTruncated(Runnable decode) {
		// MAX_VALUE makes HotSpot reject the array length before consuming heap. Translate
		// that VM error to an ordinary assertion so JUnit can finish the other cases.
		assertThrows(IllegalStateException.class, () -> {
			try {
				decode.run();
			} catch (OutOfMemoryError e) {
				throw new AssertionError("untrusted count reached array allocation: " + e);
			}
		});
	}

	@Test
	void listValueRejectsTruncatedHugeCount() {
		for (boolean nio : new boolean[]{false, true})
			assertTruncated(() -> new PList1<>(Integer.class).decode(countOnly(nio, Integer.MAX_VALUE)));
	}

	@Test
	void listBeanRejectsTruncatedHugeCount() {
		for (boolean nio : new boolean[]{false, true})
			assertTruncated(() -> new PList2<>(EmptyBean.class).decode(countOnly(nio, Integer.MAX_VALUE)));
	}

	@Test
	void setRejectsTruncatedHugeCount() {
		for (boolean nio : new boolean[]{false, true})
			assertTruncated(() -> new PSet1<>(Integer.class).decode(countOnly(nio, Integer.MAX_VALUE)));
	}

	@Test
	void mapValueRejectsTruncatedHugeCount() {
		for (boolean nio : new boolean[]{false, true})
			assertTruncated(() -> new PMap1<>(Integer.class, Integer.class).decode(countOnly(nio, 1_700_000_000)));
	}

	@Test
	void mapBeanRejectsTruncatedHugeCount() {
		for (boolean nio : new boolean[]{false, true})
			assertTruncated(() -> new PMap2<>(Integer.class, EmptyBean.class).decode(countOnly(nio, 1_700_000_000)));
	}

	@Test
	void legitimateCollectionsCanGrowBeyondInitialCapacity() {
		int size = 0x10001;
		var input = ByteBuffer.Allocate();
		input.WriteUInt(size);
		for (int i = 0; i < size; i++)
			input.WriteInt(i);
		var list = new PList1<>(Integer.class);
		list.decode(ByteBuffer.Wrap(input.Copy()));
		assertEquals(size, list.size());
		assertEquals(size - 1, list.get(size - 1));
		var set = new PSet1<>(Integer.class);
		set.decode(ByteBuffer.Wrap(input.Copy()));
		assertEquals(size, set.size());
		assertTrue(set.contains(size - 1));

		input.Reset();
		input.WriteUInt(size);
		for (int i = 0; i < size; i++) {
			input.WriteInt(i);
			input.WriteInt(i + 1);
		}
		var map = new PMap1<>(Integer.class, Integer.class);
		map.decode(input);
		assertEquals(size, map.size());
		assertEquals(size, map.get(size - 1));
	}
}
