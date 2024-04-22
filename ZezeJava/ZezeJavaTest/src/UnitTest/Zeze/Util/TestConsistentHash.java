package UnitTest.Zeze.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import Zeze.Transaction.Bean;
import Zeze.Util.ConsistentHash;
import Zeze.Util.OutLong;
import Zeze.Util.Random;
import Zeze.Util.SortedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class TestConsistentHash {
	private static final Logger logger = LogManager.getLogger(TestConsistentHash.class);

	@Test
	public void testConsistentHash() throws IOException {
		var consistentHash = new ConsistentHash<Integer>(null);

		Assert.assertNull(consistentHash.get(Integer.hashCode(1)));

		consistentHash.add("1", 1);
		logger.info("circleSize(1) = {}", consistentHash.circleSize());
		consistentHash.add("2", 2);
		logger.info("circleSize(1,2) = {}", consistentHash.circleSize());
		consistentHash.add("3", 3);
		logger.info("circleSize(1,2,3) = {}", consistentHash.circleSize());
		consistentHash.add("4", 4);
		logger.info("circleSize(1,2,3,4) = {}", consistentHash.circleSize());

		{
			var sum = new HashMap<Integer, OutLong>();
			for (int i = 0; i < 1000_0000; ++i)
				sum.computeIfAbsent(consistentHash.get(i), k -> new OutLong()).value += 1;
			logger.info("sum = {}", sum);
		}

		var nameFile = Path.of(".", "Chinese_Names_Corpus（120W）.txt");
		if (Files.exists(nameFile)) {
			var sum = new HashMap<Integer, OutLong>();
			for (var line : Files.readAllLines(nameFile)) {
				if (line.isEmpty())
					continue;
				sum.computeIfAbsent(consistentHash.get(line.hashCode()), k -> new OutLong()).value += 1;
			}
			System.out.println("name hash name.hashCode()");
			System.out.println(sum);
			sum.clear();
			for (var line : Files.readAllLines(nameFile)) {
				if (line.isEmpty())
					continue;
				sum.computeIfAbsent(consistentHash.get(Bean.hash32(line)), k -> new OutLong()).value += 1;
			}
			System.out.println("name hash Bean.hash32(name)");
			System.out.println(sum);
		}
		//*
		logger.info("{},{},{},{}",
				consistentHash.get(1),
				consistentHash.get(2),
				consistentHash.get(3),
				consistentHash.get(4));

		logger.info("{},{},{},{}",
				consistentHash.get("1".hashCode()),
				consistentHash.get("2".hashCode()),
				consistentHash.get("3".hashCode()),
				consistentHash.get("4".hashCode()));

		logger.info("{},{},{},{}",
				consistentHash.get(Bean.hash32("1")),
				consistentHash.get(Bean.hash32("2")),
				consistentHash.get(Bean.hash32("3")),
				consistentHash.get(Bean.hash32("4")));
		/*/
		Assert.assertEquals(consistentHash.get(Bean.hash32("1")), Integer.valueOf(1));
		Assert.assertEquals(consistentHash.get(Bean.hash32("2")), Integer.valueOf(2));
		Assert.assertEquals(consistentHash.get(Bean.hash32("3")), Integer.valueOf(2));
		Assert.assertEquals(consistentHash.get(Bean.hash32("4")), Integer.valueOf(3));
		// */
		consistentHash.remove(1);
		logger.info("circleSize(2,3,4) = {}", consistentHash.circleSize());
		consistentHash.remove(2);
		logger.info("circleSize(3,4) = {}", consistentHash.circleSize());
		consistentHash.remove(3);
		logger.info("circleSize(4) = {}", consistentHash.circleSize());
		consistentHash.remove(4);
		logger.info("circleSize() = {}", consistentHash.circleSize());

		Assert.assertNull(consistentHash.get(Integer.hashCode(1)));
	}

	@SuppressWarnings("LoggingSimilarMessage")
	@Test
	public void testStable() {
		SortedMap.HashFunc<Integer, Integer> selector = (key, value, index) -> ((long)value << 32) + index;

		final int TEST_COUNT = 1;
		for (int j = 0; j < TEST_COUNT; j++) {
			long timeBegin = System.nanoTime();
			final int begin = Long.hashCode(timeBegin);

			final var ch1 = new ConsistentHash<>(selector);
			for (int i = begin; i != begin + 2000; i++)
				ch1.add(String.valueOf(i), i);

			final var ch2 = new ConsistentHash<>(selector);
			for (int i = begin + 2000; i-- != begin; )
				ch2.add(String.valueOf(i), i);

			logger.info("testStable: begin={}, ch1.size={}:{}/{}, ch2.size={}:{}/{}, time={}ms", begin,
					ch1.size(), ch1.circleKeySize(), ch1.circleSize(),
					ch2.size(), ch2.circleKeySize(), ch2.circleSize(),
					(System.nanoTime() - timeBegin) / 1_000_000);
			Assert.assertEquals(ch1.size(), ch2.size());
			var s1 = ch1.toString();
			var s2 = ch2.toString();
			if (!s1.equals(s2)) {
				logger.error("testStable: s1={}", s1);
				logger.error("testStable: s2={}", s2);
				Assert.fail();
			}
			Assert.assertEquals(ch1.size(), ch2.size());
			Assert.assertEquals(ch1.circleKeySize(), ch2.circleKeySize());
			Assert.assertEquals(ch1.circleSize(), ch2.circleSize());

			timeBegin = System.nanoTime();

			var indexes = new int[1000];
			for (int i = 0; i < indexes.length; i++)
				ch1.remove(indexes[i] = Random.getInstance().nextInt(begin, begin + 2000));
			for (int i = indexes.length - 1; i >= 0; i--)
				ch2.remove(indexes[i]);
			logger.info("testStable: removed half: ch1.size={}:{}/{}, ch2.size={}:{}/{}",
					ch1.size(), ch1.circleKeySize(), ch1.circleSize(),
					ch2.size(), ch2.circleKeySize(), ch2.circleSize());
			s1 = ch1.toString();
			s2 = ch2.toString();
			if (!s1.equals(s2)) {
				logger.error("testStable: s1={}", s1);
				logger.error("testStable: s2={}", s2);
				Assert.fail();
			}
			Assert.assertEquals(ch1.size(), ch2.size());
			Assert.assertEquals(ch1.circleKeySize(), ch2.circleKeySize());
			Assert.assertEquals(ch1.circleSize(), ch2.circleSize());

			for (int i = begin; i != begin + 2000; i++) {
				ch1.remove(i);
				ch2.remove(i);
			}
			logger.info("testStable: removed all: ch1.size={}:{}/{}, ch2.size={}:{}/{}, time={}ms",
					ch1.size(), ch1.circleKeySize(), ch1.circleSize(),
					ch2.size(), ch2.circleKeySize(), ch2.circleSize(),
					(System.nanoTime() - timeBegin) / 1_000_000);
			s1 = ch1.toString();
			s2 = ch2.toString();
			if (!s1.equals("[]") || !s2.equals("[]")) {
				logger.error("testStable: s1={}", s1);
				logger.error("testStable: s2={}", s2);
				Assert.fail();
			}
			Assert.assertEquals(0, ch1.size());
			Assert.assertEquals(0, ch1.circleKeySize());
			Assert.assertEquals(0, ch1.circleSize());
			Assert.assertEquals(0, ch2.size());
			Assert.assertEquals(0, ch2.circleKeySize());
			Assert.assertEquals(0, ch2.circleSize());
		}
	}
}
