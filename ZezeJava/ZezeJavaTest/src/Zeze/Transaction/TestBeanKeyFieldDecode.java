package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import Zeze.Builtin.Collections.Queue.BQueueNodeKey;
import Zeze.Builtin.Threading.BGlobalThreadId;
import Zeze.Builtin.Threading.BLockName;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Collections.BeanKeyMeta;
import Zeze.Transaction.Collections.LogMap1;
import Zeze.Transaction.Collections.Map1Meta;
import Zeze.Transaction.Collections.PMap1;
import Zeze.Transaction.Logs.LogBeanKeyValue;
import Zeze.Util.Id128;
import demo.Module1.BValue;
import demo.Module1.Key;
import demo.Module1.Table2;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import rocksdynamic.Fixture.ExternalKeyDefaults;
import rocksdynamic.Fixture.LegacyDecodeChild;
import rocksdynamic.Fixture.LegacyDecodeKey;
import rocksdynamic.Fixture.RocksExternalKeyDefaults;

@Fast
public class TestBeanKeyFieldDecode {
	@Test
	public void keyTypesExposeOnlyFactoryDecoding() {
		for (var type : List.of(Key.class, BLockName.class, BQueueNodeKey.class, Id128.class)) {
			assertFalse(Serializable.class.isAssignableFrom(type));
			assertThrows(NoSuchMethodException.class, () -> type.getMethod("decode", IByteBuffer.class));
			assertThrows(NoSuchMethodException.class, () -> type.getMethod("decodeResultSet", ArrayList.class, ResultSet.class));
			assertThrows(NoSuchMethodException.class, () -> type.getMethod("readFrom", IByteBuffer.class, int.class));
		}
		assertThrows(NoSuchMethodException.class, () -> Id128.class.getMethod("decodeRaw", IByteBuffer.class));
		assertThrows(NoSuchMethodException.class, () -> BQueueNodeKey.class.getMethod(
				"readFromInitialized", IByteBuffer.class, String.class, long.class));
		assertThrows(NoSuchMethodException.class, () -> BQueueNodeKey.class.getMethod(
				"readFromResultSet", ArrayList.class, ResultSet.class, String.class, long.class));
	}

	@Test
	public void keyFactoryConsumesOnlyTheLegacyTaggedBody() {
		var wire = ByteBuffer.Allocate();
		wire.WriteLong(12345);
		wire.Append(hex("23036b657900"));
		wire.WriteByte(90);
		assertEquals(new Key((short)0, "key"), wire.ReadBeanKey(ByteBuffer.DYNAMIC, Key.class, Key::readFrom, Key::new));
		assertEquals(90, wire.ReadByte());

		var invalid = ByteBuffer.Wrap(new byte[]{0});
		assertThrows(IllegalStateException.class,
				() -> invalid.ReadBeanKey(ByteBuffer.INTEGER, Key.class, Key::readFrom, Key::new));
		assertEquals(0, invalid.ReadIndex);
	}

	@Test
	public void schemaInitializedKeyStillReadsTheLegacyField() {
		var expected = new LegacyDecodeKey(42);
		var legacy = hex("46102a0000");
		var bean = new LegacyDecodeChild();
		assertEquals(new LegacyDecodeKey(3), bean.getKeyValue());
		bean.decode(ByteBuffer.Wrap(legacy));
		assertEquals(expected, bean.getKeyValue());
		assertArrayEquals(legacy, ByteBuffer.encode(bean).Copy());
		bean.reset();
		assertEquals(new LegacyDecodeKey(3), bean.getKeyValue());

		var data = new LegacyDecodeChild.Data();
		data.decode(ByteBuffer.Wrap(legacy));
		assertEquals(expected, data.getKeyValue());
		assertArrayEquals(legacy, ByteBuffer.encode(data).Copy());
		data.reset();
		assertEquals(new LegacyDecodeKey(3), data.getKeyValue());
	}

	@Test
	public void externalKeyDefaultsUseTheStandardFactories() {
		var initial = new Id128(1, 2);
		var wireValue = new Id128(7, 8);
		var bean = new ExternalKeyDefaults();
		var data = new ExternalKeyDefaults.Data();
		var rocks = new RocksExternalKeyDefaults();
		assertEquals(initial, bean.getKeyValue());
		assertEquals(initial, data.getKeyValue());
		assertEquals(initial, rocks.getKeyValue());

		bean.setKeyValue(wireValue);
		var bytes = ByteBuffer.encode(bean).Copy();
		bean.decode(ByteBuffer.Wrap(bytes));
		data.decode(ByteBuffer.Wrap(bytes));
		rocks.decode(ByteBuffer.Wrap(bytes));
		assertEquals(wireValue, bean.getKeyValue());
		assertEquals(wireValue, data.getKeyValue());
		assertEquals(wireValue, rocks.getKeyValue());
		assertNotSame(wireValue, bean.getKeyValue());

		bean.decode(ByteBuffer.Wrap(new byte[]{0}));
		data.decode(ByteBuffer.Wrap(new byte[]{0}));
		rocks.decode(ByteBuffer.Wrap(new byte[]{0}));
		assertEquals(Id128.Zero, bean.getKeyValue());
		assertEquals(Id128.Zero, data.getKeyValue());
		assertEquals(Id128.Zero, rocks.getKeyValue());
		rocks.getKeyValue().assign(9, 10);
		assertEquals(new Id128(9, 10), rocks.getKeyValue());
		assertEquals(new Id128(), Id128.Zero);
		bean.reset();
		data.reset();
		assertEquals(initial, bean.getKeyValue());
		assertEquals(initial, data.getKeyValue());
	}

