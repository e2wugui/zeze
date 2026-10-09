package Zeze.Serialize;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Vector4的四维加减：继承自Vector3的add/sub/subtract只运算x/y/z并返回
 * Vector3——静默丢w且结果降维。四维重载后，静态类型为Vector4的实参必须
 * 得到四维结果；经Vector3静态类型调用的旧路径保持三维语义（二进制兼容）。
 */
@Fast
public class TestVector4AddSubKeepsW {

	@Test
	public void vector4OverloadsKeepAllFourComponents() {
		var a = new Vector4(1, 2, 3, 4);
		var b = new Vector4(5, 6, 7, 8);

		var sum = a.add(b);
		assertEquals(Vector4.class, sum.getClass(), "四维加法必须返回Vector4");
		assertEquals(6, sum.x);
		assertEquals(8, sum.y);
		assertEquals(10, sum.z);
		assertEquals(12, sum.w, "加法不得丢失w分量");

		var diff = a.sub(b);
		assertEquals(Vector4.class, diff.getClass());
		assertEquals(-4, diff.x);
		assertEquals(-4, diff.y);
		assertEquals(-4, diff.z);
		assertEquals(-4, diff.w, "减法不得丢失w分量");

		var alias = a.subtract(b);
		assertEquals(-4, alias.w, "subtract与sub同语义");
	}

	/** 旧三维路径语义保持：Vector4实参经Vector3静态类型调用仍返回三维结果。 */
	@Test
	public void vector3StaticTypePathKeepsLegacySemantics() {
		Vector3 a = new Vector4(1, 2, 3, 4);
		Vector3 b = new Vector4(5, 6, 7, 8);
		var sum = a.add(b);
		assertEquals(Vector3.class, sum.getClass(), "Vector3静态类型路径保持旧三维返回");
		assertEquals(6, sum.x);
		assertEquals(8, sum.y);
		assertEquals(10, sum.z);
	}
}
