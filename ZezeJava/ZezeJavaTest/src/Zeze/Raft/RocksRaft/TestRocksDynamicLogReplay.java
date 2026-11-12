package Zeze.Raft.RocksRaft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HexFormat;
import java.util.List;
import Zeze.Builtin.GlobalCacheManagerWithRaft.BCacheState;
import Zeze.Builtin.TestRocks.BValue;
import Zeze.Builtin.TestRocks.BeanKey;
import Zeze.Raft.RocksRaft.Log1.LogInt;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Serialize.Vector2;
import Zeze.Transaction.HasManagedException;
import harness.Fast;
import org.junit.jupiter.api.Test;
import rocksdynamic.Fixture.Amount;
import rocksdynamic.Fixture.CustomPayload;
import rocksdynamic.Fixture.Defaults;
import rocksdynamic.Fixture.LegacyDecodeKey;
import rocksdynamic.Fixture.LegacyRocksDecodeParent;
import rocksdynamic.Fixture.Quantity;
import rocksdynamic.Fixture.Value;

@Fast
public class TestRocksDynamicLogReplay {
	@Test
	public void sameSpecialIdUsesEachDeclaredFieldAfterReencode() {
		Value.registerDynamicFields();
		var source = new Value();
		var amountLog = (LogDynamic)source.getAmount().createLogBean();
		amountLog.replace(7, new Amount(12));
		var quantityLog = (LogDynamic)source.getQuantity().createLogBean();
		quantityLog.replace(7, new Quantity(1L << 40));
		var root = new LogBean();
		root.getVariablesOrNew().put(1, amountLog);
		root.getVariablesOrNew().put(2, quantityLog);

		var forwarded = new LogBean();
		forwarded.decode(encode(root));
		var received = new LogBean();
		received.decode(encode(forwarded));
		var follower = new Value();
		follower.followerApply(received);
		assertEquals(12, assertInstanceOf(Amount.class, follower.getAmount().getBean()).getNumber());
		assertEquals(1L << 40, assertInstanceOf(Quantity.class, follower.getQuantity().getBean()).getNumber());
		assertThrows(IllegalArgumentException.class, () -> follower.getQuantity().followerApply(amountLog));
	}

	@Test
	public void rejectsUnregisteredFieldsAndUnknownReplacementTypes() {
		var unregistered = new DynamicField(5930482529L, 1, Bean::typeId, ignored -> null);
		var child = (LogDynamic)unregistered.newValue().createLogBean();
		child.replace(0, null);
		var root = new LogBean();
		root.getVariablesOrNew().put(1, child);
		assertThrows(IllegalStateException.class, () -> new LogBean().decode(encode(root)));

		Value.registerDynamicFields();
		var unknownType = (LogDynamic)new Value().getAmount().createLogBean();
		unknownType.replace(99, new Amount(12));
		root.getVariablesOrNew().put(1, unknownType);
		assertThrows(IllegalStateException.class, () -> new LogBean().decode(encode(root)));
	}

	@Test
	public void rejectsUnknownCustomTypeBeforeWritingReplacementLog() {
		var field = new DynamicField(5930482530L, 1,
				bean -> bean instanceof Amount ? 7 : 99, type -> type == 7 ? new Amount() : null);
		var error = assertRejectedCustomReplacement(field.newValue(), new Amount(12), 7);
		assertTrue(error.getMessage().contains("unknown RocksRaft dynamic type 99"));
	}

	@Test
	public void rejectsMismatchedCustomTypeBeforeWritingReplacementLog() {
		var field = new DynamicField(5930482530L, 2, bean -> 7, type -> new Amount());
		var error = assertRejectedCustomReplacement(field.newValue(), new Amount(12), 7);
		assertTrue(error.getMessage().contains(Amount.class.getName()));
		assertTrue(error.getMessage().contains(Quantity.class.getName()));
	}

