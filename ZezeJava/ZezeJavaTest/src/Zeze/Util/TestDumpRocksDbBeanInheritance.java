package Zeze.Util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestDumpRocksDbBeanInheritance {
	@Test
	public void restartsFieldIdsAtEachParent() throws Exception {
		var input = ByteBuffer.Allocate();
		input.WriteTag(0, 5, ByteBuffer.INTEGER);
		input.WriteLong(50);
		input.WriteByte(1);
		input.WriteByte(1); // empty intermediate bean
		input.WriteTag(0, 2, ByteBuffer.INTEGER);
		input.WriteLong(20);
		input.WriteByte(0);

		var output = new ByteArrayOutputStream();
		var method = DumpRocksDb.class.getDeclaredMethod("dumpBean", OutputStream.class, ByteBuffer.class);
		method.setAccessible(true);
		method.invoke(null, output, input);

		assertEquals("{5:50,P:{P:{2:20}}}", output.toString(StandardCharsets.UTF_8));
		assertTrue(input.isEmpty());
	}
}
