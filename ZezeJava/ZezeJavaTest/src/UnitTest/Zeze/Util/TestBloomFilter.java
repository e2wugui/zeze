package UnitTest.Zeze.Util;

import java.nio.ByteBuffer;
import Zeze.Util.BloomFilter;
import Zeze.Util.LongHashSet;
import Zeze.Util.StableRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class TestBloomFilter implements BloomFilter.BitArray {
	private static final Logger logger = LogManager.getLogger(TestBloomFilter.class);
	private static final int HASH_COUNT = 6;
	private static final int BYTE_COUNT = 4096;
	private static final int KEY_COUNT = 2048;
	private static final int FAKE_KEY_COUNT = 20480;

	private final ByteBuffer bb = ByteBuffer.allocateDirect(BloomFilter.toPowerOfTwo(BYTE_COUNT));

	@Override
	public long getCapacity() {
		return bb.limit() * 8L;
	}

	@Override
	public void setBit(long index) {
		int i = (int)(index >>> 3);
		bb.put(i, (byte)(bb.get(i) | (1 << (index & 7))));
	}

	@Override
	public boolean getBit(long index) {
		return (bb.get((int)(index >>> 3)) & (1 << (index & 7))) != 0;
	}

	@Test
	public void testSimple() {
		var bf = new BloomFilter(this, HASH_COUNT);
		logger.info("TestBloomFilter.bitsPerKey = {}", bf.getBitsPerKey());
		var sr = new StableRandom(1);
		var keys = new LongHashSet();
		for (int i = 0; i < KEY_COUNT; i++) {
			var v = sr.next64();
			keys.add(v);
			bf.addKey(v);
		}
		sr.setSeed(1);
		for (int i = 0; i < KEY_COUNT; i++) {
			var v = sr.next64();
			Assert.assertTrue(keys.contains(v));
			Assert.assertTrue(bf.testKey(v));
		}

		sr.setSeed(System.currentTimeMillis());
		long err = 0;
		for (int i = 0; i < FAKE_KEY_COUNT; i++) {
			var v = sr.next64();
			while (keys.contains(v))
				v = sr.next64();
			err += bf.testKey(v) ? 1 : 0;
		}
		logger.info("TestBloomFilter.err = {}/{} {}%", err, FAKE_KEY_COUNT, err * 100f / FAKE_KEY_COUNT);
		logger.info("TestBloomFilter.totalBits = {}/{} {}% => {}%",
				bf.getTotalBits(), BYTE_COUNT * 8,
				bf.getTotalBits() * 100 / (BYTE_COUNT * 8),
				bf.getTotalBits() * 100 * Math.scalb(1, -bf.getBitsPerKey()) / (BYTE_COUNT * 8));
	}
}
