package Zeze.Collections;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.LongHashMap;
import Zeze.Util.Reflect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BeanFactory {
	private static final Logger logger = LogManager.getLogger(BeanFactory.class);
	private static final LongHashMap<Object> allClassNameMap = new LongHashMap<>();

	private final LongHashMap<MethodHandle> writingFactory = new LongHashMap<>();
	private volatile @Nullable LongHashMap<MethodHandle> readingFactory;

	/**
	 * 手动加载所有类路径以classPrefix前缀的全类名
	 *
	 * @param initClasses 是否初始化类
	 * @return 本次加载数量
	 */
	public static int loadAllClasses(@NotNull String classPrefix, boolean initClasses) {
		var timeBegin = System.nanoTime();
		int n = 0;
		synchronized (allClassNameMap) {
			for (var cn : Reflect.collectAllClassNames(null)) {
				if (cn.startsWith(classPrefix) && (initClasses ? loadClass(cn) : loadClassName(cn)))
					n++;
			}
		}
		logger.info("loaded {} {} for prefix '{}' ({} ms)",
				n, initClasses ? "classes" : "class names", classPrefix, (System.nanoTime() - timeBegin) / 1_000_000);
		return n;
	}

	public static int loadClassesFromPath(@NotNull String path, boolean initClasses) throws IOException {
		var timeBegin = System.nanoTime();
		int n = 0;
		synchronized (allClassNameMap) {
			for (var cn : Reflect.collectClassNamesFromPath(path)) {
				if (initClasses ? loadClass(cn) : loadClassName(cn))
					n++;
			}
		}
		logger.info("loaded {} {} from path '{}' ({} ms)",
				n, initClasses ? "classes" : "class names", path, (System.nanoTime() - timeBegin) / 1_000_000);
		return n;
	}

	public static int loadClassesFromJar(@NotNull String jarFile, boolean initClasses) throws IOException {
		var timeBegin = System.nanoTime();
		int n = 0;
		synchronized (allClassNameMap) {
			for (var cn : Reflect.collectClassNamesFromJar(jarFile)) {
				if (initClasses ? loadClass(cn) : loadClassName(cn))
					n++;
			}
		}
		logger.info("loaded {} {} from jar '{}' ({} ms)",
				n, initClasses ? "classes" : "class names", jarFile, (System.nanoTime() - timeBegin) / 1_000_000);
		return n;
	}

	@SuppressWarnings("deprecation")
	public static boolean loadClass(String className) {
		try {
			var cls = Class.forName(className);
			if (Bean.class.isAssignableFrom(cls)) {
				var typeId = ((Bean)cls.newInstance()).typeId();
				var oldObj = allClassNameMap.put(typeId, cls);
				if (oldObj != null && (oldObj instanceof String ? !oldObj.equals(className) : oldObj != cls)) {
					throw new AssertionError("duplicate typeId=" + typeId
							+ " for '" + oldObj + "' and '" + className + '\'');
				}
				return !(oldObj instanceof Class);
			}
		} catch (Exception | LinkageError e) {
			logger.warn("load class failed: '{}'", className, e);
		}
		return false;
	}

	public static boolean loadClassName(String className) {
		var typeId = Bean.hash64(className);
		var oldObj = allClassNameMap.put(typeId, className);
		if (oldObj != null) {
			var oldCn = oldObj instanceof Class ? ((Class<?>)oldObj).getName() : (String)oldObj;
			if (!oldCn.equals(className)) {
				throw new AssertionError("duplicate typeId=" + typeId
						+ " for '" + oldCn + "' and '" + className + '\'');
			}
			if (oldObj instanceof Class) {
				allClassNameMap.put(typeId, oldObj);
				return false;
			}
		}
		return true;
	}

	/**
	 * 获取指定typeId的Bean类,如果之前未用loadClasses加载过,会自动加载所有类路径中的所有类(不初始化类)
	 */
	@SuppressWarnings("unchecked")
	public static @Nullable Class<? extends Bean> findClass(long typeId) {
		synchronized (allClassNameMap) {
			var obj = allClassNameMap.get(typeId);
			if (obj instanceof Class)
				return (Class<? extends Bean>)obj;
			if (obj instanceof String) {
				try {
					var cls = Class.forName((String)obj);
					if (Bean.class.isAssignableFrom(cls))
						allClassNameMap.put(typeId, cls);
					else {
						allClassNameMap.remove(typeId);
						cls = null;
					}
					return (Class<? extends Bean>)cls;
				} catch (ClassNotFoundException | NoClassDefFoundError e) {
					throw new RuntimeException("load class failed: '" + obj + '\'', e);
				}
			}
			if (allClassNameMap.isEmpty() && loadAllClasses("", false) != 0)
				return findClass(typeId);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Bean> T invoke(@NotNull MethodHandle methodHandle) {
		try {
			return (T)methodHandle.invoke();
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
			throw new RuntimeException(e);
		}
	}

	public @NotNull MethodHandle register(@NotNull Class<? extends Bean> beanClass) {
		MethodHandle beanCtor = Reflect.getDefaultConstructor(beanClass);
		Bean bean;
		try {
			bean = (Bean)beanCtor.invoke();
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
			throw new RuntimeException(e);
		}
		synchronized (writingFactory) {
			if (null == writingFactory.putIfAbsent(bean.typeId(), beanCtor))
				readingFactory = null;
		}
		return beanCtor;
	}

	public @NotNull MethodHandle register(@NotNull Bean bean) {
		MethodHandle beanCtor = Reflect.getDefaultConstructor(bean.getClass());
		synchronized (writingFactory) {
			if (null == writingFactory.putIfAbsent(bean.typeId(), beanCtor))
				readingFactory = null;
		}
		return beanCtor;
	}

	private void register(long typeId, @NotNull MethodHandle beanCtor) {
		synchronized (writingFactory) {
			if (writingFactory.putIfAbsent(typeId, beanCtor) == null)
				readingFactory = null;
		}
	}

	public static long getSpecialTypeIdFromBean(@NotNull Bean bean) {
		return bean.typeId();
	}

	public @NotNull Bean createBeanFromSpecialTypeId(long typeId) {
		try {
			var factory = readingFactory;
			if (factory == null) {
				synchronized (writingFactory) {
					factory = readingFactory;
					if (factory == null)
						readingFactory = factory = writingFactory.clone();
				}
			}
			var beanCtor = factory.get(typeId);
			if (beanCtor != null)
				return (Bean)beanCtor.invoke();
			var cls = findClass(typeId);
			if (cls == null)
				throw new UnsupportedOperationException("unknown bean typeId=" + typeId);
			beanCtor = Reflect.getDefaultConstructor(cls);
			var bean = (Bean)beanCtor.invoke();
			var beanTypeId = bean.typeId();
			if (beanTypeId != typeId)
				throw new UnsupportedOperationException("unmatched bean typeId: " + beanTypeId + " != " + typeId);
			register(beanTypeId, beanCtor);
			return bean;
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
			throw new RuntimeException(e);
		}
	}

	public static @NotNull Binary toBinary(@NotNull Bean bean) {
		var preAllocSize = bean.preAllocSize();
		var bb = ByteBuffer.Allocate(Math.min(8 + preAllocSize, 65536));
		bb.WriteLong8(bean.typeId());
		bean.encode(bb);
		int size = bb.WriteIndex - 8;
		if (size > preAllocSize)
			bean.preAllocSize(size);
		return new Binary(bb);
	}

	public @NotNull Bean toBean(@NotNull Binary data) {
		var bb = data.Wrap();
		var typeId = bb.ReadLong8();
		var bean = createBeanFromSpecialTypeId(typeId);
		bean.decode(bb);
		return bean;
	}
}
