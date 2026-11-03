package Zeze.Arch;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Test;

public class TestLinkdSendStreamLength {
	@Fast
	@Test
	public void malformedLengthsDoNotEscapeStatistics() throws Exception {
		var count = LinkdProvider.class.getDeclaredMethod("countSendStream", Binary.class);
		count.setAccessible(true);
		for (int size : new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE - 11, -1, 1, 0}) {
			var bb = ByteBuffer.Allocate();
			bb.WriteInt4(1);
			bb.WriteInt4(2);
			bb.WriteInt4(size);
			count.invoke(null, new Binary(bb));
		}
	}
}
