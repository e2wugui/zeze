package Zeze.World.Graphics;

import java.util.ArrayList;
import java.util.List;
import Zeze.Serialize.Vector3;

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
}
