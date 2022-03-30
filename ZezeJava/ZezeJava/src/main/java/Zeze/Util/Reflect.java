package Zeze.Util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import Zeze.Net.Binary;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.TransactionLevel;

public class Reflect {
	private final HashMap<String, Method> Methods = new HashMap<>();

	public Reflect(Class<?> cls) {
		for (var method : cls.getDeclaredMethods()) {
			if (method.getName().startsWith("Process")) { // 只有协议处理函数能配置TransactionLevel
				if (null != Methods.putIfAbsent(method.getName(), method))
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
		var method = Methods.get(methodName);
		if (null == method)
			return def;

		var annotation = method.getAnnotation(Zeze.Util.TransactionLevel.class);
		if (null == annotation)
			return def;

		return TransactionLevel.valueOf(annotation.Level());
	}

	public static String GetStableName(Class<?> cls) {
		// 支持的 Zeze/Gen/Types/ 类型。
		if (cls == boolean.class || cls == Boolean.class)
			return "bool";
		if (cls == Byte.class)
			return "byte";
		if (cls == Short.class)
			return "short";
		if (cls == Integer.class)
			return "int";
		if (cls == Long.class)
			return "long";
		if (cls == Float.class)
			return "float";
		if (cls == Double.class)
			return "double";
		if (cls == Binary.class)
			return "binary";
		if (cls == String.class)
			return "string";
		if (cls.isPrimitive())
			return cls.getName();
		if (Serializable.class.isAssignableFrom(cls))
			return cls.getName();

		throw new UnsupportedOperationException("Unsupported type: " + cls.getName());
	}

	public static String GetStableName(Class<?> cls, Class<?> tplCls) {
		return cls.getName() + '<' + GetStableName(tplCls) + '>';
	}

	public static String GetStableName(Class<?> cls, Class<?> tplCls1, Class<?> tplCls2) {
		return cls.getName() + '<' + GetStableName(tplCls1) + ", " + GetStableName(tplCls2) + '>';
	}
}
