package Zeze.Collections;

import java.lang.invoke.MethodHandle;
import Zeze.Transaction.Bean;
import Zeze.Util.LongHashMap;
import Zeze.Util.Reflect;

public final class BeanFactory {
	private final LongHashMap<MethodHandle> writingFactory = new LongHashMap<>();
	private volatile LongHashMap<MethodHandle> readingFactory;

	@SuppressWarnings("unchecked")
	public static <T extends Bean> T invoke(MethodHandle methodHandle) {
		try {
			return (T)methodHandle.invoke();
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
			throw new RuntimeException(e);
		}
	}

	public MethodHandle register(Class<? extends Bean> beanClass) {
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

	public MethodHandle register(Bean bean) {
		MethodHandle beanCtor = Reflect.getDefaultConstructor(bean.getClass());
		synchronized (writingFactory) {
			if (null == writingFactory.putIfAbsent(bean.typeId(), beanCtor))
				readingFactory = null;
		}
		return beanCtor;
	}

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return bean.typeId();
	}

	public Bean createBeanFromSpecialTypeId(long typeId) {
		try {
			LongHashMap<MethodHandle> factory = readingFactory;
			if (factory == null) {
				synchronized (writingFactory) {
					factory = readingFactory;
					if (factory == null)
						readingFactory = factory = writingFactory.clone();
				}
			}
			MethodHandle beanCtor = factory.get(typeId);
			if (beanCtor == null)
				throw new UnsupportedOperationException("Unknown Bean TypeId=" + typeId);
			return (Bean)beanCtor.invoke();
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
			throw new RuntimeException(e);
		}
	}
}
