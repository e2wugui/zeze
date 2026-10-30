package Zeze.Arch;

import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestOnlineProtocolSlice {
	@ParameterizedTest
	@ValueSource(strings = {"Zeze.Arch.Online", "Zeze.Game.Online"})
	public void diagnosticTypeUsesTheBinarySlice(String onlineClass) throws Exception {
		var encoded = ByteBuffer.Allocate();
		encoded.WriteInt4s(123, 456);
		var bytes = new byte[5 + encoded.size()];
		System.arraycopy(encoded.Bytes, encoded.ReadIndex, bytes, 5, encoded.size());
		var slice = new Binary(bytes, 5, encoded.size());
		var method = Class.forName(onlineClass).getDeclaredMethod("getTypeId", Binary.class);
		method.setAccessible(true);
		assertEquals(Protocol.makeTypeId(123, 456), method.invoke(null, slice));
	}
}