	@Test
	public void rejectsUndeclaredPayloadFromGeneratedCustomFactory() {
		var error = assertRejectedCustomReplacement(new Value().getCustom(), new CustomPayload(), 71);
		assertTrue(error.getMessage().contains("undeclared RocksRaft dynamic payload"));
		assertTrue(error.getMessage().contains(Quantity.class.getName()));
	}

	private static IllegalArgumentException assertRejectedCustomReplacement(
			DynamicBean value, Bean original, long originalType) {
		value.setBean(original);
		value.initRootInfo(new Record.RootInfo(null, new TableKey("DynamicCustomMapping", 1)), null);
		var replacement = new Quantity(34);
		var transaction = Transaction.create();
		try {
			transaction.begin();
			var error = assertThrows(IllegalArgumentException.class, () -> value.setBean(replacement));
			assertFalse(replacement.isManaged());
			assertNull(transaction.getLog(value.objectId()));
			assertSame(original, value.getBean());
			assertEquals(originalType, value.getSpecialTypeId());
			return error;
		} finally {
			Transaction.destroy();
		}
	}

	@Test
	public void rejectsAnEditAgainstAnotherPayloadType() {
		var value = new Value();
		value.getAmount().setBean(new Quantity(33));
		var edit = (LogDynamic)value.getAmount().createLogBean();
		edit.specialTypeId = 7;
		edit.edit = new LogBean();
		assertThrows(IllegalStateException.class, () -> value.getAmount().followerApply(edit));
		assertEquals(33, assertInstanceOf(Quantity.class, value.getAmount().getBean()).getNumber());
	}

	@Test
	public void fullValueDecodeResetsOmittedFieldsAndRejectsForeignWrappers() {
		var value = new Value();
		var listItem = Value.newDynamicBean_Amounts();
		listItem.setBean(new Amount(20));
		var mapItem = Value.newDynamicBean_Quantities();
		mapItem.setBean(new Quantity(30));
		var sortedItem = Value.newDynamicBean_SortedAmounts();
		assertThrows(IllegalArgumentException.class, () -> value.getAmounts().add(sortedItem));
		assertThrows(IllegalArgumentException.class, () -> value.getQuantities().put(1, listItem));
		assertThrows(IllegalArgumentException.class, () -> value.getSortedAmounts().put(1, listItem));

		value.getAmount().setBean(new Amount(10));
		value.getAmounts().add(listItem);
		value.getQuantities().put(1, mapItem);
		value.getSortedAmounts().put(1, sortedItem);
		var decoded = new Value();
		decoded.decode(encode(value));
		assertEquals(10, assertInstanceOf(Amount.class, decoded.getAmount().getBean()).getNumber());
		var listValue = decoded.getAmounts().get(0).getBean();
		assertNotNull(listValue);
		assertEquals(20, assertInstanceOf(Amount.class, listValue).getNumber());
		var mapItemAfterDecode = decoded.getQuantities().get(1);
		assertNotNull(mapItemAfterDecode);
		var mapValue = mapItemAfterDecode.getBean();
		assertNotNull(mapValue);
		assertEquals(30, assertInstanceOf(Quantity.class, mapValue).getNumber());
		var sortedItemAfterDecode = decoded.getSortedAmounts().get(1);
		assertNotNull(sortedItemAfterDecode);
		assertTrue(sortedItemAfterDecode.isEmpty());

		decoded.decode(encode(new Value()));
		assertTrue(decoded.getAmount().isEmpty());
		assertTrue(decoded.getAmounts().isEmpty());
		assertEquals(0, decoded.getQuantities().size());
		assertEquals(0, decoded.getSortedAmounts().size());
	}

	@Test
	public void fullValueDecodeReplacesSharedBeanKey() {
		var source = new BValue();
		source.setBeankey(new BeanKey(42, ""));
		var bytes = encode(source).Copy();

		var fresh = new BValue();
		fresh.decode(ByteBuffer.Wrap(bytes));

		var reused = new BValue();
		var oldKey = new BeanKey(1, "old");
		reused.setBeankey(oldKey);
		var sibling = reused.copy();
		reused.decode(ByteBuffer.Wrap(bytes));

		assertEquals(new BeanKey(42, ""), fresh.getBeankey());
		assertEquals(fresh.getBeankey(), reused.getBeankey());
		assertSame(oldKey, sibling.getBeankey());
		assertEquals(new BeanKey(1, "old"), sibling.getBeankey());
	}

