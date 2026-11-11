package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import java.util.function.Supplier;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/** Constructs collection values and their edits using the same declared type. */
record BeanValueFactory<V extends Bean>(Supplier<V> create, @Nullable DynamicField field) {
	private static final ClassValue<BeanValueFactory<?>> factories = new ClassValue<>() {
		@Override
		protected BeanValueFactory<?> computeValue(Class<?> type) {
			return of(Reflect.getDefaultConstructor(type));
		}
	};

	@SuppressWarnings("unchecked")
	static <V extends Bean> BeanValueFactory<V> of(Class<V> type) {
		return (BeanValueFactory<V>)factories.get(type);
	}

	@SuppressWarnings("unchecked")
	static <V extends Bean> BeanValueFactory<V> of(MethodHandle constructor) {
		return new BeanValueFactory<>(() -> {
			try {
				return (V)constructor.invoke();
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
		}, null);
	}

	void checkValue(V value) {
		if (field != null)
			DynamicField.checkMatch(field, ((DynamicBean)value).field());
	}

	void checkLog(Log log) {
		DynamicField.checkMatch(field, ((LogBean)log).dynamicField());
	}

	V read(IByteBuffer bb) {
		var value = create.get();
		value.decode(bb);
		return value;
	}

	LogBean readEdit(IByteBuffer bb) {
		var log = field != null ? new LogDynamic(field) : new LogBean();
		log.decode(bb);
		return log;
	}
}
