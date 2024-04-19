package UnitTest.Zeze.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import Zeze.Transaction.Bean;
import Zeze.Util.ConsistentHash;
import Zeze.Util.OutInt;
import Zeze.Util.OutLong;
import Zeze.Util.SortedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class TestConsistentHash {
	private static final Logger logger = LogManager.getLogger(TestConsistentHash.class);

	@Test
	public void testConsistentHash() throws IOException {
		var consistentHash = new ConsistentHash<Integer>((oldK, oldV, oldIndex, newK, newV, newIndex) -> false);

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

	@Test
	public void testStable() {
		var cn = new OutInt();
		SortedMap.Selector<Integer, Integer> selector = (oldK, oldV, oldIndex, newK, newV, newIndex) -> {
			cn.value++;
			var oldHash = Bean.hash64(oldK ^ oldIndex, String.valueOf(oldV));
			var newHash = Bean.hash64(newK ^ newIndex, String.valueOf(newV));
			if (oldHash < newHash)
				return true;
			if (oldHash > newHash)
				return false;
			if (oldIndex < newIndex)
				return true;
			if (oldIndex > newIndex)
				return false;
			// hash和index都一致的可能性极低,但还得考虑极小概率的意外,此时就不管公平了
			int c = oldK.compareTo(newK);
			if (c < 0)
				return true;
			if (c > 0)
				return false;
			c = oldV.compareTo(newV);
			return c < 0; // 如果K,V都相同,说明是服务本身的多个虚拟节点有冲突,那么选择哪个都行
		};

		final int TEST_COUNT = 1;
		for (int j = 0; j < TEST_COUNT; j++) {
			final long timeBegin = System.nanoTime();
			final int begin = Long.hashCode(timeBegin);

			var ch1 = new ConsistentHash<>(selector);
			for (int i = begin; i != begin + 2000; i++)
				ch1.add(String.valueOf(i), i);

			var ch2 = new ConsistentHash<>(selector);
			for (int i = begin + 2000; i-- != begin; )
				ch2.add(String.valueOf(i), i);

			logger.info("testStable: begin={}, conflict={}, time={}ms",
					begin, cn.value, (System.nanoTime() - timeBegin) / 1_000_000);
			Assert.assertEquals(ch1.size(), ch2.size());
			Assert.assertEquals(ch1.toString(), ch2.toString());
		}
	}
}
