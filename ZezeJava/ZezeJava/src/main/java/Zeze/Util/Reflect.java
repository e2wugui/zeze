package Zeze.Util;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Serializable;
import Zeze.Serialize.Vector2;
import Zeze.Serialize.Vector2Int;
import Zeze.Serialize.Vector3;
import Zeze.Serialize.Vector3Int;
import Zeze.Serialize.Vector4;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Reflect {
	public static final boolean inDebugMode = !"true".equalsIgnoreCase(System.getProperty("noDebugMode")) &&
			ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
	public static final @NotNull StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
	public static final @NotNull MethodHandle supplierMH;
	private static final HashMap<Class<?>, String> stableNameMap = new HashMap<>(32);
	private static final HashMap<Class<?>, Class<?>> boxClassMap = new HashMap<>(16);

	static {
		try {
			supplierMH = MethodHandles.lookup().findVirtual(Supplier.class, "get", MethodType.methodType(Object.class));
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}

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

		stableNameMap.put(BigDecimal.class, "decimal");

		boxClassMap.put(void.class, Void.class);
		boxClassMap.put(boolean.class, Boolean.class);
		boxClassMap.put(char.class, Character.class);
		boxClassMap.put(byte.class, Byte.class);
		boxClassMap.put(short.class, Short.class);
		boxClassMap.put(int.class, Integer.class);
		boxClassMap.put(long.class, Long.class);
		boxClassMap.put(float.class, Float.class);
		boxClassMap.put(double.class, Double.class);
	}

	private final HashMap<String, Method> methods = new HashMap<>();

	public Reflect(@NotNull Class<?> cls) {
		for (var c = cls; c != null; c = c.getSuperclass()) {
			for (var method : c.getDeclaredMethods()) {
				// 只有协议处理函数能配置TransactionLevel
				// 其实这里还不是足够严谨, 手动写了同名方法,都只有1个Protocol子类类型的参数,就可能不生效,除非都加上一样的注解
				if (method.getName().startsWith("Process") && method.getParameterCount() == 1
						&& Protocol.class.isAssignableFrom(method.getParameters()[0].getType()))
					methods.putIfAbsent(method.getName(), method);
			}
		}
	}

	public static @NotNull MethodHandle getDefaultConstructor(@NotNull Class<?> cls) {
		try {
			return MethodHandles.lookup().findConstructor(cls, MethodType.methodType(void.class));
		} catch (ReflectiveOperationException e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	public TransactionLevel getTransactionLevel(@NotNull String methodName, TransactionLevel def) {
		var method = methods.get(methodName);
		if (null == method)
			return def;

		var annotation = method.getAnnotation(TransactionLevelAnnotation.class);
		return annotation != null ? annotation.Level() : def;
	}

	public DispatchMode getDispatchMode(@NotNull String methodName, DispatchMode def) {
		var method = methods.get(methodName);
		if (null == method)
			return def;

		var annotation = method.getAnnotation(DispatchModeAnnotation.class);
		return annotation != null ? annotation.mode() : def;
	}

	public static @NotNull String getStableName(@NotNull Class<?> cls) {
		// 支持的 Zeze/Gen/Types/ 类型。
		var name = stableNameMap.get(cls);
		if (name != null)
			return name;
		if (Serializable.class.isAssignableFrom(cls))
			return cls.getName();
		throw new UnsupportedOperationException("Unsupported type: " + cls.getName());
	}

	public static @NotNull String getStableName(@NotNull Class<?> cls, @NotNull Class<?> tplCls) {
		return cls.getName() + '<' + getStableName(tplCls) + '>';
	}

	public static @NotNull String getStableName(@NotNull Class<?> cls, @NotNull Class<?> tplCls1,
												@NotNull Class<?> tplCls2) {
		return cls.getName() + '<' + getStableName(tplCls1) + ", " + getStableName(tplCls2) + '>';
	}

	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj) {
		return (T)obj;
	}

	private static void collectClassFromPath(@NotNull String parent, @NotNull File path,
											 @NotNull ArrayList<String> classNames) {
		var files = path.listFiles();
		if (files != null) {
			for (var file : files) {
				var fn = file.getName();
				if (file.isDirectory()) {
					if (!fn.equals("META-INF"))
						collectClassFromPath(parent + fn + '.', file, classNames);
				} else if (fn.endsWith(".class") && !fn.equals("module-info.class"))
					classNames.add(parent + fn.substring(0, fn.length() - 6));
			}
		}
	}

	public static @NotNull ArrayList<String> collectClassNamesFromPath(@NotNull String path) {
		var classNames = new ArrayList<String>();
		collectClassFromPath("", new File(path), classNames);
		return classNames;
	}

	public static @NotNull ArrayList<String> collectClassNamesFromJar(@NotNull String jarFile) throws IOException {
		try (var jf = new JarFile(jarFile)) {
			return collectClassNamesFromJar(jf);
		}
	}

	public static @NotNull ArrayList<String> collectClassNamesFromJar(@NotNull JarFile jarFile) {
		var classNames = new ArrayList<String>();
		for (var e = jarFile.entries(); e.hasMoreElements(); ) {
			var fn = e.nextElement().getName();
			if (fn.endsWith(".class") && !fn.startsWith("META-INF/") && !fn.endsWith("module-info.class"))
				classNames.add(fn.substring(0, fn.length() - 6).replace('/', '.'));
		}
		return classNames;
	}

	public static @NotNull ArrayList<String> collectAllClassNames(@Nullable Predicate<String> cpFilter) {
		var isWin = System.getProperty("os.name").toLowerCase().startsWith("win");
		var classNames = new ArrayList<String>();
		for (var cp : System.getProperty("java.class.path").split(isWin ? ";" : ":")) {
			if (cpFilter == null || cpFilter.test(cp)) {
				var file = new File(cp);
				if (file.isDirectory())
					collectClassFromPath("", file, classNames);
				else {
					try (var jf = new JarFile(file)) {
						for (var e = jf.entries(); e.hasMoreElements(); ) {
							var fn = e.nextElement().getName();
							if (fn.endsWith(".class") && !fn.startsWith("META-INF/") && !fn.endsWith("module-info.class"))
								classNames.add(fn.substring(0, fn.length() - 6).replace('/', '.'));
						}
					} catch (IOException ignored) {
					}
				}
			}
		}
		return classNames;
	}

	public static @NotNull String @NotNull [] collectClassPaths(@NotNull ClassLoader classLoader) {
		var isWin = System.getProperty("os.name").toLowerCase().startsWith("win");
		return System.getProperty("java.class.path").split(isWin ? ";" : ":");
	}

	public static @Nullable Class<?> getBoxClass(@NotNull Class<?> primitiveClass) {
		return boxClassMap.get(primitiveClass);
	}
}
