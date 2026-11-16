package Zeze.Serialize;

import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import Zeze.Net.Binary;
import Zeze.Transaction.Bean;
import Zeze.Transaction.BeanKey;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.Reflect;
import Zeze.Util.Task;

/** 标量类型的编解码工厂：按 Class 查表或反射创建 encoder/decoder/CodecFuncs。 */
public final class SerializeHelper {
	public interface ObjectIntFunction<T, R> {
		R apply(T t, int i);
	}

	@SuppressWarnings("ClassCanBeRecord")
	public static final class CodecFuncs<T> {
		public final int encodeType;
		public final BiConsumer<ByteBuffer, T> encoder;
		public final Function<IByteBuffer, T> decoder;
		public final ObjectIntFunction<IByteBuffer, T> decoderWithType;

		public CodecFuncs(int encodeType,
						  BiConsumer<ByteBuffer, T> encoder,
						  Function<IByteBuffer, T> decoder,
						  ObjectIntFunction<IByteBuffer, T> decoderWithType) {
			this.encodeType = encodeType;
			this.encoder = encoder;
			this.decoder = decoder;
			this.decoderWithType = decoderWithType;
		}
	}

	private static final HashMap<Class<?>, CodecFuncs<?>> codecs = new HashMap<>();
	private static final ClassValue<CodecFuncs<?>> beanKeyCodecs = new ClassValue<>() {
		@Override
		protected CodecFuncs<?> computeValue(Class<?> cls) {
			MethodHandle factory;
			try {
				factory = MethodHandles.publicLookup().findStatic(cls, "readFrom", MethodType.methodType(cls, IByteBuffer.class));
			} catch (ReflectiveOperationException e) {
				throw new IllegalArgumentException("missing BeanKey readFrom(IByteBuffer): " + cls.getName(), e);
			}
			Function<IByteBuffer, BeanKey> reader = bb -> readKey(factory, bb);
			Supplier<BeanKey> defaultKey = () -> newObject(Reflect.getDefaultConstructor(cls));
			return new CodecFuncs<>(IByteBuffer.BEAN, (bb, key) -> key.encode(bb), reader,
					(bb, tag) -> bb.ReadBeanKey(tag, cls, reader, defaultKey));
		}
	};

