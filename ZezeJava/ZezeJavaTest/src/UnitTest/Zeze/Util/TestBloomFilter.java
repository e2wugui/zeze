package UnitTest.Zeze.Util;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;
import Zeze.Util.BloomFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class TestBloomFilter implements BloomFilter.BitMap {
	private static final Logger logger = LogManager.getLogger(TestBloomFilter.class);
	private static final int BYTE_COUNT = 65536;

	private final ByteBuffer bb = ByteBuffer.allocateDirect(BYTE_COUNT);

	@Override
	public long getBitCount() {
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
		var bf = new BloomFilter(this, 8);
		for (int i = 0; i < BYTE_COUNT; i++)
			bf.addKey(i);
		for (int i = 0; i < BYTE_COUNT; i++)
			Assert.assertTrue(bf.testKey(i));

		long err = 0;
		var r = ThreadLocalRandom.current();
		for (int i = 0; i < BYTE_COUNT; i++)
			err += bf.testKey(r.nextLong(BYTE_COUNT, Long.MAX_VALUE)) ? 1 : 0;
		logger.info("TestBloomFilter.err = {}/{}", err, BYTE_COUNT);
	}
}
