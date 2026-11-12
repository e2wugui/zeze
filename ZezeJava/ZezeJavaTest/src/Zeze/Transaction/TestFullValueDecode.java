package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector2;
import demo.Bean1;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import demo.Module1.Key;
import harness.Fast;
import org.junit.jupiter.api.Test;
import rocksdynamic.Fixture.LegacyDecodeKey;
import rocksdynamic.Fixture.LegacyDecodeParent;

@Fast
public class TestFullValueDecode {
	// Bytes captured with the legacy encoder. Field 12 contains only its long field.
	private static final byte[] LEGACY_PARTIAL_VALUE = HexFormat.of().parseHex("102ab620050000");
	private static final byte[] LEGACY_EMPTY_VALUE = {0};
	private static final byte[] LEGACY_NESTED_NONZERO = HexFormat.of().parseHex("1610070000");

	@Test
	public void beanResetReachesNestedBeanThroughCollOne() {
		var value = new BValue();
		var simple = value.getBean12();
		var removed = simple.getRemoved();
		simple.setInt_1(7);
		removed.setInt_1(9);

		value.reset();

		assertSame(simple, value.getBean12());
		assertSame(removed, simple.getRemoved());
		assertEquals(0, simple.getInt_1());
		assertEquals(0, removed.getInt_1());
	}

	@Test
	public void beanDecodeClearsOmittedFieldsAndNestedValues() {
		var source = new BValue();
		source.setKey28(new Key((short)0, ""));
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(source).Copy());
		source.setInt_1(42);
		source.getBean12().setLong2(5);
		assertArrayEquals(LEGACY_PARTIAL_VALUE, ByteBuffer.encode(source).Copy());

		var reused = new BValue();
		reused.setInt_1(7);
		reused.setString3("old");
		reused.setBool4(true);
		reused.setShort5((short)6);
		reused.setFloat6(7);
		reused.setDouble7(8);
		reused.setBytes8(new Binary(new byte[]{9}));
		reused.setByte13((byte)10);
		reused.setVector2(new Vector2(1, 2));
		reused.setDynamic14(new Bean1(3));
		reused.getList9().add(new Bean1(4));
		reused.getSet10().add(5);
		reused.getMap15().put(6L, 7L);
		reused.getSortedmap2().put(8, new BSimple());
		reused.getBean12().setString3("old child");
		var removed = reused.getBean12().getRemoved();
		removed.setInt_1(9);

		var fresh = new BValue();
		fresh.decode(ByteBuffer.Wrap(LEGACY_PARTIAL_VALUE));
		reused.decode(ByteBuffer.Wrap(LEGACY_PARTIAL_VALUE));
		assertEquals(42, fresh.getInt_1());
		assertEquals(5, fresh.getBean12().getLong2());
		assertEquals("", fresh.getBean12().getString3());
		assertEquals(0, fresh.getBean12().getRemoved().getInt_1());
		assertSame(removed, reused.getBean12().getRemoved());
		assertEquals(new Key((short)0, ""), fresh.getKey28());
		assertEquals(fresh, reused);