	private static BeanKey readKey(MethodHandle factory, IByteBuffer bb) {
		try {
			return (BeanKey)factory.invoke(bb);
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
	}

	static {
		var boolCodec = new CodecFuncs<>(IByteBuffer.INTEGER, ByteBuffer::WriteBool,
			IByteBuffer::ReadBool, IByteBuffer::ReadBool);
		var byteCodec = new CodecFuncs<>(IByteBuffer.INTEGER, (bb, obj) -> bb.WriteLong(obj.longValue()),
			bb -> (byte)bb.ReadLong(), (bb, type) -> (byte)bb.ReadLong(type));
		var shortCodec = new CodecFuncs<>(IByteBuffer.INTEGER, (bb, obj) -> bb.WriteLong(obj.longValue()),
			bb -> (short)bb.ReadLong(), (bb, type) -> (short)bb.ReadLong(type));
		var intCodec = new CodecFuncs<>(IByteBuffer.INTEGER, (bb, obj) -> bb.WriteLong(obj.longValue()),
			bb -> (int)bb.ReadLong(), (bb, type) -> (int)bb.ReadLong(type));
		var longCodec = new CodecFuncs<>(IByteBuffer.INTEGER, ByteBuffer::WriteLong,
			IByteBuffer::ReadLong, IByteBuffer::ReadLong);
		var floatCodec = new CodecFuncs<>(IByteBuffer.FLOAT, ByteBuffer::WriteFloat,
			IByteBuffer::ReadFloat, IByteBuffer::ReadFloat);
		var doubleCodec = new CodecFuncs<>(IByteBuffer.DOUBLE, ByteBuffer::WriteDouble,
			IByteBuffer::ReadDouble, IByteBuffer::ReadDouble);

		codecs.put(boolean.class, boolCodec);
		codecs.put(Boolean.class, boolCodec);
		codecs.put(byte.class, byteCodec);
		codecs.put(Byte.class, byteCodec);
		codecs.put(short.class, shortCodec);
		codecs.put(Short.class, shortCodec);
		codecs.put(int.class, intCodec);
		codecs.put(Integer.class, intCodec);
		codecs.put(long.class, longCodec);
		codecs.put(Long.class, longCodec);
		codecs.put(float.class, floatCodec);
		codecs.put(Float.class, floatCodec);
		codecs.put(double.class, doubleCodec);
		codecs.put(Double.class, doubleCodec);
		codecs.put(String.class, new CodecFuncs<>(IByteBuffer.BYTES, ByteBuffer::WriteString,
			IByteBuffer::ReadString, IByteBuffer::ReadString));
		codecs.put(Binary.class, new CodecFuncs<>(IByteBuffer.BYTES, ByteBuffer::WriteBinary,
			IByteBuffer::ReadBinary, IByteBuffer::ReadBinary));
		codecs.put(Vector2.class, new CodecFuncs<>(IByteBuffer.VECTOR2, ByteBuffer::WriteVector2,
			IByteBuffer::ReadVector2, IByteBuffer::ReadVector2));
		codecs.put(Vector2Int.class, new CodecFuncs<>(IByteBuffer.VECTOR2INT, ByteBuffer::WriteVector2Int,
			IByteBuffer::ReadVector2Int, IByteBuffer::ReadVector2Int));
		codecs.put(Vector3.class, new CodecFuncs<>(IByteBuffer.VECTOR3, ByteBuffer::WriteVector3,
			IByteBuffer::ReadVector3, IByteBuffer::ReadVector3));
		codecs.put(Vector3Int.class, new CodecFuncs<>(IByteBuffer.VECTOR3INT, ByteBuffer::WriteVector3Int,
			IByteBuffer::ReadVector3Int, IByteBuffer::ReadVector3Int));
		codecs.put(Vector4.class, new CodecFuncs<>(IByteBuffer.VECTOR4, ByteBuffer::WriteVector4,
			IByteBuffer::ReadVector4, IByteBuffer::ReadVector4));
		codecs.put(Quaternion.class, new CodecFuncs<>(IByteBuffer.VECTOR4, ByteBuffer::WriteQuaternion,
			IByteBuffer::ReadQuaternion, IByteBuffer::ReadQuaternion));
		// decimal：decimal是注册的内建schema类型且IsKeyable=true，gtable/map/
		// list/set/sortedmap全标量集合家族都会把BigDecimal送进createCodec——不注册则
		// 工厂层抛UnsupportedOperationException（生成器放行，bean类初始化即崩）。
		// 格式与LogDecimal一致：全精度字符串严格双射，encodeType取BYTES（同String）。
		codecs.put(BigDecimal.class, new CodecFuncs<>(IByteBuffer.BYTES,
			(bb, obj) -> bb.WriteString(obj.toString()),
			bb -> new BigDecimal(bb.ReadString()),
			(bb, type) -> new BigDecimal(bb.ReadString(type))));
	}

	public static <T> BiConsumer<ByteBuffer, T> createEncodeFunc(Class<T> cls) {
		//noinspection unchecked
		var codec = (CodecFuncs<T>)codecs.get(cls);
		if (codec != null)
			return codec.encoder;
		if (!Encodable.class.isAssignableFrom(cls))
			throw new UnsupportedOperationException("unsupported encoder type: " + cls.getName());
		return (bb, obj) -> ((Encodable)obj).encode(bb);
	}

	private static <T> T newObject(Constructor<T> ctor) {
		try {
			return ctor.newInstance((Object[])null);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}

	private static <T> T newObject(MethodHandle ctor) {
		try {
			//noinspection unchecked
			return (T)ctor.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
	}

	public static <T> Function<IByteBuffer, T> createDecodeFunc(Class<T> cls) {
		if (BeanKey.class.isAssignableFrom(cls))
			return keyCodec(cls).decoder;
		//noinspection unchecked
		var codec = (CodecFuncs<T>)codecs.get(cls);
		if (codec != null)
			return codec.decoder;
		if (!Serializable.class.isAssignableFrom(cls))
			throw new UnsupportedOperationException("unsupported decoder type: " + cls.getName());
		Constructor<T> ctor;
		try {
			ctor = cls.getConstructor((Class<?>[])null);
		} catch (NoSuchMethodException e) {
			throw Task.forceThrow(e);
		}
		return bb -> {
			T obj = newObject(ctor);
			((Serializable)obj).decode(bb);
			return obj;
		};
	}

	public static <T> CodecFuncs<T> createCodec(Class<T> cls) {
		if (BeanKey.class.isAssignableFrom(cls))
			return keyCodec(cls);
		//noinspection unchecked
		var codec = (CodecFuncs<T>)codecs.get(cls);
		if (codec != null)
			return codec;
		if (!Serializable.class.isAssignableFrom(cls))
			throw new UnsupportedOperationException("unsupported codec type: " + cls.getName());
		Constructor<T> ctor;
		try {
			ctor = cls.getConstructor((Class<?>[])null);
		} catch (NoSuchMethodException e) {
			throw Task.forceThrow(e);
		}
		return new CodecFuncs<>(IByteBuffer.BEAN, (bb, obj) -> ((Serializable)obj).encode(bb), bb -> {
			T obj = newObject(ctor);
			((Serializable)obj).decode(bb);
			return obj;
		}, (bb, type) -> {
			T obj = newObject(ctor);
			bb.ReadBean((Serializable)obj, type);
			return obj;
		});
	}

	public static <T> CodecFuncs<T> createCodec(Class<T> cls, MethodHandle ctor) {
		if (BeanKey.class.isAssignableFrom(cls))
			return keyCodec(cls);
		//noinspection unchecked
		var codec = (CodecFuncs<T>)codecs.get(cls);
		if (codec != null)
			return codec;
		if (!Serializable.class.isAssignableFrom(cls))
			throw new UnsupportedOperationException("unsupported codec type: " + cls.getName());
		return new CodecFuncs<>(IByteBuffer.BEAN, (bb, obj) -> ((Serializable)obj).encode(bb), bb -> {
			T obj = newObject(ctor);
			((Serializable)obj).decode(bb);
			return obj;
		}, (bb, type) -> {
			T obj = newObject(ctor);
			bb.ReadBean((Serializable)obj, type);
			return obj;
		});
	}

	@SuppressWarnings("unchecked")
	private static <T> CodecFuncs<T> keyCodec(Class<T> cls) {
		return (CodecFuncs<T>)beanKeyCodecs.get(cls);
	}

	public static MethodHandle createDynamicFactory(ToLongFunction<? super Bean> get,
													LongFunction<@Nullable Bean> create) {
		return Reflect.supplierMH.bindTo((Supplier<?>)() -> new DynamicBean(0, get, create));
	}

	private SerializeHelper() {
	}
}
