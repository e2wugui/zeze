package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HexFormat;
import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.BLockName;
import Zeze.Serialize.ByteBuffer;
import demo.Module1.BValue;
import demo.Module1.Key;
import harness.Fast;
import org.junit.jupiter.api.Test;

@Fast
public class TestBeanKeyFieldDecode {
	@Test
	public void fullValueDecodeReplacesSharedBeanKey() {
		var source = new BValue();
		source.setKey28(new Key((short)42, ""));
		var bytes = ByteBuffer.encode(source).Copy();

		var fresh = new BValue();
		fresh.decode(ByteBuffer.Wrap(bytes));

		var reused = new BValue();
		var oldKey = new Key((short)1, "old");
		reused.setKey28(oldKey);
		var sibling = new BValue();
		sibling.setKey28(oldKey);
		reused.decode(ByteBuffer.Wrap(bytes));

		assertEquals(new Key((short)42, ""), fresh.getKey28());
		assertEquals(fresh.getKey28(), reused.getKey28());
		assertSame(oldKey, sibling.getKey28());
		assertEquals(new Key((short)1, "old"), sibling.getKey28());
	}

	@Test
	public void beanKeyDecodeReplacesSharedNestedKey() {
		var source = new BLockName(new BGlobalThreadId(42, 0), "lock");
		var bytes = ByteBuffer.encode(source).Copy();
		var oldKey = new BGlobalThreadId(1, 99);
		var reused = new BLockName(oldKey, "lock");
		var sibling = new BLockName(oldKey, "other");

		reused.decode(ByteBuffer.Wrap(bytes));

		assertEquals(source, reused);
		assertSame(oldKey, sibling.getGlobalThreadId());
		assertEquals(new BGlobalThreadId(1, 99), sibling.getGlobalThreadId());
	}

	@Test
	public void omittedBeanKeyUsesLegacyFreshDecodeValue() {
		byte[] legacyBytes = {0};
		var source = new BValue();
		source.setKey28(new Key((short)0, ""));
		assertArrayEquals(legacyBytes, ByteBuffer.encode(source).Copy());

		var fresh = new BValue();
		fresh.decode(ByteBuffer.Wrap(legacyBytes));
		var oldKey = new Key((short)9, "old");
		var reused = new BValue();
		reused.setKey28(oldKey);
		reused.decode(ByteBuffer.Wrap(legacyBytes));

		// The legacy fresh decode retained Key's constructor value of 1.
		assertEquals(new Key((short)1, ""), fresh.getKey28());
		assertEquals(fresh.getKey28(), reused.getKey28());
		assertEquals(new Key((short)9, "old"), oldKey);
	}

	@Test
	public void omittedNestedKeyReplacesOldKey() {
		var legacyBytes = HexFormat.of().parseHex("23046c6f636b00");
		var source = new BLockName(new BGlobalThreadId(0, 0), "lock");
		assertArrayEquals(legacyBytes, ByteBuffer.encode(source).Copy());
		var oldKey = new BGlobalThreadId(1, 99);
		var reused = new BLockName(oldKey, "old");
		var sibling = new BLockName(oldKey, "other");

		reused.decode(ByteBuffer.Wrap(legacyBytes));

		assertEquals(source, reused);
		assertSame(oldKey, sibling.getGlobalThreadId());
		assertEquals(new BGlobalThreadId(1, 99), oldKey);
	}

	@Test
	public void dataDecodeReplacesSharedBeanKeyAndResetsOmittedKey() {
		var legacyBytes = HexFormat.of().parseHex("f60d102a0000");
		var source = new BValue.Data();
		source.setKey28(new Key((short)42, ""));
		assertArrayEquals(legacyBytes, ByteBuffer.encode(source).Copy());

		var oldKey = new Key((short)1, "old");
		var reused = new BValue.Data();
		reused.setKey28(oldKey);
		var sibling = new BValue.Data();
		sibling.setKey28(oldKey);
		reused.decode(ByteBuffer.Wrap(legacyBytes));

		assertEquals(new Key((short)42, ""), reused.getKey28());
		assertSame(oldKey, sibling.getKey28());
		assertEquals(new Key((short)1, "old"), oldKey);

		reused.decode(ByteBuffer.Wrap(new byte[1]));
		assertEquals(new Key((short)1, ""), reused.getKey28());
	}
}
