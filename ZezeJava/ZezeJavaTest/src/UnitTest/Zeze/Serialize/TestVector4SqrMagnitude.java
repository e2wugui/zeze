package UnitTest.Zeze.Serialize;

import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Vector4;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * SE2-F1 回归：Vector4覆写了magnitude()/normalized()（含w²）但未覆写sqrMagnitude()，
 * 继承Vector3三分量口径——(0,0,0,2)得magnitude=2而sqrMagnitude=0。
 * 修复：Vector4覆写sqrMagnitude()补(double)w*w，与4分量口径对齐（Quaternion随继承自然正确）。
 */
@Fast
public class TestVector4SqrMagnitude {
	@Test
	public void testSqrMagnitudeIncludesW() {
		var v = new Vector4(0, 0, 0, 2);
		Assertions.assertEquals(4.0f, v.sqrMagnitude(), 1e-6f, "w分量必须计入平方和");
		Assertions.assertEquals(v.magnitude(), (float)Math.sqrt(v.sqrMagnitude()), 1e-6f,
				"magnitude必须等于sqrt(sqrMagnitude)");

		var v2 = new Vector4(1, 2, 3, 4);
		Assertions.assertEquals(30.0f, v2.sqrMagnitude(), 1e-6f);

		// Quaternion继承Vector4：随覆写自然正确
		var q = new Quaternion(0, 0, 0, 1);
		Assertions.assertEquals(1.0f, q.sqrMagnitude(), 1e-6f);
		Assertions.assertEquals(q.magnitude(), (float)Math.sqrt(q.sqrMagnitude()), 1e-6f);

		// normalized与sqrMagnitude口径一致：单位化后模为1
		var n = v2.normalized();
		Assertions.assertEquals(1.0f, n.magnitude(), 1e-6f);
	}
}