	@Test
	public void readsLegacyTableCollectionAndLogBytesWithStableTypeIds() {
		var key = new Key((short)0, "key");
		var legacyKey = hex("23036b657900");
		var table = new Table2();
		assertEquals(key, table.decodeKey(ByteBuffer.Wrap(legacyKey)));
		assertArrayEquals(legacyKey, table.encodeKey(key).Copy());

		var meta = Map1Meta.get(Key.class, Integer.class);
		var collection = new PMap1<>(meta);
		var legacyCollection = hex("0123036b6579002a");
		collection.decode(ByteBuffer.Wrap(legacyCollection));
		assertEquals(42, collection.get(key));
		assertArrayEquals(legacyCollection, ByteBuffer.encode(collection).Copy());

		var legacyMapLog = hex("0123036b6579002a00");
		var mapLog = LogMap1.decode(7, meta, ByteBuffer.Wrap(legacyMapLog));
		assertEquals(-1396916332, mapLog.getTypeId());
		assertEquals(Map.of(key, 42), mapLog.getModified());
		assertArrayEquals(legacyMapLog, encodeLog(mapLog));

		var keyLog = LogBeanKeyValue.decode(8, BeanKeyMeta.get(Key.class), ByteBuffer.Wrap(legacyKey));
		assertEquals(-1875400904, keyLog.getTypeId());
		assertEquals(key, keyLog.value);
		assertArrayEquals(legacyKey, encodeLog(keyLog));

		var legacyRocksKey = hex("102a13036b657900");
		var rocksKeyType = Zeze.Builtin.TestRocks.BeanKey.class;
		var rocksKey = new Zeze.Builtin.TestRocks.BeanKey(42, "key");
		assertEquals(rocksKey, SerializeHelper.createDecodeFunc(rocksKeyType).apply(ByteBuffer.Wrap(legacyRocksKey)));
		var rocksLog = new Zeze.Raft.RocksRaft.Log1.LogBeanKey<>(rocksKeyType);
		rocksLog.decode(ByteBuffer.Wrap(legacyRocksKey));
		var first = rocksLog.value;
		rocksLog.decode(ByteBuffer.Wrap(legacyRocksKey));
		assertNotSame(first, rocksLog.value);
		assertEquals(rocksKey, rocksLog.value);
		assertEquals(30401496, rocksLog.getTypeId());
		var encoded = ByteBuffer.Allocate();
		rocksLog.encode(encoded);
		assertArrayEquals(legacyRocksKey, encoded.Copy());
	}

	@Test
	public void readsBothLegacyId128Representations() {
		var id = new Id128(129, -1);
		var binary = hex("14204081ff00");
		var raw = hex("8081ffffffffffffffffff");
		assertEquals(id, Id128.readFrom(ByteBuffer.Wrap(binary)));
		assertEquals(id, Id128.readRawFrom(ByteBuffer.Wrap(raw)));
		assertArrayEquals(binary, ByteBuffer.encode(id).Copy());
		var encoded = ByteBuffer.Allocate();
		id.encodeRaw(encoded);
		assertArrayEquals(raw, encoded.Copy());
		assertEquals(-1351030866, BeanKeyMeta.get(Id128.class).logTypeId);
	}

	@Test
	public void sqlReplacesSharedKeyOnlyAfterItIsFullyRead() throws SQLException {
		var shared = new LegacyDecodeKey(99);
		var row = new LegacyDecodeChild();
		var sibling = new LegacyDecodeChild();
		row.setKeyValue(shared);
		sibling.setKeyValue(shared);
		var hashed = new HashMap<>(Map.of(shared, "shared"));
		var sorted = new TreeMap<>(Map.of(shared, "shared"));
		var parents = new ArrayList<>(List.of("Row"));

		assertThrows(SQLException.class, () -> row.decodeResultSet(parents, sqlRow(Map.of(
				"Row_Number", 0, "Row_Entries", "[]"))));
		assertSame(shared, row.getKeyValue());
		assertEquals(List.of("Row"), parents);

		row.decodeResultSet(parents, sqlRow(Map.of("Row_Number", 0, "Row_Entries", "[]", "Row_KeyValue_Number", 42)));
		assertEquals(new LegacyDecodeKey(42), row.getKeyValue());
		assertNotSame(shared, row.getKeyValue());
		assertSame(shared, sibling.getKeyValue());
		assertEquals(99, shared.getNumber());
		assertEquals("shared", hashed.get(new LegacyDecodeKey(99)));
		assertEquals("shared", sorted.get(new LegacyDecodeKey(99)));
		assertEquals(List.of("Row"), parents);
	}

