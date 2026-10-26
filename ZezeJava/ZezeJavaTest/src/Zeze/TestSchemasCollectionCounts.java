package Zeze;

import java.util.function.Consumer;
import java.util.ArrayList;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.NioByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestSchemasCollectionCounts {
	private static ByteBuffer variablePrefix() {
		var bb = ByteBuffer.Allocate();
		bb.WriteInt(1);
		for (var s : new String[]{"field", "dynamic", "", ""})
			bb.WriteString(s);
		return bb;
	}

	private static ByteBuffer beanPrefix() {
		var bb = ByteBuffer.Allocate();
		bb.WriteString("Bean");
		bb.WriteBool(false);
		return bb;
	}

	private static void rejects(Consumer<IByteBuffer> decode, ByteBuffer bytes) {
		assertAll(
				() -> assertThrows(IllegalArgumentException.class, () -> decode.accept(ByteBuffer.Wrap(bytes.Copy()))),
				() -> assertThrows(IllegalArgumentException.class, () -> decode.accept(NioByteBuffer.Wrap(bytes.Copy()))));
	}

	@Test
	void rejectsNegativeCountsAtEverySchemaCollectionBoundary() {
		var cases = new ArrayList<Executable>();
		for (int count : new int[]{-1, Integer.MIN_VALUE}) {
			var tables = ByteBuffer.Allocate();
			tables.WriteInt(count);
			tables.WriteInt(0);
			cases.add(() -> rejects(bb -> new Schemas().decode(bb), tables));
			var beans = ByteBuffer.Allocate();
			beans.WriteInt(0);
			beans.WriteInt(count);
			cases.add(() -> rejects(bb -> new Schemas().decode(bb), beans));
			var variables = beanPrefix();
			variables.WriteInt(count);
			variables.WriteInt(0);
			cases.add(() -> rejects(bb -> new Schemas.Bean().decode(bb), variables));
			var deleted = beanPrefix();
			deleted.WriteInt(0);
			deleted.WriteInt(count);
			cases.add(() -> rejects(bb -> new Schemas.Bean().decode(bb), deleted));
			var dynamics = variablePrefix();
			dynamics.WriteInt(count);
			cases.add(() -> rejects(bb -> new Schemas.Variable().decode(bb), dynamics));
		}
		assertAll(cases);
	}

	@Test
	void preservesLegacyEmptySchemasAndSignedCountEncoding() {
		var legacy = ByteBuffer.Allocate();
		legacy.WriteInt(0);
		legacy.WriteInt(0); // 旧格式没有 appVersion 尾部。
		var empty = new Schemas();
		empty.decode(legacy);
		assertEquals(0, empty.getAppVersion());

		var schema = new Schemas();
		var bean = new Schemas.Bean("Bean", false);
		var variable = new Schemas.Variable();
		variable.id = 1;
		variable.name = "field";
		variable.typeName = "dynamic";
		variable.dynamicBeans.put(7, "Payload");
		bean.addVariable(variable);
		var deleted = new Schemas.Variable();
		deleted.id = 2;
		deleted.name = "old";
		deleted.typeName = "long";
		bean.getDeletedVars().put(2, deleted);
		schema.addBean(bean);
		schema.addTable(new Schemas.Table("Table", "long", "Bean"));
		schema.setAppVersion(123);
		var bytes = ByteBuffer.Allocate();
		schema.encode(bytes);
		for (IByteBuffer input : new IByteBuffer[]{ByteBuffer.Wrap(bytes.Copy()),
				NioByteBuffer.Wrap(bytes.Copy())}) {
			var decoded = new Schemas();
			decoded.decode(input);
			assertEquals(123, decoded.getAppVersion());
			assertEquals("Bean", decoded.tables.get("Table").valueName);
			assertEquals("Payload", decoded.beans.get("Bean").getVariables().get(1).dynamicBeans.get(7));
			assertEquals("old", decoded.beans.get("Bean").getDeletedVars().get(2).name);
			assertEquals(input.getWriteIndex(), input.getReadIndex());
		}
	}
}
