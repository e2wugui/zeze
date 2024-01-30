package UnitTest.Zeze.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import Zeze.Transaction.Bean;
import Zeze.Util.ConsistentHash;
import Zeze.Util.OutLong;
import org.junit.Assert;
import org.junit.Test;

public class TestConsistentHash {
	@Test
	public void testConsistentHash() throws IOException {
		var consistentHash = new ConsistentHash<Integer>();

		Assert.assertNull(consistentHash.get(Integer.hashCode(1)));

		consistentHash.add("1", 1);
		consistentHash.add("2", 2);
		consistentHash.add("3", 3);
		consistentHash.add("4", 4);

		{
			var sum = new HashMap<Integer, OutLong>();
			for (int i = 0; i < 1000_0000; ++i)
				sum.computeIfAbsent(consistentHash.get(i), k -> new OutLong()).value += 1;
			System.out.println(sum);
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
		System.out.print(consistentHash.get(1) + ",");
		System.out.print(consistentHash.get(2) + ",");
		System.out.print(consistentHash.get(3) + ",");
		System.out.print(consistentHash.get(4) + ",");
		System.out.println();

		System.out.print(consistentHash.get("1".hashCode()) + ",");
		System.out.print(consistentHash.get("2".hashCode()) + ",");
		System.out.print(consistentHash.get("3".hashCode()) + ",");
		System.out.print(consistentHash.get("4".hashCode()) + ",");
		System.out.println();

		System.out.print(consistentHash.get(Bean.hash32("1")) + ",");
		System.out.print(consistentHash.get(Bean.hash32("2")) + ",");
		System.out.print(consistentHash.get(Bean.hash32("3")) + ",");
		System.out.print(consistentHash.get(Bean.hash32("4")) + ",");
		System.out.println();
		/*/
		Assert.assertEquals(consistentHash.get(Bean.hash32("1")), Integer.valueOf(1));
		Assert.assertEquals(consistentHash.get(Bean.hash32("2")), Integer.valueOf(2));
		Assert.assertEquals(consistentHash.get(Bean.hash32("3")), Integer.valueOf(2));
		Assert.assertEquals(consistentHash.get(Bean.hash32("4")), Integer.valueOf(3));
		// */
		consistentHash.remove(1);
		consistentHash.remove(2);
		consistentHash.remove(3);
		consistentHash.remove(4);

		Assert.assertNull(consistentHash.get(Integer.hashCode(1)));
	}
}
