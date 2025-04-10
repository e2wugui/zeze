package Zeze.Collections;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.FastLock;
import Zeze.Util.LongHashMap;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BeanFactory {
	private static final @NotNull Logger logger = LogManager.getLogger(BeanFactory.class);
	private static final LongHashMap<Object> allClassNameMap = new LongHashMap<>();
	private static final LongHashMap<Object> allDataClassNameMap = new LongHashMap<>();
	private static final @NotNull ReentrantReadWriteLock.ReadLock allClassNameMapReadLock;
	private static final @NotNull ReentrantReadWriteLock.WriteLock allClassNameMapWriteLock;
	private static final @NotNull ReentrantReadWriteLock.ReadLock allDataNameMapReadLock;
	private static final @NotNull ReentrantReadWriteLock.WriteLock allDataNameMapWriteLock;
	private static @Nullable Application zeze;

	static {
		var rwLock = new ReentrantReadWriteLock();
		allClassNameMapReadLock = rwLock.readLock();
		allClassNameMapWriteLock = rwLock.writeLock();
		rwLock = new ReentrantReadWriteLock();
		allDataNameMapReadLock = rwLock.readLock();
		allDataNameMapWriteLock = rwLock.writeLock();
	}

	private final LongHashMap<MethodHandle> writingBeanFactory = new LongHashMap<>();
	private final FastLock writingBeanFactoryLock = new FastLock();
	private volatile @Nullable LongHashMap<MethodHandle> readingBeanFactory;
	private final LongHashMap<MethodHandle> writingDataFactory = new LongHashMap<>();
	private final FastLock writingDataFactoryLock = new FastLock();
	private volatile @Nullable LongHashMap<MethodHandle> readingDataFactory;
	private final ConcurrentHashSet<Consumer<Class<?>>> globalToLocalWatchers = new ConcurrentHashSet<>();
	private final ConcurrentHashMap<Class<?>, MethodHandle> registeredClasses = new ConcurrentHashMap<>();

	public void registerWatch(@NotNull Consumer<Class<?>> consumer) {
		globalToLocalWatchers.add(consumer);
	}

	public void unregisterWatch(@NotNull Consumer<Class<?>> consumer) {
		globalToLocalWatchers.remove(consumer);
	}

	private void notifyAllWatcher(@NotNull Class<?> cls) {
		for (var consumer : globalToLocalWatchers)
			consumer.accept(cls);
	}

	public static void setApplication(@NotNull Application zeze) {
		BeanFactory.zeze = zeze;
	}

	public static long typeId(@NotNull Class<? extends Bean> beanClass) {
		try {
			return beanClass.getConstructor((Class<?>[])null).newInstance((Object[])null).typeId();
		} catch (Exception ex) {
			throw Task.forceThrow(ex);
		}
	}

	/**
	 * Hot热更的时候调用这个函数更新Bean信息。
	 * 【Bean 只会修改或增加，不会删除，内部实现时采取put方式即可。】
	 *
	 * @param beanFactories 相关的 BeanFactories
	 * @param hotModules    相关的 HotModule's JarFile
	 */
	public static void resetHot(@NotNull Map<BeanFactory, List<Class<?>>> beanFactories,
								@NotNull List<JarFile> hotModules) {
		for (JarFile jf : hotModules)
			reloadClassesFromJar(jf);

		assert zeze != null;
		var hotRedirect = zeze.getHotManager().getHotRedirect();
		for (var e : beanFactories.entrySet()) {
			var bf = e.getKey();
			var classes = e.getValue();
			bf.writingBeanFactoryLock.lock();
			try {
				bf.writingBeanFactory.foreachUpdate((typeId, oldCtor) -> {
					try {
						var oldCls = oldCtor.type().returnType();
						var newCls = hotRedirect.loadClass(oldCls.getName());
						classes.add(newCls);
						var ctor = Reflect.getDefaultConstructor(newCls);
						if (oldCls != newCls) {
							bf.registeredClasses.put(newCls, ctor);
							bf.registeredClasses.remove(oldCls);
						}
						return ctor;
					} catch (Throwable ex) { // MethodHandle.invoke
						throw Task.forceThrow(ex);
					}
				});
				bf.readingBeanFactory = null;
			} finally {
				bf.writingBeanFactoryLock.unlock();
			}
			bf.writingDataFactoryLock.lock();
			try {
				bf.writingDataFactory.foreachUpdate((typeId, oldCtor) -> {
					try {
						var oldCls = oldCtor.type().returnType();
						var newCls = hotRedirect.loadClass(oldCls.getName());
						classes.add(newCls);
						var ctor = Reflect.getDefaultConstructor(newCls);
						if (oldCls != newCls) {
							bf.registeredClasses.put(newCls, ctor);
							bf.registeredClasses.remove(oldCls);
						}
						return ctor;
					} catch (Throwable ex) { // MethodHandle.invoke
						throw Task.forceThrow(ex);
					}
				});
				bf.readingDataFactory = null;
			} finally {
				bf.writingDataFactoryLock.unlock();
			}
		}
	}

	/**
	 * 手动加载所有类路径以classPrefix前缀的全类名
	 *
	 * @param initClasses 是否初始化类
	 * @return 本次加载数量
	 */
	public static int loadAllClasses(@NotNull String classPrefix, boolean initClasses) {
		var timeBegin = System.nanoTime();
		int n = 0;
		allClassNameMapWriteLock.lock();
		try {
			allDataNameMapWriteLock.lock();
			try {
				for (var cn : Reflect.collectAllClassNames(null)) {
					if (cn.startsWith(classPrefix) && (initClasses ? loadClass(cn) : loadClassName(cn)))
						n++;
				}
			} finally {
				allDataNameMapWriteLock.unlock();
			}
		} finally {
			allClassNameMapWriteLock.unlock();
		}
		logger.info("loaded {} {} for prefix '{}' ({} ms)",
				n, initClasses ? "classes" : "class names", classPrefix, (System.nanoTime() - timeBegin) / 1_000_000);
		return n;
	}

	public static int loadClassesFromPath(@NotNull String path, boolean initClasses) {
		var timeBegin = System.nanoTime();
		int n = 0;
		allClassNameMapWriteLock.lock();
		try {
			allDataNameMapWriteLock.lock();
			try {
				for (var cn : Reflect.collectClassNamesFromPath(path)) {
					if (initClasses ? loadClass(cn) : loadClassName(cn))
						n++;
				}
			} finally {
				allDataNameMapWriteLock.unlock();
			}
		} finally {
			allClassNameMapWriteLock.unlock();
		}
		logger.info("loaded {} {} from path '{}' ({} ms)",
				n, initClasses ? "classes" : "class names", path, (System.nanoTime() - timeBegin) / 1_000_000);
		return n;
	}

	public static int loadClassesFromJar(@NotNull String jarFile, boolean initClasses) throws IOException {
		var timeBegin = System.nanoTime();
		int n = 0;
		allClassNameMapWriteLock.lock();
		try {
			allDataNameMapWriteLock.lock();
			try {
				for (var cn : Reflect.collectClassNamesFromJar(jarFile)) {
					if (initClasses ? loadClass(cn) : loadClassName(cn))
						n++;
				}
			} finally {
				allDataNameMapWriteLock.unlock();
			}
		} finally {
			allClassNameMapWriteLock.unlock();
		}
		logger.info("loaded {} {} from jar '{}' ({} ms)",
				n, initClasses ? "classes" : "class names", jarFile, (System.nanoTime() - timeBegin) / 1_000_000);
		return n;
	}

	public static int reloadClassesFromJar(@NotNull JarFile jarFile) {
		var timeBegin = System.nanoTime();
		int n = 0;
		allClassNameMapWriteLock.lock();
		try {
			allDataNameMapWriteLock.lock();
			try {
				for (var cn : Reflect.collectClassNamesFromJar(jarFile)) {
					reloadClassName(cn);
					n++;
				}
			} finally {
				allDataNameMapWriteLock.unlock();
			}
		} finally {
			allClassNameMapWriteLock.unlock();
		}
		logger.info("reloaded {} class names from jar '{}' ({} ms)",
				n, jarFile, (System.nanoTime() - timeBegin) / 1_000_000);
		return n;
	}

	@SuppressWarnings("deprecation")
	private static boolean loadClass(@NotNull String className) {
		try {
			long typeId;
			Object oldObj;
			var cls = zeze == null || zeze.getHotManager() == null
					? Class.forName(className)
					: Class.forName(className, true, zeze.getHotManager().getHotRedirect());
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

	private static boolean loadClassName(@NotNull String className) {
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

	private static void reloadClassName(@NotNull String className) {
		long typeId;
		LongHashMap<Object> classNameMap;
		if (className.endsWith("$Data")) {
			typeId = Bean.hash64(className.substring(0, className.length() - 5));
			classNameMap = allDataClassNameMap;
		} else {
			typeId = Bean.hash64(className);
			classNameMap = allClassNameMap;
		}
		classNameMap.put(typeId, className);
	}

	/**
	 * 获取指定typeId的Bean类,如果之前未用loadClasses加载过,会自动加载所有类路径中的所有类(不初始化类)
	 */
	@SuppressWarnings("unchecked")
	public static @Nullable Class<? extends Bean> findClass(long typeId) {
		allClassNameMapReadLock.lock();
		try {
			var obj = allClassNameMap.get(typeId);
			if (obj instanceof Class)
				return (Class<? extends Bean>)obj;
			if (obj == null && !allClassNameMap.isEmpty())
				return null;
		} finally {
			allClassNameMapReadLock.unlock();
		}

		allClassNameMapWriteLock.lock();
		try {
			var obj = allClassNameMap.get(typeId);
			if (obj instanceof Class)
				return (Class<? extends Bean>)obj;
			if (obj instanceof String) {
				try {
					var cls = zeze == null || zeze.getHotManager() == null
							? Class.forName((String)obj)
							: Class.forName((String)obj, true, zeze.getHotManager().getHotRedirect());
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
		} finally {
			allClassNameMapWriteLock.unlock();
		}
		return null;
	}

	/**
	 * 获取指定typeId的Data类,如果之前未用loadClasses加载过,会自动加载所有类路径中的所有类(不初始化类)
	 */
	@SuppressWarnings("unchecked")
	public static @Nullable Class<? extends Data> findDataClass(long typeId) {
		allDataNameMapReadLock.lock();
		try {
			var obj = allDataClassNameMap.get(typeId);
			if (obj instanceof Class)
				return (Class<? extends Data>)obj;
			if (obj == null && !allDataClassNameMap.isEmpty())
				return null;
		} finally {
			allDataNameMapReadLock.unlock();
		}

		allDataNameMapWriteLock.lock();
		try {
			var obj = allDataClassNameMap.get(typeId);
			if (obj instanceof Class)
				return (Class<? extends Data>)obj;
			if (obj instanceof String) {
				try {
					var cls = zeze == null || zeze.getHotManager() == null
							? Class.forName((String)obj)
							: Class.forName((String)obj, true, zeze.getHotManager().getHotRedirect());
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
		} finally {
			allDataNameMapWriteLock.unlock();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T invoke(@NotNull MethodHandle methodHandle) {
		try {
			return (T)methodHandle.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
	}

	public @NotNull MethodHandle register(@NotNull Class<? extends Serializable> cls) {
		var ctor = registeredClasses.get(cls);
		if (ctor != null)
			return ctor;
		ctor = Reflect.getDefaultConstructor(cls);
		Serializable s;
		try {
			s = (Serializable)ctor.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
		return register(cls, s, ctor);
	}

	public @NotNull MethodHandle register(@NotNull Serializable s) {
		var cls = s.getClass();
		var ctor = registeredClasses.get(cls);
		if (ctor != null)
			return ctor;
		ctor = Reflect.getDefaultConstructor(cls);
		return register(cls, s, ctor);
	}

	private @NotNull MethodHandle register(@NotNull Class<? extends Serializable> cls, @NotNull Serializable s,
										   @NotNull MethodHandle ctor) {
		if (s instanceof Bean) {
			writingBeanFactoryLock.lock();
			try {
				var oldCtor = writingBeanFactory.put(s.typeId(), ctor);
				if (oldCtor != ctor) {
					if (oldCtor != null && oldCtor.type().returnType() != cls) {
						logger.warn("overwrite bean factory for typeId={},class={}=>{}",
								s.typeId(), oldCtor.type().returnType().getName(), cls.getName());
					}
					readingBeanFactory = null;
					registeredClasses.put(cls, ctor);
				}
			} finally {
				writingBeanFactoryLock.unlock();
			}
		} else if (s instanceof Data) {
			writingDataFactoryLock.lock();
			try {
				var oldCtor = writingDataFactory.put(s.typeId(), ctor);
				if (oldCtor != ctor) {
					if (oldCtor != null && oldCtor.type().returnType() != cls) {
						logger.warn("overwrite data factory for typeId={},class={}=>{}",
								s.typeId(), oldCtor.type().returnType().getName(), cls.getName());
					}
					readingDataFactory = null;
					registeredClasses.put(cls, ctor);
				}
			} finally {
				writingDataFactoryLock.unlock();
			}
		} else
			throw new IllegalArgumentException("not Bean or Data: " + cls.getName());
		return ctor;
	}

	private void registerBean(@NotNull Class<? extends Bean> cls, long typeId, @NotNull MethodHandle ctor) {
		writingBeanFactoryLock.lock();
		try {
			var oldCtor = writingBeanFactory.put(typeId, ctor);
			if (oldCtor != ctor) {
				if (oldCtor != null && oldCtor.type().returnType() != cls) {
					logger.warn("overwrite bean factory for typeId={}, class={}=>{}",
							typeId, oldCtor.type().returnType().getName(), cls.getName());
				}
				readingBeanFactory = null;
				registeredClasses.put(cls, ctor);
			}
		} finally {
			writingBeanFactoryLock.unlock();
		}
	}

	private void registerData(@NotNull Class<? extends Data> cls, long typeId, @NotNull MethodHandle ctor) {
		writingDataFactoryLock.lock();
		try {
			var oldCtor = writingDataFactory.put(typeId, ctor);
			if (oldCtor != ctor) {
				if (oldCtor != null && oldCtor.type().returnType() != cls) {
					logger.warn("overwrite data factory for typeId={}, class={}=>{}",
							typeId, oldCtor.type().returnType().getName(), cls.getName());
				}
				readingDataFactory = null;
				registeredClasses.put(cls, ctor);
			}
		} finally {
			writingDataFactoryLock.unlock();
		}
	}

	public static long getSpecialTypeIdFromBean(@NotNull Serializable s) {
		return s.typeId();
	}

	public @NotNull Bean createBeanFromSpecialTypeId(long typeId) {
		try {
			var factory = readingBeanFactory;
			if (factory == null) {
				writingBeanFactoryLock.lock();
				try {
					factory = readingBeanFactory;
					if (factory == null)
						readingBeanFactory = factory = writingBeanFactory.clone();
				} finally {
					writingBeanFactoryLock.unlock();
				}
			}
			var ctor = factory.get(typeId);
			if (ctor != null)
				return (Bean)ctor.invoke();
			var cls = findClass(typeId);
			if (cls == null) {
				if (typeId != EmptyBean.TYPEID)
					throw new UnsupportedOperationException("unknown bean typeId=" + typeId);
				cls = EmptyBean.class;
			}
			ctor = Reflect.getDefaultConstructor(cls);
			var bean = (Bean)ctor.invoke();
			var beanTypeId = bean.typeId();
			if (beanTypeId != typeId)
				throw new UnsupportedOperationException("unmatched bean typeId: " + beanTypeId + " != " + typeId);
			notifyAllWatcher(cls);
			registerBean(cls, beanTypeId, ctor);
			return bean;
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
	}

	public @NotNull Data createDataFromSpecialTypeId(long typeId) {
		try {
			var factory = readingDataFactory;
			if (factory == null) {
				writingDataFactoryLock.lock();
				try {
					factory = readingDataFactory;
					if (factory == null)
						readingDataFactory = factory = writingDataFactory.clone();
				} finally {
					writingDataFactoryLock.unlock();
				}
			}
			var ctor = factory.get(typeId);
			if (ctor != null)
				return (Data)ctor.invoke();
			var cls = findDataClass(typeId);
			if (cls == null) {
				if (typeId != EmptyBean.Data.TYPEID)
					throw new UnsupportedOperationException("unknown data typeId=" + typeId);
				cls = EmptyBean.Data.class;
			}
			ctor = cls == EmptyBean.Data.class ? Reflect.lookup.findStaticGetter(cls, "instance", cls)
					: Reflect.getDefaultConstructor(cls);
			var data = (Data)ctor.invoke();
			var dataTypeId = data.typeId();
			if (dataTypeId != typeId)
				throw new UnsupportedOperationException("unmatched data typeId: " + dataTypeId + " != " + typeId);
			notifyAllWatcher(cls);
			registerData(cls, dataTypeId, ctor);
			return data;
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
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
