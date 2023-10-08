package UnitTest.Zeze.Util;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector3;
import Zeze.Serialize.Vector3Int;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.FloatList;
import Zeze.Util.IntHashMap;
import Zeze.Util.IntHashSet;
import Zeze.Util.IntList;
import Zeze.Util.JsonReader;
import Zeze.Util.JsonWriter;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.LongList;
import Zeze.Util.LongMap;
import Zeze.Util.Vector3IntList;
import Zeze.Util.Vector3List;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import demo.Module1.Key;
import junit.framework.TestCase;

public class TestJsonZeze extends TestCase {
	public void testDynamicBean() throws ReflectiveOperationException {
		{
			BValue v = new BValue();
			DynamicBean db = new DynamicBean(0, BValue::getSpecialTypeIdFromBean_26, BValue::createBeanFromSpecialTypeId_26);
			BSimple s = new BSimple();
			s.setInt_1(456);
			db.setBean(s);
			v.getMap26().put(new Key((short)123, ""), db);
			String j = JsonWriter.local().clear().setPrettyFormat(true).setWriteNull(true).setDepthLimit(9).write(v).toString();
			BValue v2 = JsonReader.local().buf(j).parse(new BValue());
			assertNotNull(v2);
			assertEquals(1, v2.getMap26().size());
			Map.Entry<Key, DynamicBean> e = v2.getMap26().iterator().next();
			assertEquals(Key.class, e.getKey().getClass());
			assertEquals(123, e.getKey().getS());
			assertEquals(BSimple.TYPEID, e.getValue().typeId());
			assertEquals(BSimple.class, e.getValue().getBean().getClass());
			assertEquals(456, ((BSimple)e.getValue().getBean()).getInt_1());
		}

		{
			BValue.Data d = new BValue.Data();
			BValue.Data.DynamicData_map26 dd = new BValue.Data.DynamicData_map26();
			BSimple.Data sd = new BSimple.Data();
			sd.setInt_1(456);
			dd.setData(sd);
			d.getMap26().put(new Key((short)123, ""), dd);
			String j = JsonWriter.local().clear().setPrettyFormat(true).setWriteNull(true).setDepthLimit(9).write(d).toString();
			BValue.Data v2 = JsonReader.local().buf(j).parse(new BValue.Data());
			assertNotNull(v2);
			assertEquals(1, v2.getMap26().size());
			Map.Entry<Key, BValue.Data.DynamicData_map26> e = v2.getMap26().entrySet().iterator().next();
			assertEquals(Key.class, e.getKey().getClass());
			assertEquals(123, e.getKey().getS());
			assertEquals(BSimple.Data.TYPEID, e.getValue().typeId());
			assertEquals(BSimple.Data.class, e.getValue().getData().getClass());
			assertEquals(456, ((BSimple.Data)e.getValue().getData()).getInt_1());
		}
	}

	// byte[], Binary, ByteBuffer
	public void testBytes() throws ReflectiveOperationException {
		class B {
			byte[] b1;
			Binary b2;
			ByteBuffer b3;
			byte[] b11 = ByteBuffer.Empty;
			Binary b22 = Binary.Empty;
			ByteBuffer b33 = ByteBuffer.Wrap(ByteBuffer.Empty);

			@Override
			public boolean equals(Object o) {
				if (o == null || getClass() != o.getClass())
					return false;
				B b = (B)o;
				return Arrays.equals(b1, b.b1)
						&& Objects.equals(b2, b.b2)
						&& Objects.equals(b3, b.b3)
						&& Arrays.equals(b11, b.b11)
						&& Objects.equals(b22, b.b22)
						&& Objects.equals(b33, b.b33);
			}
		}

		B b = new B();
		var s = JsonWriter.local().clear().write(b).toString();
		// System.out.println(s); // {"b11":"","b22":"","b33":""}
		B bb = JsonReader.local().buf(s).parse(B.class);
		assertEquals(b, bb);

		b.b1 = new byte[0];
		b.b2 = new Binary(new byte[0]);
		b.b3 = ByteBuffer.Allocate();
		b.b11 = new byte[]{'0', 0, -1, 0x7f, (byte)0x80};
		b.b22 = new Binary(new byte[]{'A', 'B', 'C'}, 1, 1);
		b.b33 = ByteBuffer.Wrap(new byte[]{'a', 'b', 'c'}, 1, 1);
		var d = JsonWriter.local().clear().write(b).toBytes();
		// System.out.println(new String(d, StandardCharsets.ISO_8859_1)); // {"b1":"","b2":"","b3":"","b11":"0\u0000ÿ","b22":"B","b33":"b"}
		bb = JsonReader.local().buf(d).parse(B.class);
		assertEquals(b, bb);
	}