		var empty = new BValue();
		empty.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		var simple = reused.getBean12();
		removed.setInt_1(11);
		reused.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		assertSame(simple, reused.getBean12());
		assertSame(removed, simple.getRemoved());
		assertEquals(0, removed.getInt_1());
		assertEquals(0, reused.getBean12().getLong2());
		assertEquals(empty, reused);
	}

	@Test
	public void dataDecodeClearsOmittedFieldsAndNestedValues() {
		var source = new BValue.Data();
		source.setKey28(new Key((short)0, ""));
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(source).Copy());
		source.setInt_1(42);
		source.getBean12().setLong2(5);
		assertArrayEquals(LEGACY_PARTIAL_VALUE, ByteBuffer.encode(source).Copy());

		var reused = new BValue.Data();
		reused.setInt_1(7);
		reused.setString3("old");
		reused.setBool4(true);
		reused.setBytes8(new Binary(new byte[]{9}));
		reused.setVector2(new Vector2(1, 2));
		reused.setDynamic14(new Bean1.Data(3, null));
		reused.getList9().add(new Bean1.Data(4, null));
		reused.getSet10().add(5);
		reused.getMap15().put(6L, 7L);
		reused.getSortedmap1().put(8, 9);
		reused.getList30().add(10);
		reused.getArray29().add(11.0f);
		reused.getBean12().setString3("old child");
		var removed = reused.getBean12().getRemoved();
		removed.setInt_1(9);

		var fresh = new BValue.Data();
		fresh.decode(ByteBuffer.Wrap(LEGACY_PARTIAL_VALUE));
		reused.decode(ByteBuffer.Wrap(LEGACY_PARTIAL_VALUE));
		assertEquals(42, fresh.getInt_1());
		assertEquals(5, fresh.getBean12().getLong2());
		assertEquals("", fresh.getBean12().getString3());
		assertEquals(0, fresh.getBean12().getRemoved().getInt_1());
		assertSame(removed, reused.getBean12().getRemoved());
		assertEquals(new Key((short)0, ""), fresh.getKey28());
		assertEquals(fresh, reused);

		var empty = new BValue.Data();
		empty.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		var simple = reused.getBean12();
		removed.setInt_1(11);
		reused.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		assertSame(simple, reused.getBean12());
		assertSame(removed, simple.getRemoved());
		assertEquals(0, removed.getInt_1());
		assertEquals(0, reused.getBean12().getLong2());
		assertEquals(empty, reused);
	}

	@Test
	public void omittedBatchResetsCollectionsInPlace() {
		var key = new Binary(new byte[]{1});
		var value = new Binary(new byte[]{2});
		var bean = new BPrepareBatch();
		var data = new BPrepareBatch.Data();
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(bean).Copy());
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(data).Copy());

		var batch = bean.getBatch();
		var batchData = data.getBatch();
		batch.getPuts().put(key, value);
		batch.getDeletes().add(key);
		batch.setTid(99);
		batchData.getPuts().put(key, value);
		batchData.getDeletes().add(key);
		batchData.setTid(99);
		bean.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		data.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));

		assertSame(batch, bean.getBatch());
		assertSame(batchData, data.getBatch());
		assertTrue(batch.getPuts().isEmpty());
		assertTrue(batch.getDeletes().isEmpty());
		assertEquals(0, batch.getTid());
		assertTrue(batchData.getPuts().isEmpty());
		assertTrue(batchData.getDeletes().isEmpty());
		assertEquals(0, batchData.getTid());
	}

	@Test
	public void standaloneBeanKeyDecodeClearsOmittedMembers() {
		var legacyKey = HexFormat.of().parseHex("23036b657900");
		var source = new Key((short)0, "key");
		assertArrayEquals(legacyKey, ByteBuffer.encode(source).Copy());

		var fresh = new Key();
		fresh.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		var reused = new Key();
		reused.decode(ByteBuffer.Wrap(legacyKey));
		assertEquals(source, reused);
		reused.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		assertEquals(new Key((short)0, ""), fresh);
		assertEquals(fresh, reused);
	}

	@Test
	public void omittedScalarRetainsLegacyWireZeroDespiteConstructorDefault() {
		var fresh = new Bean1();
		var freshData = new Bean1.Data();
		assertEquals(1, fresh.getV1());
		assertEquals(1, freshData.getV1());
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(new Bean1(0)).Copy());
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(new Bean1.Data(0, null)).Copy());

		fresh.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		freshData.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		var reused = new Bean1(7);
		var reusedData = new Bean1.Data(7, null);
		reused.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		reusedData.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		assertEquals(0, fresh.getV1());
		assertEquals(0, freshData.getV1());
		assertEquals(fresh.getV1(), reused.getV1());
		assertEquals(freshData.getV1(), reusedData.getV1());
	}

	@Test
	public void omittedGTableClearsOldRows() {
		var fresh = new demo.ModuleGTable.BValue();
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(fresh).Copy());
		fresh.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));

		var reused = new demo.ModuleGTable.BValue();
		reused.getGTable().put(1, 2, 3);
		reused.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		assertTrue(reused.getGTable().getPMap2().isEmpty());
		assertArrayEquals(ByteBuffer.encode(fresh).Copy(), ByteBuffer.encode(reused).Copy());
	}

	@Test
	public void omittedBeanDecodesWireDefaultsInPlace() {
		var source = new LegacyDecodeParent();
		assertEquals(7, source.getChild().getNumber());
		assertEquals(new LegacyDecodeKey(3), source.getChild().getKeyValue());
		source.getChild().setKeyValue(new LegacyDecodeKey(0));
		assertArrayEquals(LEGACY_NESTED_NONZERO, ByteBuffer.encode(source).Copy());
		source.getChild().setNumber(0);
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(source).Copy());

		var fresh = new LegacyDecodeParent();
		fresh.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		var reused = new LegacyDecodeParent();
		var child = reused.getChild();
		var shared = new LegacyDecodeKey(99);
		var sibling = new LegacyDecodeParent();
		sibling.getChild().setKeyValue(shared);
		child.setNumber(99);
		child.setScratch(77);
		child.setKeyValue(shared);
		child.getEntries().add(1);
		reused.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));

		assertSame(child, reused.getChild());
		assertEquals(0, fresh.getChild().getNumber());
		assertEquals(fresh.getChild().getNumber(), child.getNumber());
		assertEquals(77, child.getScratch());
		assertTrue(child.getEntries().isEmpty());
		assertEquals(new LegacyDecodeKey(0), child.getKeyValue());
		assertEquals(fresh.getChild().getKeyValue(), child.getKeyValue());
		assertSame(shared, sibling.getChild().getKeyValue());
		assertEquals(99, shared.getNumber());
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(fresh).Copy());
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(reused).Copy());

		fresh.decode(ByteBuffer.Wrap(LEGACY_NESTED_NONZERO));
		reused.decode(ByteBuffer.Wrap(LEGACY_NESTED_NONZERO));
		assertSame(child, reused.getChild());
		assertEquals(7, fresh.getChild().getNumber());
		assertEquals(7, child.getNumber());
		assertEquals(77, child.getScratch());
		assertArrayEquals(LEGACY_NESTED_NONZERO, ByteBuffer.encode(reused).Copy());
	}

	@Test
	public void omittedDataBeanDecodesWireDefaultsInPlace() {
		var source = new LegacyDecodeParent.Data();
		assertEquals(7, source.getChild().getNumber());
		assertEquals(new LegacyDecodeKey(3), source.getChild().getKeyValue());
		source.getChild().setKeyValue(new LegacyDecodeKey(0));
		assertArrayEquals(LEGACY_NESTED_NONZERO, ByteBuffer.encode(source).Copy());
		source.getChild().setNumber(0);
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(source).Copy());

		var fresh = new LegacyDecodeParent.Data();
		fresh.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		var reused = new LegacyDecodeParent.Data();
		var child = reused.getChild();
		var shared = new LegacyDecodeKey(99);
		var sibling = new LegacyDecodeParent.Data();
		sibling.getChild().setKeyValue(shared);
		child.setNumber(99);
		child.setScratch(77);
		child.setKeyValue(shared);
		child.getEntries().add(1);
		reused.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));

		assertSame(child, reused.getChild());
		assertEquals(0, fresh.getChild().getNumber());
		assertEquals(fresh.getChild().getNumber(), child.getNumber());
		assertEquals(77, child.getScratch());
		assertTrue(child.getEntries().isEmpty());
		assertEquals(new LegacyDecodeKey(0), child.getKeyValue());
		assertEquals(fresh.getChild().getKeyValue(), child.getKeyValue());
		assertSame(shared, sibling.getChild().getKeyValue());
		assertEquals(99, shared.getNumber());
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(fresh).Copy());
		assertArrayEquals(LEGACY_EMPTY_VALUE, ByteBuffer.encode(reused).Copy());

		fresh.decode(ByteBuffer.Wrap(LEGACY_NESTED_NONZERO));
		reused.decode(ByteBuffer.Wrap(LEGACY_NESTED_NONZERO));
		assertSame(child, reused.getChild());
		assertEquals(7, fresh.getChild().getNumber());
		assertEquals(7, child.getNumber());
		assertEquals(77, child.getScratch());
		assertArrayEquals(LEGACY_NESTED_NONZERO, ByteBuffer.encode(reused).Copy());
	}

	@Test
	public void dataSortedMapDecodeUsesNaturalOrderAndLeavesOldMapAlone() {
		var legacyBytes = HexFormat.of().parseHex("f5200002ff0a011400");
		var source = new BValue.Data();
		source.setKey28(new Key((short)0, ""));
		source.getSortedmap1().put(-1, 10);
		source.getSortedmap1().put(1, 20);
		assertArrayEquals(legacyBytes, ByteBuffer.encode(source).Copy());

		var fresh = new BValue.Data();
		fresh.decode(ByteBuffer.Wrap(legacyBytes));
		var reused = new BValue.Data();
		var oldMap = new TreeMap<Integer, Integer>(Comparator.comparingInt(Math::abs));
		oldMap.put(9, 90);
		reused.setSortedmap1(oldMap);
		reused.decode(ByteBuffer.Wrap(legacyBytes));
		assertEquals(Map.of(-1, 10, 1, 20), fresh.getSortedmap1());
		assertEquals(fresh.getSortedmap1(), reused.getSortedmap1());
		assertNull(reused.getSortedmap1().comparator());
		assertEquals(Map.of(9, 90), oldMap);

		reused.setSortedmap1(oldMap);
		reused.decode(ByteBuffer.Wrap(LEGACY_EMPTY_VALUE));
		assertTrue(reused.getSortedmap1().isEmpty());
		assertNull(reused.getSortedmap1().comparator());
		assertEquals(Map.of(9, 90), oldMap);
	}
}
