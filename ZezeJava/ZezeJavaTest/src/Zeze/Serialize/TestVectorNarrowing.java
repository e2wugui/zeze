package Zeze.Serialize;

import java.util.HashMap;

import Zeze.Transaction.Logs.LogQuaternionValue;
import Zeze.Transaction.Logs.LogVector3IntValue;
import Zeze.Transaction.Logs.LogVector3Value;
import Zeze.Transaction.Logs.LogVector4Value;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 向量窄化解码（IByteBuffer.ReadVector*）与日志回放（LogVector*Value / LogQuaternionValue）
 * 必须投影为精确目标值类型，满足 Vector* 的精确类 equals/hashCode/isZero 契约。
 */
@Fast
public class TestVectorNarrowing {

	private static void assertExactVector2(Vector2 actual, float x, float y) {
		var expected = new Vector2(x, y);
		assertSame(Vector2.class, actual.getClass());
		assertEquals(expected, actual);
		assertEquals(actual, expected);
		assertEquals(expected.hashCode(), actual.hashCode());
		assertEquals(expected.isZero(), actual.isZero());
	}

	private static void assertExactVector3(Vector3 actual, float x, float y, float z) {
		var expected = new Vector3(x, y, z);
		assertSame(Vector3.class, actual.getClass());
		assertEquals(expected, actual);
		assertEquals(actual, expected);
		assertEquals(expected.hashCode(), actual.hashCode());
		assertEquals(expected.isZero(), actual.isZero());
	}

	private static void assertExactVector4(Vector4 actual, float x, float y, float z, float w) {
		var expected = new Vector4(x, y, z, w);
		assertSame(Vector4.class, actual.getClass());
		assertEquals(expected, actual);
		assertEquals(actual, expected);
		assertEquals(expected.hashCode(), actual.hashCode());
		assertEquals(expected.isZero(), actual.isZero());
	}

	private static void assertExactVector2Int(Vector2Int actual, int x, int y) {
		var expected = new Vector2Int(x, y);
		assertSame(Vector2Int.class, actual.getClass());
		assertEquals(expected, actual);
		assertEquals(actual, expected);
		assertEquals(expected.hashCode(), actual.hashCode());
		assertEquals(expected.isZero(), actual.isZero());
	}

	private static IByteBuffer[] buffersOf(ByteBuffer encoded) {
		return new IByteBuffer[]{
				ByteBuffer.Wrap(encoded.Copy()),
				NioByteBuffer.Wrap(encoded.Copy()),
		};
	}

	@Test
	public void testDecodeVector2NarrowingReturnsExactType() {
		var bb3 = ByteBuffer.Allocate();
		bb3.WriteVector3(new Vector3(1.5f, 2.5f, 9.0f));
		for (var buf : buffersOf(bb3)) {
			var v = buf.ReadVector2(ByteBuffer.VECTOR3);
			assertExactVector2(v, 1.5f, 2.5f);
			assertTrue(buf.isEmpty());
		}

		// 前两分量全零但 z 非零时，窄化为 Vector2 后 isZero() 必须为 true，且可作为 Map 键命中
		var bb3Zero = ByteBuffer.Allocate();
		bb3Zero.WriteVector3(new Vector3(0, 0, 5.0f));
		for (var buf : buffersOf(bb3Zero)) {
			var v = buf.ReadVector2(ByteBuffer.VECTOR3);
			assertExactVector2(v, 0, 0);
			assertTrue(v.isZero());
			var map = new HashMap<Vector2, String>();
			map.put(v, "hit");
			assertEquals("hit", map.get(Vector2.ZERO));
		}

		var bb4 = ByteBuffer.Allocate();
		bb4.WriteVector4(new Vector4(1.0f, 2.0f, 3.0f, 4.0f));
		for (var buf : buffersOf(bb4)) {
			var v = buf.ReadVector2(ByteBuffer.VECTOR4);
			assertExactVector2(v, 1.0f, 2.0f);
			assertTrue(buf.isEmpty());
		}

		var bb3i = ByteBuffer.Allocate();
		bb3i.WriteVector3Int(new Vector3Int(7, 8, 9));
		for (var buf : buffersOf(bb3i)) {
			var v = buf.ReadVector2(ByteBuffer.VECTOR3INT);
			assertExactVector2(v, 7.0f, 8.0f);
			assertTrue(buf.isEmpty());
		}
	}

