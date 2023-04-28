package Zeze.Collections;

import java.lang.invoke.MethodHandle;
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

	public static Class<?> findClass(long typeId) {
		synchronized (allClassNameMap) {
			var obj = allClassNameMap.get(typeId);
			if (obj instanceof Class)
				return (Class<?>)obj;
			if (obj instanceof String) {
				try {
					var cls = Class.forName((String)obj);
					allClassNameMap.put(typeId, cls);
					return cls;
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
			if (allClassNameMap.isEmpty()) {
				var timeBegin = System.nanoTime();
				for (var cn : Reflect.collectAllClassName(null))
					allClassNameMap.put(Bean.hash64(cn), cn);
				logger.info("collected all {} classes ({} ms)",
						allClassNameMap.size(), (System.nanoTime() - timeBegin) / 1_000_000);
				if (!allClassNameMap.isEmpty())
					return findClass(typeId);
			}
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

	public static long getSpecialTypeIdFromBean(@NotNull Bean bean) {
		return bean.typeId();
	}

	public @NotNull Bean createBeanFromSpecialTypeId(long typeId) {
		try {
			LongHashMap<MethodHandle> factory = readingFactory;
			if (factory == null) {
				synchronized (writingFactory) {
					factory = readingFactory;
					if (factory == null)
						readingFactory = factory = writingFactory.clone();
				}
			}
			var beanCtor = factory.get(typeId);
			if (beanCtor == null) {
				var cls = findClass(typeId);
				if (cls == null || !Bean.class.isAssignableFrom(cls))
					throw new UnsupportedOperationException("Unknown Bean TypeId=" + typeId);
				@SuppressWarnings({"unchecked", "unused"})
				var __ = beanCtor = register((Class<? extends Bean>)cls);
			}
			return (Bean)beanCtor.invoke();
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
			throw new RuntimeException(e);
		}
	}
}