	@Test
	public void fullValueDecodeResetsMissingNestedBeanAndSortedMap() {
		var value = new Value();
		var nested = value.getNested();
		nested.getPayload().setBean(new Quantity(42));
		nested.getSortedNumbers().put(1, 11L);
		nested.setScratchNumber(77);

		value.decode(encode(new Value()));
		assertSame(nested, value.getNested());
		assertTrue(nested.getPayload().isEmpty());
		assertEquals(0, nested.getSortedNumbers().size());
		assertEquals(77, nested.getScratchNumber());

		nested.getSortedNumbers().put(2, 22L);
		var source = new Value();
		source.getNested().getPayload().setBean(new Quantity(43));
		value.decode(encode(source));
		assertEquals(43, assertInstanceOf(Quantity.class, nested.getPayload().getBean()).getNumber());
		assertEquals(0, nested.getSortedNumbers().size());
		assertEquals(77, nested.getScratchNumber());
	}

	@Test
	public void omittedRocksChildAndKeyUseWireDefaults() {
		byte[] legacyBytes = {0};
		var legacyNonzero = HexFormat.of().parseHex("1610070000");
		var source = new LegacyRocksDecodeParent();
		assertEquals(7, source.getChild().getNumber());
		assertEquals(new LegacyDecodeKey(3), source.getKeyValue());
		source.setKeyValue(new LegacyDecodeKey(0));
		assertArrayEquals(legacyNonzero, encode(source).Copy());
		source.getChild().setNumber(0);
		assertArrayEquals(legacyBytes, encode(source).Copy());

		var fresh = new LegacyRocksDecodeParent();
		fresh.decode(ByteBuffer.Wrap(legacyBytes));
		var reused = new LegacyRocksDecodeParent();
		var child = reused.getChild();
		child.setNumber(99);
		child.setScratch(77);
		child.getEntries().add(1);
		var shared = new LegacyDecodeKey(99);
		reused.setKeyValue(shared);
		var sibling = new LegacyRocksDecodeParent();
		sibling.setKeyValue(shared);
		reused.decode(ByteBuffer.Wrap(legacyBytes));

		assertSame(child, reused.getChild());
		assertEquals(0, fresh.getChild().getNumber());
		assertEquals(fresh.getChild().getNumber(), child.getNumber());
		assertEquals(77, child.getScratch());
		assertEquals(0, child.getEntries().size());
		assertEquals(new LegacyDecodeKey(0), fresh.getKeyValue());
		assertEquals(fresh.getKeyValue(), reused.getKeyValue());
		assertSame(shared, sibling.getKeyValue());
		assertEquals(99, shared.getNumber());
		assertArrayEquals(legacyBytes, encode(fresh).Copy());
		assertArrayEquals(legacyBytes, encode(reused).Copy());

		fresh.decode(ByteBuffer.Wrap(legacyNonzero));
		reused.decode(ByteBuffer.Wrap(legacyNonzero));
		assertSame(child, reused.getChild());
		assertEquals(7, fresh.getChild().getNumber());
		assertEquals(7, child.getNumber());
		assertEquals(77, child.getScratch());
		assertArrayEquals(legacyNonzero, encode(reused).Copy());
	}

