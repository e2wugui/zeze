package Zeze.World.Graphics;

import java.util.List;
import java.util.ArrayList;
import Zeze.Serialize.Vector3;
import Zeze.Serialize.Vector4;

/**
 * 3d 图形算法。
 */
public class Graphics3D {
	public static final Vector3 DirectionToZ = new Vector3(0, 0, 1);

	public static List<Vector3> transformPolygon(
			List<Vector3> polygon, Vector3 origin, Vector3 direction,
			Vector3 newOrigin, Vector3 newDirection) {

		var from = direction.normalized();
		var to = newDirection.normalized();
		Matrix3x3f rotationMatrix = new Matrix3x3f();
		rotationMatrix.setFromToRotation(from, to);
		var result = new ArrayList<Vector3>();
		for (var vector3 : polygon) {
			var offset = vector3.subtract(origin);
			var point = rotationMatrix.multiplyVector(offset);
			result.add(newOrigin.add(point));
		}
		return result;
	}

	public static Vector3 randomVector3() {
		float x = (float)Math.random() - 0.5f;
		float y = (float)Math.random() - 0.5f;
		float z = (float)Math.random() - 0.5f;
		return new Vector3(x * 2f, y * 2f, z * 2f);
	}

	public static float test(Vector3 from, Vector3 to) {
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

	public static void testTransformPolygon() {
		var polygon = new ArrayList<Vector3>();
		polygon.add(new Vector3(0.12f, -0.34f, 0.78f));
		polygon.add(new Vector3(-0.26f, 56.76f, -986.23f));
		polygon.add(new Vector3(123.23f, -7.45f, 2.3f));

		var origin = new Vector3(-3.5f, -12.3f, 12.87f);
		var from = new Vector3(-2.3f, 1.5f, -0.7f);
		var to = new Vector3(1.2f, -0.6f, 1f);
		var newOrigin = new Vector3(2f, 1.287f, -4.87f);

		var result = transformPolygon(polygon, origin, from, newOrigin, to);
		for (var vector3 : result) {
			System.out.println(vector3);
		}
	}

	public static void main(String[] args) {
		testTransformPolygon();
	}
}