	// IntList, LongList, FloatList, Vector3List, Vector3IntList
	public void testList() throws ReflectiveOperationException {
		class L {
			IntList il;
			LongList ll;
			FloatList fl;
			Vector3List v3l;
			Vector3IntList v3il;

			@Override
			public boolean equals(Object o) {
				if (o == null || getClass() != o.getClass())
					return false;
				L l = (L)o;
				return Objects.equals(il, l.il)
						&& Objects.equals(ll, l.ll)
						&& Objects.equals(fl, l.fl)
						&& Objects.equals(v3l, l.v3l)
						&& Objects.equals(v3il, l.v3il);
			}
		}

		L l = new L();
		var s = JsonWriter.local().clear().write(l).toString();
		// System.out.println(s);
		L ll = JsonReader.local().buf(s).parse(L.class);
		assertEquals(l, ll);

		l.il = new IntList();
		l.ll = new LongList();
		l.fl = new FloatList();
		l.v3l = new Vector3List();
		l.v3il = new Vector3IntList();
		s = JsonWriter.local().clear().write(l).toString();
		// System.out.println(s);
		ll = JsonReader.local().buf(s).parse(L.class);
		assertEquals(l, ll);

		l.il.add(Integer.MIN_VALUE);
		l.ll.add(Long.MIN_VALUE);
		l.ll.add(Long.MAX_VALUE);
		l.fl.add(-Float.MAX_VALUE);
		l.fl.add(Float.MIN_VALUE);
		l.fl.add(Float.MAX_VALUE);
		l.v3l.add(new Vector3(1.1f, 2.2f, 3.3f));
		l.v3l.add(new Vector3(-4.4f, -5.5f, -6.6f));
		l.v3il.add(new Vector3Int(1, 2, 3));
		s = JsonWriter.local().clear().write(l).toString();
		// System.out.println(s);
		ll = JsonReader.local().buf(s).parse(L.class);
		assertEquals(l, ll);
	}

	// IntHashSet, LongHashSet
	public void testSet() throws ReflectiveOperationException {
		class S {
			IntHashSet is;
			LongHashSet ls;

			@Override
			public boolean equals(Object o) {
				if (o == null || getClass() != o.getClass())
					return false;
				S s = (S)o;
				return Objects.equals(is, s.is)
						&& Objects.equals(ls, s.ls);
			}
		}

		S s = new S();
		var t = JsonWriter.local().clear().write(s).toString();
		// System.out.println(t);
		S ss = JsonReader.local().buf(t).parse(S.class);
		assertEquals(s, ss);

		s.is = new IntHashSet();
		s.ls = new LongHashSet();
		t = JsonWriter.local().clear().write(s).toString();
		// System.out.println(t);
		ss = JsonReader.local().buf(t).parse(S.class);
		assertEquals(s, ss);

		s.is.add(Integer.MIN_VALUE);
		s.ls.add(Long.MIN_VALUE);
		s.ls.add(Long.MAX_VALUE);
		t = JsonWriter.local().clear().write(s).toString();
		// System.out.println(t);
		ss = JsonReader.local().buf(t).parse(S.class);
		assertEquals(s, ss);
	}

	// IntHashMap, LongHashMap, LongMap
	public void testMap() throws ReflectiveOperationException {
		class M {
			IntHashMap<Integer> im;
			LongHashMap<Long> lm;
			final LongMap<Integer> cm = new LongConcurrentHashMap<>();

			@Override
			public boolean equals(Object o) {
				if (o == null || getClass() != o.getClass())
					return false;
				M m = (M)o;
				return Objects.equals(im, m.im)
						&& Objects.equals(lm, m.lm)
						&& LongMap.equals(cm, m.cm);
			}
		}

		M m = new M();
		var t = JsonWriter.local().clear().write(m).toString();
		// System.out.println(t);
		M mm = JsonReader.local().buf(t).parse(new M());
		assertEquals(m, mm);

		m.im = new IntHashMap<>();
		m.lm = new LongHashMap<>();
		t = JsonWriter.local().clear().write(m).toString();
		// System.out.println(t);
		mm = JsonReader.local().buf(t).parse(new M());
		assertEquals(m, mm);

		m.im.put(Integer.MIN_VALUE, Integer.MIN_VALUE);
		m.lm.put(Long.MIN_VALUE, Long.MIN_VALUE);
		m.lm.put(Long.MAX_VALUE, Long.MAX_VALUE);
		m.cm.put(Long.MIN_VALUE, Integer.MIN_VALUE);
		m.cm.put(Long.MAX_VALUE, Integer.MAX_VALUE);
		t = JsonWriter.local().clear().write(m).toString();
		// System.out.println(t);
		mm = JsonReader.local().buf(t).parse(new M());
		assertEquals(m, mm);
	}
}
