package Zeze.Util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import Zeze.Net.Binary;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Serializable;
import Zeze.Serialize.Vector2;
import Zeze.Serialize.Vector2Int;
import Zeze.Serialize.Vector3;
import Zeze.Serialize.Vector3Int;
import Zeze.Serialize.Vector4;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;

public class Reflect {
	public static final boolean inDebugMode = !"true".equals(System.getProperty("noDebugMode")) &&
			ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
	public static final StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
	private static final HashMap<Class<?>, String> stableNameMap = new HashMap<>(32);

	static {
		stableNameMap.put(boolean.class, "bool");
		stableNameMap.put(Boolean.class, "bool");
		stableNameMap.put(byte.class, "byte");
		stableNameMap.put(Byte.class, "byte");
		stableNameMap.put(short.class, "short");
		stableNameMap.put(Short.class, "short");
		stableNameMap.put(int.class, "int");
		stableNameMap.put(Integer.class, "int");
		stableNameMap.put(long.class, "long");
		stableNameMap.put(Long.class, "long");
		stableNameMap.put(float.class, "float");
		stableNameMap.put(Float.class, "float");
		stableNameMap.put(double.class, "double");
		stableNameMap.put(Double.class, "double");
		stableNameMap.put(Binary.class, "binary");
		stableNameMap.put(String.class, "string");
		stableNameMap.put(char.class, "char");
		stableNameMap.put(Character.class, "char");
		stableNameMap.put(Vector2.class, "vector2");
		stableNameMap.put(Vector3.class, "vector3");
		stableNameMap.put(Vector4.class, "vector4");
		stableNameMap.put(Quaternion.class, "quaternion");
		stableNameMap.put(Vector2Int.class, "vector2int");
		stableNameMap.put(Vector3Int.class, "vector3int");
	}

	private final HashMap<String, Method> methods = new HashMap<>();

	public Reflect(Class<?> cls) {
		for (var method : cls.getDeclaredMethods()) {
			if (method.getName().startsWith("Process")) { // 只有协议处理函数能配置TransactionLevel
				if (null != methods.putIfAbsent(method.getName(), method))
					throw new IllegalStateException("Duplicate Method Name Of Protocol Handle: " + method.getName());
			}
		}
	}

	public static MethodHandle getDefaultConstructor(Class<?> cls) {
		try {
			return MethodHandles.lookup().findConstructor(cls, MethodType.methodType(void.class));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public TransactionLevel getTransactionLevel(String methodName, TransactionLevel def) {
		var method = methods.get(methodName);
		if (null == method)
			return def;

		var annotation = method.getAnnotation(TransactionLevelAnnotation.class);
		return annotation != null ? annotation.Level() : def;
	}

	public DispatchMode getDispatchMode(String methodName, DispatchMode def) {
		var method = methods.get(methodName);
		if (null == method)
			return def;

		var annotation = method.getAnnotation(DispatchModeAnnotation.class);
		return annotation != null ? annotation.mode() : def;
	}

	public static String getStableName(Class<?> cls) {
		// 支持的 Zeze/Gen/Types/ 类型。
		var name = stableNameMap.get(cls);
		if (name != null)
			return name;
		if (Serializable.class.isAssignableFrom(cls))
			return cls.getName();
		throw new UnsupportedOperationException("Unsupported type: " + cls.getName());
	}

	public static String getStableName(Class<?> cls, Class<?> tplCls) {
		return cls.getName() + '<' + getStableName(tplCls) + '>';
	}

	public static String getStableName(Class<?> cls, Class<?> tplCls1, Class<?> tplCls2) {
		return cls.getName() + '<' + getStableName(tplCls1) + ", " + getStableName(tplCls2) + '>';
	}

	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj) {
		return (T)obj;
	}
}
