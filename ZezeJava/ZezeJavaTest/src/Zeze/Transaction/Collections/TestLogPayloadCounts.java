package Zeze.Transaction.Collections;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.NioByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Log;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestLogPayloadCounts {

	private record Family(String name, int sections, Function<IByteBuffer, Log> decoder) {
	}

	private static List<Family> families() {
		return List.of(
				new Family("bean", 1, input -> LogBean.decode(0, input)),
				new Family("list values", 1, input -> LogList1.decode(0, List1Meta.get(Integer.class), input)),
				new Family("list beans", 2, input -> LogList2.decode(0, List2Meta.get(EmptyBean.class), input)),
				new Family("map values", 2, input -> LogMap1.decode(0, Map1Meta.get(Integer.class, Integer.class), input)),
				new Family("map beans", 3, input -> LogMap2.decode(0, Map2Meta.get(Integer.class, EmptyBean.class), input)),
				new Family("set", 2, input -> LogSet1.decode(0, Set1Meta.get(Integer.class), input)),
				new Family("sorted values", 2, input -> LogSortedMap1.decode(0,
						SortedMap1Meta.get(Integer.class, Integer.class), input)),
				new Family("sorted beans", 3, input -> LogSortedMap2.decode(0,
						SortedMap2Meta.get(Integer.class, EmptyBean.class), input)));
	}

	@Test
	public void unrepresentableCountsFailAtTheirOwnBoundary() {
		var checks = new ArrayList<Executable>();
		for (var family : families()) {
			for (int section = 0; section < family.sections; section++) {
				for (int count : new int[]{-1, Integer.MIN_VALUE}) {
					var bytes = ByteBuffer.Allocate();
					for (int i = 0; i < family.sections; i++)
						bytes.WriteUInt(i == section ? count : 0);
					bytes.WriteByte(0x5a); // Later bytes must remain untouched after the invalid count.
					int expectedPosition = section + 5;
					for (var input : new IByteBuffer[]{ByteBuffer.Wrap(bytes.Copy()), NioByteBuffer.Wrap(bytes.Copy())}) {
						checks.add(() -> assertAll(family.name,
								() -> assertThrows(IllegalStateException.class, () -> family.decoder.apply(input)),
								() -> assertEquals(expectedPosition, input.getReadIndex())));
					}
				}
			}
		}
		assertAll(checks);
	}

	@Test
	public void emptyPayloadsKeepTheirExistingEncoding() {
		for (var family : families()) {
			var bytes = new byte[family.sections];
			var input = ByteBuffer.Wrap(bytes);
			var payload = family.decoder.apply(input);
			assertTrue(input.isEmpty(), family.name);
			var output = ByteBuffer.Allocate();
			payload.encode(output);
			assertArrayEquals(bytes, output.Copy(), family.name);
		}
	}
}
