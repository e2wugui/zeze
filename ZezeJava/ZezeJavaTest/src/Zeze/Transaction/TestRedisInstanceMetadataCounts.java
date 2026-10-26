package Zeze.Transaction;

import harness.Fast;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.NioByteBuffer;

@Fast
public class TestRedisInstanceMetadataCounts {
	private static IByteBuffer input(ByteBuffer bytes, boolean nio) {
		return nio ? NioByteBuffer.Wrap(bytes.Copy()) : ByteBuffer.Wrap(bytes.Copy());
	}

	@Test
	public void invalidUnsignedCountsFailBeforeReadingTheGlobalName() {
		var checks = new ArrayList<Executable>();
		for (boolean nio : new boolean[]{false, true}) {
			for (int count : new int[]{-1, Integer.MIN_VALUE}) {
				checks.add(() -> {
					var bytes = ByteBuffer.Allocate();
					bytes.WriteUInt(count);
					int boundary = bytes.getWriteIndex();
					bytes.WriteString("global");
					var input = input(bytes, nio);
					Assertions.assertThrows(IllegalStateException.class, () -> new DatabaseRedis.InUse().decode(input));
					Assertions.assertEquals(boundary, input.getReadIndex());
				});
			}
		}
		Assertions.assertAll(checks);
	}

	@Test
	public void validEmptyAndPopulatedMetadataStillRoundTrips() {
		for (boolean populated : new boolean[]{false, true}) {
			var metadata = new DatabaseRedis.InUse();
			metadata.global = "global";
			if (populated) {
				metadata.instances.add(1);
				metadata.instances.add(2);
			}
			var bytes = metadata.encode();
			for (boolean nio : new boolean[]{false, true}) {
				var input = input(bytes, nio);
				var decoded = new DatabaseRedis.InUse();
				decoded.decode(input);
				Assertions.assertTrue(input.isEmpty());
				Assertions.assertEquals(metadata.global, decoded.global);
				Assertions.assertEquals(populated ? 2 : 0, decoded.instances.size());
				if (populated) {
					Assertions.assertTrue(decoded.instances.contains(1));
					Assertions.assertTrue(decoded.instances.contains(2));
				}
			}
		}
	}
}