	@Test
	public void fullValueDecodeUsesWireDefaultsWithoutChangingSchemaInitializers() {
		var defaults = new Defaults();
		assertEquals(7, defaults.getCount());
		assertTrue(defaults.isEnabled());
		assertEquals("schema", defaults.getLabel());
		assertEquals(new Vector2(1, 2), defaults.getPosition());
		assertEquals(9, defaults.getOverride().getNumber());
		var encodedDefaults = encode(defaults);

		defaults.setCount(20);
		defaults.setLabel("stale");
		defaults.getOverride().setNumber(99);
		defaults.decode(ByteBuffer.Wrap(new byte[1]));
		assertEquals(0, defaults.getCount());
		assertFalse(defaults.isEnabled());
		assertEquals("", defaults.getLabel());
		assertEquals(Vector2.ZERO, defaults.getPosition());
		assertEquals(0, defaults.getOverride().getNumber());

		defaults.decode(encodedDefaults);
		assertEquals(7, defaults.getCount());
		assertTrue(defaults.isEnabled());
		assertEquals("schema", defaults.getLabel());
		assertEquals(new Vector2(1, 2), defaults.getPosition());
		assertEquals(9, defaults.getOverride().getNumber());

		var explicit = new Defaults(0, false, "", Vector2.ZERO);
		explicit.getOverride().setNumber(0);
		var encodedZero = encode(explicit);
		assertArrayEquals(new byte[1], encodedZero.Copy());
		defaults.decode(encodedZero);
		assertEquals(0, defaults.getCount());
		assertFalse(defaults.isEnabled());
		assertEquals("", defaults.getLabel());
		assertEquals(Vector2.ZERO, defaults.getPosition());
		assertEquals(0, defaults.getOverride().getNumber());
	}

	@Test
	public void cacheStateDecodesLegacyOmittedModifyAsServerZero() {
		// The old writer omitted Modify=0 and an empty Share set.
		byte[] legacyServerZero = {0};
		var restored = new BCacheState();
		assertEquals(-1, restored.getModify());
		restored.decode(ByteBuffer.Wrap(legacyServerZero));
		assertEquals(0, restored.getModify());
		restored.setModify(5);
		restored.decode(ByteBuffer.Wrap(legacyServerZero));
		assertEquals(0, restored.getModify());

		var cacheState = new BCacheState();
		cacheState.setModify(0);
		var encodedZero = encode(cacheState);
		assertArrayEquals(legacyServerZero, encodedZero.Copy());
		restored.decode(encodedZero);
		assertEquals(0, restored.getModify());
		restored.decode(encode(new BCacheState()));
		assertEquals(-1, restored.getModify());
	}

	@Test
	public void copiesRocksBeanCollectionsInsideDynamicPayload() {
		var original = new Value();
		var payload = new CustomPayload();
		payload.getList().add(new Quantity(11));
		payload.getMap().put(1, new Quantity(11));
		payload.getSortedMap().put(2, new Quantity(11));
		original.getCustom().setBean(payload);

		var copy = original.copy();
		var copiedPayload = assertInstanceOf(CustomPayload.class, copy.getCustom().getBean());
		var originalValues = collectionValues(payload);
		var copiedValues = collectionValues(copiedPayload);
		for (int i = 0; i < originalValues.size(); i++) {
			var originalValue = originalValues.get(i);
			var copiedValue = copiedValues.get(i);
			assertEquals(11, copiedValue.getNumber());
			copiedValue.setNumber(22);
			assertEquals(11, originalValue.getNumber());
			originalValue.setNumber(33);
			assertEquals(22, copiedValue.getNumber());
		}

		var originalRoot = new Record.RootInfo(null, new TableKey("DynamicCopy", 1));
		var copyRoot = new Record.RootInfo(null, new TableKey("DynamicCopy", 2));
		original.initRootInfo(originalRoot, null);
		copy.initRootInfo(copyRoot, null);
		for (var value : originalValues)
			assertSame(originalRoot, value.rootInfo());
		for (var value : copiedValues)
			assertSame(copyRoot, value.rootInfo());
	}

	private static List<Quantity> collectionValues(CustomPayload payload) {
		return List.of(payload.getList().get(0), requireNonNull(payload.getMap().get(1)),
				requireNonNull(payload.getSortedMap().get(2)));
	}

