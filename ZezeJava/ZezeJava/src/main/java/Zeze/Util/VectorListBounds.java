package Zeze.Util;

/** 向量单位转标量单位；分开容量、直接索引和可裁剪范围，禁止int乘法绕回后修改错误元素。 */
final class VectorListBounds {
	private VectorListBounds() {
	}

	static int count(int vectors, int dimension) {
		return Math.multiplyExact(Math.max(vectors, 0), dimension);
	}

	static int index(int vector, int dimension) {
		return Math.multiplyExact(vector, dimension);
	}

	static int index(int vector, int dimension, int component) {
		return Math.toIntExact((long)vector * dimension + component);
	}

	static int rangeIndex(int vector, int dimension) {
		return (int)Math.min((long)Math.max(vector, 0) * dimension, Integer.MAX_VALUE);
	}
}
