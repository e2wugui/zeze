package UnitTest.Zeze.Serialize;

import Zeze.Serialize.ByteBuffer;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import demo.Module1.Key;
import junit.framework.TestCase;

public class TestDynamic extends TestCase {
	public void testDynamicMap() {
		var b = new BValue();
		var m = b.getMap26();
		var v = m.createValue();
		var s = new BSimple();
		s.setInt_1(123);
		v.setBean(s);
		m.put(new Key((short)1, ""), v);

		var bb = ByteBuffer.Allocate();
		b.encode(bb);

		// System.out.println(BitConverter.toString(bb.Bytes,bb.ReadIndex,bb.size()));

		b = new BValue();
		b.decode(bb);
		m = b.getMap26();
		v = m.get(new Key((short)1, ""));
		assertNotNull(v);
		var s0 = v.getBean();
		assertEquals(BSimple.class, s0.getClass());
		assertEquals(123, ((BSimple)s0).getInt_1());
	}
}
