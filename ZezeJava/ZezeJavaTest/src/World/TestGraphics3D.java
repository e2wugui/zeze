package World;

import java.util.ArrayList;
import Zeze.Serialize.Vector3;
import Zeze.World.Graphics.Graphics3D;
import Zeze.World.Graphics.Matrix3x3f;
import org.junit.Test;

public class TestGraphics3D {
	@Test
	public float test(Vector3 from, Vector3 to) {
		var m = new Matrix3x3f();
		m.setFromToRotation(from, to);
		var v = m.multiplyVector(from);
		System.out.println("from : " + from + ", to : " + to  + ", m.multiplyVector : " + v + ", m : \n" + m);
		float magnitude = v.subtract(to).magnitude();
		if (magnitude > 1e-2f) {
			throw new java.lang.RuntimeException("result not match, magnitude : " + magnitude);
		}
		return magnitude;
	}

	@Test
	public void testTransformPolygon() {
		var polygon = new ArrayList<Vector3>();
		polygon.add(new Vector3(0.12f, -0.34f, 0.78f));
		polygon.add(new Vector3(-0.26f, 56.76f, -986.23f));
		polygon.add(new Vector3(123.23f, -7.45f, 2.3f));

		var origin = new Vector3(-3.5f, -12.3f, 12.87f);
		var from = new Vector3(-2.3f, 1.5f, -0.7f);
		var to = new Vector3(1.2f, -0.6f, 1f);
		var newOrigin = new Vector3(2f, 1.287f, -4.87f);

		var result = Graphics3D.transformPolygon(polygon, origin, from, newOrigin, to);
		for (var vector3 : result) {
			System.out.println(vector3);
		}
	}
}
