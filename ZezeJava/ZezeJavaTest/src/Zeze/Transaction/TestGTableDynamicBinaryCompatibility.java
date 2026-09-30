package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import demo.ModuleGTable.BValue;
import demo.ModuleGTable.Bean1;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestGTableDynamicBinaryCompatibility {

	@Test
	public void unknownDynamicTablesCanBeSkippedByOlderSchema() {
		var value = new BValue();
		var integerColumnValue = BValue.newDynamicBean_GTable3();
		var firstBean = new Bean1();
		firstBean.setIntVar(17);
		integerColumnValue.setBean(firstBean);
		value.getGTable3().put(1, 2, integerColumnValue);
		var stringColumnValue = BValue.newDynamicBean_GTable4();
		var secondBean = new Bean1();
		secondBean.setIntVar(29);
		stringColumnValue.setBean(secondBean);
		value.getGTable4().put(3, "tail", stringColumnValue);
		var buffer = ByteBuffer.Allocate();
		value.encode(buffer);

		var input = ByteBuffer.Wrap(buffer.Copy());
		assertDoesNotThrow(() -> new EmptyBean().decode(input));
		assertTrue(input.isEmpty(), "An older schema must skip both complete dynamic tables");
	}

	@Test
	public void dynamicTablesRoundTripWithTheirConcreteBeans() {
		var value = new BValue();
		var dynamic = BValue.newDynamicBean_GTable3();
		var bean = new Bean1();
		bean.setIntVar(17);
		dynamic.setBean(bean);
		value.getGTable3().put(1, 2, dynamic);
		var buffer = ByteBuffer.Allocate();
		value.encode(buffer);

		var input = ByteBuffer.Wrap(buffer.Copy());
		var decoded = new BValue();
		decoded.decode(input);
		assertTrue(input.isEmpty());
		assertEquals(17, ((Bean1)decoded.getGTable3().get(1, 2).getBean()).getIntVar());
	}

	@Test
	public void legacyBeanTaggedDynamicRowRemainsReadable() {
		var dynamic = BValue.newDynamicBean_GTable3();
		var bean = new Bean1();
		bean.setIntVar(47);
		dynamic.setBean(bean);
		// Older BeanMap2 encoders tagged dynamic values as BEAN, but still wrote the type id.
		var legacy = ByteBuffer.Allocate();
		legacy.WriteTag(0, 1, ByteBuffer.MAP);
		legacy.WriteMapType(1, ByteBuffer.INTEGER, ByteBuffer.BEAN);
		legacy.WriteInt(2);
		dynamic.encode(legacy);
		legacy.WriteByte(0);

		var input = ByteBuffer.Wrap(legacy.Copy());
		var row = new BValue().getGTable3().getPMap2().createValue();
		row.decode(input);
		assertTrue(input.isEmpty());
		assertEquals(47, ((Bean1)row.get(2).getBean()).getIntVar());
	}
}