	@Test
	public void sqlFactoriesKeepTableAndNestedColumnNamesAndNullRules() throws SQLException {
		var columns = new HashMap<String, @Nullable Object>();
		columns.put("__key_s", (short)5);
		columns.put("__key_str", null);
		assertEquals(new Key((short)5, ""), new Table2().decodeKeyResultSet(sqlRow(columns)));

		var parents = new ArrayList<>(List.of("Root"));
		var key = BLockName.readFromResultSet(parents, sqlRow(Map.of("Root_GlobalThreadId_ServerId", 2,
				"Root_GlobalThreadId_ThreadId", 9L, "Root_Name", "lock")));
		assertEquals(new BLockName(new BGlobalThreadId(2, 9), "lock"), key);
		assertEquals(List.of("Root"), parents);

		assertThrows(SQLException.class, () -> BLockName.readFromResultSet(parents,
				sqlRow(Map.of("Root_GlobalThreadId_ServerId", 2))));
		assertEquals(List.of("Root"), parents);
	}

	private static ResultSet sqlRow(Map<String, ?> columns) {
		return (ResultSet)Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
				(proxy, method, args) -> {
					var column = args[0];
					if (!columns.containsKey(column))
						throw new SQLException("unexpected column " + column);
					return columns.get(column);
				});
	}

	private static byte[] hex(String bytes) {
		return HexFormat.of().parseHex(bytes);
	}

	private static byte[] encodeLog(Log log) {
		var bb = ByteBuffer.Allocate();
		log.encode(bb);
		return bb.Copy();
	}

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
	public void beanKeyFactoryReadsNestedKeyIntoNewObject() {
		var source = new BLockName(new BGlobalThreadId(42, 0), "lock");
		var bytes = ByteBuffer.encode(source).Copy();
		var oldKey = new BGlobalThreadId(1, 99);
		var previous = new BLockName(oldKey, "lock");
		var sibling = new BLockName(oldKey, "other");

		var decoded = BLockName.readFrom(ByteBuffer.Wrap(bytes));

		assertEquals(source, decoded);
		assertNotSame(previous, decoded);
		assertSame(oldKey, previous.getGlobalThreadId());
		assertSame(oldKey, sibling.getGlobalThreadId());
		assertEquals(new BGlobalThreadId(1, 99), sibling.getGlobalThreadId());
	}

	@Test
	public void nestedKeyFactoryKeepsReadingLegacyNamesLongerThanConstructorLimit() {
		var name = "x".repeat(257);
		assertThrows(IllegalArgumentException.class, () -> new BLockName(new BGlobalThreadId(42, 0), name));

		var bytes = ByteBuffer.Allocate();
		bytes.WriteTag(0, 1, ByteBuffer.BEAN);
		new BGlobalThreadId(42, 0).encode(bytes);
		bytes.WriteTag(1, 2, ByteBuffer.BYTES);
		bytes.WriteString(name);
		bytes.WriteByte(0);
		var wire = bytes.Copy();

		var decoded = BLockName.readFrom(ByteBuffer.Wrap(wire));

		assertEquals(new BGlobalThreadId(42, 0), decoded.getGlobalThreadId());
		assertEquals(name, decoded.getName());
		assertArrayEquals(wire, ByteBuffer.encode(decoded).Copy());
	}

	@Test
	public void omittedBeanKeyUsesWireZeroDespiteConstructorDefault() {
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

		assertEquals(new Key((short)0, ""), fresh.getKey28());
		assertEquals(fresh.getKey28(), reused.getKey28());
		assertEquals(new Key((short)9, "old"), oldKey);
		assertArrayEquals(legacyBytes, ByteBuffer.encode(reused).Copy());
	}

	@Test
	public void omittedNestedKeyIsCreatedWithWireDefaults() {
		var legacyBytes = HexFormat.of().parseHex("23046c6f636b00");
		var source = new BLockName(new BGlobalThreadId(0, 0), "lock");
		assertArrayEquals(legacyBytes, ByteBuffer.encode(source).Copy());
		var oldKey = new BGlobalThreadId(1, 99);
		var previous = new BLockName(oldKey, "old");
		var sibling = new BLockName(oldKey, "other");

		var decoded = BLockName.readFrom(ByteBuffer.Wrap(legacyBytes));

		assertEquals(source, decoded);
		assertSame(oldKey, previous.getGlobalThreadId());
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
		assertEquals(new Key((short)0, ""), reused.getKey28());
	}
}