	@Test
	public void testDecodeVector3NarrowingReturnsExactType() {
		var bb4 = ByteBuffer.Allocate();
		bb4.WriteVector4(new Vector4(1.0f, 2.0f, 3.0f, 4.0f));
		for (var buf : buffersOf(bb4)) {
			var v = buf.ReadVector3(ByteBuffer.VECTOR4);
			assertExactVector3(v, 1.0f, 2.0f, 3.0f);
			assertTrue(buf.isEmpty());
		}

		var bb4Zero = ByteBuffer.Allocate();
		bb4Zero.WriteVector4(new Vector4(0, 0, 0, 7.0f));
		for (var buf : buffersOf(bb4Zero)) {
			var v = buf.ReadVector3(ByteBuffer.VECTOR4);
			assertExactVector3(v, 0, 0, 0);
			assertTrue(v.isZero());
		}
	}

	@Test
	public void testDecodeVector2IntNarrowingReturnsExactType() {
		var bb3i = ByteBuffer.Allocate();
		bb3i.WriteVector3Int(new Vector3Int(3, 4, 5));
		for (var buf : buffersOf(bb3i)) {
			var v = buf.ReadVector2Int(ByteBuffer.VECTOR3INT);
			assertExactVector2Int(v, 3, 4);
			assertTrue(buf.isEmpty());
		}

		var bb3iZero = ByteBuffer.Allocate();
		bb3iZero.WriteVector3Int(new Vector3Int(0, 0, 5));
		for (var buf : buffersOf(bb3iZero)) {
			var v = buf.ReadVector2Int(ByteBuffer.VECTOR3INT);
			assertExactVector2Int(v, 0, 0);
			assertTrue(v.isZero());
		}

		var bb3 = ByteBuffer.Allocate();
		bb3.WriteVector3(new Vector3(3.9f, -4.9f, 5.5f));
		for (var buf : buffersOf(bb3)) {
			var v = buf.ReadVector2Int(ByteBuffer.VECTOR3);
			assertExactVector2Int(v, 3, -4);
			assertTrue(buf.isEmpty());
		}

		var bb4 = ByteBuffer.Allocate();
		bb4.WriteVector4(new Vector4(6.2f, 7.8f, 8.1f, 9.4f));
		for (var buf : buffersOf(bb4)) {
			var v = buf.ReadVector2Int(ByteBuffer.VECTOR4);
			assertExactVector2Int(v, 6, 7);
			assertTrue(buf.isEmpty());
		}
	}

	@Test
	public void testLogVectorNarrowingReturnsExactType() {
		var bb3 = ByteBuffer.Allocate();
		bb3.WriteVector3(new Vector3(1.5f, 2.5f, 3.5f));
		var logV3 = LogVector3Value.decode(1, bb3);
		assertExactVector2(logV3.vector2Value(), 1.5f, 2.5f);
		assertExactVector2Int(logV3.vector2IntValue(), 1, 2);

		var bb3Zero = ByteBuffer.Allocate();
		bb3Zero.WriteVector3(new Vector3(0, 0, 9.0f));
		var logV3Zero = LogVector3Value.decode(1, bb3Zero);
		assertTrue(logV3Zero.vector2Value().isZero());
		assertTrue(logV3Zero.vector2IntValue().isZero());

		var bb3i = ByteBuffer.Allocate();
		bb3i.WriteVector3Int(new Vector3Int(4, 5, 6));
		var logV3i = LogVector3IntValue.decode(2, bb3i);
		assertExactVector2(logV3i.vector2Value(), 4.0f, 5.0f);
		assertExactVector2Int(logV3i.vector2IntValue(), 4, 5);

		var bb4 = ByteBuffer.Allocate();
		bb4.WriteVector4(new Vector4(1.0f, 2.0f, 3.0f, 4.0f));
		var logV4 = LogVector4Value.decode(3, bb4);
		assertExactVector2(logV4.vector2Value(), 1.0f, 2.0f);
		assertExactVector2Int(logV4.vector2IntValue(), 1, 2);
		assertExactVector3(logV4.vector3Value(), 1.0f, 2.0f, 3.0f);

		var bbq = ByteBuffer.Allocate();
		bbq.WriteQuaternion(new Quaternion(1.0f, 2.0f, 3.0f, 4.0f));
		var logQ = LogQuaternionValue.decode(4, bbq);
		assertExactVector2(logQ.vector2Value(), 1.0f, 2.0f);
		assertExactVector2Int(logQ.vector2IntValue(), 1, 2);
		assertExactVector3(logQ.vector3Value(), 1.0f, 2.0f, 3.0f);
		assertExactVector4(logQ.vector4Value(), 1.0f, 2.0f, 3.0f, 4.0f);
	}
}
