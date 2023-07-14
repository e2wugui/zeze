package Zeze.Collections;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.LongHashMap;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BeanFactory {
	private static final Logger logger = LogManager.getLogger(BeanFactory.class);
	private static final LongHashMap<Object> allClassNameMap = new LongHashMap<>();
	private static final LongHashMap<Object> allDataClassNameMap = new LongHashMap<>();

	private final LongHashMap<MethodHandle> writingBeanFactory = new LongHashMap<>();
	private volatile @Nullable LongHashMap<MethodHandle> readingBeanFactory;
	private final LongHashMap<MethodHandle> writingDataFactory = new LongHashMap<>();
	private volatile @Nullable LongHashMap<MethodHandle> readingDataFactory;

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
	private static boolean loadClass(String className) {
		try {
			long typeId;
			Object oldObj;
			var cls = Class.forName(className);
			if (Bean.class.isAssignableFrom(cls)) {
				typeId = ((Bean)cls.newInstance()).typeId();
				oldObj = allClassNameMap.put(typeId, cls);
			} else if (Data.class.isAssignableFrom(cls)) {
				typeId = ((Data)cls.newInstance()).typeId();
				oldObj = allDataClassNameMap.put(typeId, cls);
			} else
				return false;
			if (oldObj != null && (oldObj instanceof String ? !oldObj.equals(className) : oldObj != cls)) {
				throw new AssertionError("duplicate typeId=" + typeId
						+ " for '" + oldObj + "' and '" + className + '\'');
			}
			return !(oldObj instanceof Class);
		} catch (Exception | LinkageError e) {
			logger.warn("load class failed: '{}'", className, e);
		}
		return false;
	}

	private static boolean loadClassName(String className) {
		long typeId;
		LongHashMap<Object> classNameMap;
		if (className.endsWith("$Data")) {
			typeId = Bean.hash64(className.substring(0, className.length() - 5));
			classNameMap = allDataClassNameMap;
		} else {
			typeId = Bean.hash64(className);
			classNameMap = allClassNameMap;
		}
		var oldObj = classNameMap.put(typeId, className);
		if (oldObj != null) {
			var oldCn = oldObj instanceof Class ? ((Class<?>)oldObj).getName() : (String)oldObj;
			if (!oldCn.equals(className)) {
				throw new AssertionError("duplicate typeId=" + typeId
						+ " for '" + oldCn + "' and '" + className + '\'');
			}
			if (oldObj instanceof Class) {
				classNameMap.put(typeId, oldObj);
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
					throw new IllegalStateException("load class failed: '" + obj + '\'', e);
				}
			}
			if (allClassNameMap.isEmpty() && loadAllClasses("", false) != 0)
				return findClass(typeId);
		}
		return null;
	}

	/**
	 * 获取指定typeId的Data类,如果之前未用loadClasses加载过,会自动加载所有类路径中的所有类(不初始化类)
	 */
	@SuppressWarnings("unchecked")
	public static @Nullable Class<? extends Data> findDataClass(long typeId) {
		synchronized (allClassNameMap) {
			var obj = allDataClassNameMap.get(typeId);
			if (obj instanceof Class)
				return (Class<? extends Data>)obj;
			if (obj instanceof String) {
				try {
					var cls = Class.forName((String)obj);
					if (Data.class.isAssignableFrom(cls))
						allDataClassNameMap.put(typeId, cls);
					else {
						allDataClassNameMap.remove(typeId);
						cls = null;
					}
					return (Class<? extends Data>)cls;
				} catch (ClassNotFoundException | NoClassDefFoundError e) {
					throw new IllegalStateException("load class failed: '" + obj + '\'', e);
				}
			}
			if (allDataClassNameMap.isEmpty() && loadAllClasses("", false) != 0)
				return findDataClass(typeId);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T invoke(@NotNull MethodHandle methodHandle) {
		try {
			return (T)methodHandle.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			Task.forceThrow(e);
			throw new AssertionError(); // never run here
		}
	}

	public @NotNull MethodHandle register(@NotNull Class<? extends Serializable> cls) {
		MethodHandle ctor = Reflect.getDefaultConstructor(cls);
		Serializable s;
		try {
			s = (Serializable)ctor.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			Task.forceThrow(e);
			return null; // never run here
		}
		if (s instanceof Bean) {
			synchronized (writingBeanFactory) {
				if (writingBeanFactory.putIfAbsent(s.typeId(), ctor) == null)
					readingBeanFactory = null;
			}
		} else if (s instanceof Data) {
			synchronized (writingDataFactory) {
				if (writingDataFactory.putIfAbsent(s.typeId(), ctor) == null)
					readingDataFactory = null;
			}
		} else
			throw new IllegalArgumentException("not Bean or Data: " + s.getClass().getName());
		return ctor;
	}

	public @NotNull MethodHandle register(@NotNull Serializable s) {
		var ctor = Reflect.getDefaultConstructor(s.getClass());
		if (s instanceof Bean) {
			synchronized (writingBeanFactory) {
				if (writingBeanFactory.putIfAbsent(s.typeId(), ctor) == null)
					readingBeanFactory = null;
			}
		} else if (s instanceof Data) {
			synchronized (writingDataFactory) {
				if (writingDataFactory.putIfAbsent(s.typeId(), ctor) == null)
					readingDataFactory = null;
			}
		} else
			throw new IllegalArgumentException("not Bean or Data: " + s.getClass().getName());
		return ctor;
	}

	private void register(long typeId, @NotNull MethodHandle beanCtor) {
		synchronized (writingBeanFactory) {
			if (writingBeanFactory.putIfAbsent(typeId, beanCtor) == null)
				readingBeanFactory = null;
		}
	}

	private void registerData(long typeId, @NotNull MethodHandle dataCtor) {
		synchronized (writingDataFactory) {
			if (writingDataFactory.putIfAbsent(typeId, dataCtor) == null)
				readingDataFactory = null;
		}
	}

	public static long getSpecialTypeIdFromBean(@NotNull Serializable s) {
		return s.typeId();
	}

	public @NotNull Bean createBeanFromSpecialTypeId(long typeId) {
		try {
			var factory = readingBeanFactory;
			if (factory == null) {
				synchronized (writingBeanFactory) {
					factory = readingBeanFactory;
					if (factory == null)
						readingBeanFactory = factory = writingBeanFactory.clone();
				}
			}
			var beanCtor = factory.get(typeId);
			if (beanCtor != null)
				return (Bean)beanCtor.invoke();
			var cls = findClass(typeId);
			if (cls == null) {
				if (typeId != EmptyBean.TYPEID)
					throw new UnsupportedOperationException("unknown bean typeId=" + typeId);
				cls = EmptyBean.class;
			}
			beanCtor = Reflect.getDefaultConstructor(cls);
			var bean = (Bean)beanCtor.invoke();
			var beanTypeId = bean.typeId();
			if (beanTypeId != typeId)
				throw new UnsupportedOperationException("unmatched bean typeId: " + beanTypeId + " != " + typeId);
			register(beanTypeId, beanCtor);
			return bean;
		} catch (Throwable e) { // MethodHandle.invoke
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	public @NotNull Data createDataFromSpecialTypeId(long typeId) {
		try {
			var factory = readingDataFactory;
			if (factory == null) {
				synchronized (writingDataFactory) {
					factory = readingDataFactory;
					if (factory == null)
						readingDataFactory = factory = writingDataFactory.clone();
				}
			}
			var dataCtor = factory.get(typeId);
			if (dataCtor != null)
				return (Data)dataCtor.invoke();
			var cls = findDataClass(typeId);
			if (cls == null) {
				if (typeId != EmptyBean.Data.TYPEID)
					throw new UnsupportedOperationException("unknown data typeId=" + typeId);
				cls = EmptyBean.Data.class;
			}
			dataCtor = cls == EmptyBean.Data.class ? MethodHandles.lookup().findStaticGetter(cls, "instance", cls)
					: Reflect.getDefaultConstructor(cls);
			var data = (Data)dataCtor.invoke();
			var dataTypeId = data.typeId();
			if (dataTypeId != typeId)
				throw new UnsupportedOperationException("unmatched data typeId: " + dataTypeId + " != " + typeId);
			registerData(dataTypeId, dataCtor);
			return data;
		} catch (Throwable e) { // MethodHandle.invoke
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	public static @NotNull ByteBuffer toByteBuffer(@NotNull Serializable s, @Nullable ByteBuffer bb) {
		int preAllocSize = s.preAllocSize();
		if (bb == null)
			bb = ByteBuffer.Allocate(Math.min(8 + preAllocSize, 65536));
		else
			bb.EnsureWrite(Math.min(8 + preAllocSize, 65536));
		bb.WriteLong8(s.typeId());
		int oldIndex = bb.WriteIndex;
		s.encode(bb);
		int size = bb.WriteIndex - oldIndex;
		if (size > preAllocSize)
			s.preAllocSize(size);
		return bb;
	}

	public static @NotNull Binary toBinary(@NotNull Serializable s) {
		int preAllocSize = s.preAllocSize();
		var bb = ByteBuffer.Allocate(Math.min(8 + preAllocSize, 65536));
		bb.WriteLong8(s.typeId());
		s.encode(bb);
		int size = bb.WriteIndex - 8;
		if (size > preAllocSize)
			s.preAllocSize(size);
		return new Binary(bb);
	}

	public @NotNull Bean toBean(@NotNull ByteBuffer bb) {
		var typeId = bb.ReadLong8();
		var bean = createBeanFromSpecialTypeId(typeId);
		bean.decode(bb);
		return bean;
	}

	public @NotNull Bean toBean(@NotNull Binary b) {
		return toBean(b.Wrap());
	}

	public @NotNull Data toData(@NotNull ByteBuffer bb) {
		var typeId = bb.ReadLong8();
		var data = createDataFromSpecialTypeId(typeId);
		data.decode(bb);
		return data;
	}

	public @NotNull Data toData(@NotNull Binary b) {
		return toData(b.Wrap());
	}
}