	@Test
	public void selfAssignKeepsCollectionsWithAndWithoutTransaction() {
		var payload = new CustomPayload();
		payload.getList().add(new Quantity(11));
		payload.getMap().put(1, new Quantity(12));
		payload.getSortedMap().put(2, new Quantity(13));
		var originalValues = collectionValues(payload);
		payload.assign(payload);
		assertEquals(originalValues, collectionValues(payload));

		payload.initRootInfo(new Record.RootInfo(null, new TableKey("DynamicSelfAssign", 1)), null);
		var transaction = Transaction.create();
		try {
			transaction.begin();
			payload.assign(payload);
			assertEquals(originalValues, collectionValues(payload));
		} finally {
			Transaction.destroy();
		}
	}

	@Test
	public void copyUsesNaturalOrderInsideNestedAndDynamicBeans() {
		var original = new Value();
		var nested = original.getNested().getSortedNumbers();
		nested.put(2, 22L);
		nested.put(1, 11L);

		var payload = new CustomPayload();
		var dynamic = payload.getSortedMap();
		dynamic.put(2, new Quantity(22));
		dynamic.put(1, new Quantity(11));
		original.getCustom().setBean(payload);

		var copy = original.copy();
		var nestedCopy = copy.getNested().getSortedNumbers();
		var dynamicCopy = assertInstanceOf(CustomPayload.class, copy.getCustom().getBean()).getSortedMap();
		assertEquals(List.of(1, 2), List.copyOf(nestedCopy.keys()));
		assertEquals(List.of(1, 2), List.copyOf(dynamicCopy.keys()));
		assertNull(nestedCopy.get(-1));
		assertNull(dynamicCopy.get(-1));
		assertEquals(11L, requireNonNull(nestedCopy.get(1)).longValue());
		assertEquals(11, requireNonNull(dynamicCopy.get(1)).getNumber());
	}

	@Test
	public void rejectsManagedPayloadWhenBuildingAnUnmanagedValue() {
		var owner = new Value();
		owner.getAmount().setBean(new Amount(9));
		owner.initRootInfo(new Record.RootInfo(null, new TableKey("DynamicOwnership", 1)), null);

		var other = new Value();
		assertThrows(HasManagedException.class,
				() -> other.getAmount().setBean(requireNonNull(owner.getAmount().getBean())));
		assertTrue(other.getAmount().isEmpty());
		other.getAmount().assign(owner.getAmount());
		assertEquals(9, assertInstanceOf(Amount.class, other.getAmount().getBean()).getNumber());
	}

	@Test
	public void nestedRocksBeanAttachesDynamicFieldToRecord() {
		var value = new Value();
		value.initRootInfo(new Record.RootInfo(null, new TableKey("DynamicNested", 1)), null);
		assertTrue(value.getNested().getPayload().isManaged());
	}

	@Test
	public void transientDynamicValueStaysUnmanagedAndOutsideFullValueEncoding() {
		var value = new Value();
		value.initRootInfo(new Record.RootInfo(null, new TableKey("DynamicTransient", 1)), null);
		value.getScratch().setBean(new Amount(71));
		assertFalse(value.getScratch().isManaged());
		assertEquals(71, assertInstanceOf(Amount.class, value.getScratch().getBean()).getNumber());

		var decoded = new Value();
		decoded.decode(encode(value));
		assertTrue(decoded.getScratch().isEmpty());
	}

	@Test
	public void plainLogBeanStillUsesTheOriginalChildHeader() {
		Log.register(LogInt::new);
		var child = new LogInt(new Amount(), 1, 1234);
		var oldBytes = ByteBuffer.Allocate();
		oldBytes.WriteUInt(1);
		oldBytes.WriteInt4(child.getTypeId());
		oldBytes.WriteUInt(1);
		oldBytes.WriteInt(1234);

		var decoded = new LogBean();
		decoded.decode(ByteBuffer.Wrap(oldBytes.Copy()));
		assertArrayEquals(oldBytes.Copy(), encode(decoded).Copy());
	}

	private static ByteBuffer encode(Serializable value) {
		var bb = ByteBuffer.Allocate();
		value.encode(bb);
		return ByteBuffer.Wrap(bb.Copy());
	}
}
