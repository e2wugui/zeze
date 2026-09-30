package Zeze.Util;

import java.lang.reflect.InvocationTargetException;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestVectorListUnitOverflow {
	private record Shape(Class<?> type, int dimension, boolean integers) {
		Class<?> arrayType() { return integers ? int[].class : float[].class; }
		Object sample() {
			if (integers) {
				var data = new int[dimension * 2];
				for (var i = 0; i < data.length; i++) data[i] = i + 1;
				return data;
			}
			var data = new float[dimension * 2];
			for (var i = 0; i < data.length; i++) data[i] = i + 1;
			return data;
		}
		Object list() throws Exception { return type.getConstructor(arrayType()).newInstance(sample()); }
	}

	private static List<Shape> shapes() {
		return List.of(new Shape(Vector2List.class, 2, false), new Shape(Vector3List.class, 3, false),
				new Shape(Vector4List.class, 4, false), new Shape(Vector2IntList.class, 2, true),
				new Shape(Vector3IntList.class, 3, true));
	}

	private static void unchanged(Shape shape, Object list) throws Exception {
		var actual = shape.type.getMethod("toArray").invoke(list);
		if (shape.integers) assertArrayEquals((int[])shape.sample(), (int[])actual);
		else assertArrayEquals((float[])shape.sample(), (float[])actual);
	}

	private static void arithmeticOverflow(Shape shape, Object list, String method, Class<?>[] types, Object... args) {
		var error = assertThrows(InvocationTargetException.class,
				() -> shape.type.getMethod(method, types).invoke(list, args));
		assertInstanceOf(ArithmeticException.class, error.getCause(), shape.type.getSimpleName() + '.' + method);
	}

	@Test
	public void enormousRemovalIndicesCannotDeleteOrExchangeARealVector() throws Exception {
		for (var shape : shapes()) {
			var list = shape.list();
			for (var method : List.of("removeVector", "removeAndExchangeLastVector")) {
				for (var index : new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE, 1 << 30, 1_431_655_766}) {
					shape.type.getMethod(method, int.class).invoke(list, index);
					unchanged(shape, list);
				}
			}
			shape.type.getMethod("eraseVector", int.class, int.class).invoke(list, Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
			unchanged(shape, list);
		}
	}

	@Test
	public void overflowingVectorCountsFailBeforeChangingTheExistingData() throws Exception {
		for (var shape : shapes()) {
			var list = shape.list();
			var count = Integer.MAX_VALUE / shape.dimension + 1;
			for (var method : List.of("resizeVector", "reserveVector", "reserveSpaceVector", "shrinkVector")) {
				arithmeticOverflow(shape, list, method, new Class<?>[]{int.class}, count);
				unchanged(shape, list);
			}
			arithmeticOverflow(shape, list, "wrapsVector", new Class<?>[]{shape.arrayType(), int.class}, shape.sample(), count);
			unchanged(shape, list);
			arithmeticOverflow(shape, list, "replaceVector", new Class<?>[]{shape.arrayType(), int.class, int.class}, shape.sample(), 0, count);
			unchanged(shape, list);
			arithmeticOverflow(shape, list, "toArrayVector", new Class<?>[]{int.class, int.class}, 0, count);
			unchanged(shape, list);
		}
	}

	@Test
	public void bulkGrowthChecksBothMultiplicationAndAdditionBeforeIteration() throws Exception {
		for (var shape : shapes()) {
			var list = shape.list();
			for (var size : new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE / shape.dimension}) {
				Collection<Object> enormous = new AbstractCollection<>() {
					@Override public int size() { return size; }
					@Override public Iterator<Object> iterator() { throw new AssertionError("overflow must be rejected before iteration"); }
				};
				arithmeticOverflow(shape, list, "addAllVector", new Class<?>[]{Collection.class}, enormous);
				unchanged(shape, list);
			}
		}
	}

	@Test
	public void directComponentIndicesCannotWrapBackToTheFirstVector() throws Exception {
		for (var shape : shapes()) {
			var list = shape.list();
			var index = Integer.MAX_VALUE / shape.dimension + 1;
			arithmeticOverflow(shape, list, "getX", new Class<?>[]{int.class}, index);
			arithmeticOverflow(shape, list, "setX", new Class<?>[]{int.class, shape.integers ? int.class : float.class},
					index, shape.integers ? (Object)99 : (Object)99f);
			unchanged(shape, list);
		}
	}
}
