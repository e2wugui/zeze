package Zeze.Serialize;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import Zeze.Net.Binary;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.Reflect;

public final class SerializeHelper {
	@SuppressWarnings("ClassCanBeRecord")
	public static final class CodecFuncs<T> {
		public final BiConsumer<ByteBuffer, T> encoder;
		public final Function<ByteBuffer, T> decoder;

		public CodecFuncs(BiConsumer<ByteBuffer, T> encoder, Function<ByteBuffer, T> decoder) {
			this.encoder = encoder;
			this.decoder = decoder;
		}
	}

	private static final HashMap<Class<?>, CodecFuncs<?>> codecs = new HashMap<>();

	static {
		var boolCodec = new CodecFuncs<>(ByteBuffer::WriteBool, ByteBuffer::ReadBool);
		var byteCodec = new CodecFuncs<>((bb, obj) -> bb.WriteLong(((Number)obj).longValue()), bb -> (byte)bb.ReadLong());
		var shortCodec = new CodecFuncs<>((bb, obj) -> bb.WriteLong(((Number)obj).longValue()), bb -> (short)bb.ReadLong());
		var intCodec = new CodecFuncs<>((bb, obj) -> bb.WriteLong(((Number)obj).longValue()), bb -> (int)bb.ReadLong());
		var longCodec = new CodecFuncs<>((bb, obj) -> bb.WriteLong(((Number)obj).longValue()), ByteBuffer::ReadLong);
		var floatCodec = new CodecFuncs<>((bb, obj) -> bb.WriteFloat(((Number)obj).floatValue()), ByteBuffer::ReadFloat);
		var doubleCodec = new CodecFuncs<>((bb, obj) -> bb.WriteDouble(((Number)obj).doubleValue()), ByteBuffer::ReadDouble);

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
		codecs.put(String.class, new CodecFuncs<>(ByteBuffer::WriteString, ByteBuffer::ReadString));
		codecs.put(Binary.class, new CodecFuncs<>(ByteBuffer::WriteBinary, ByteBuffer::ReadBinary));
		codecs.put(Vector2.class, new CodecFuncs<>(ByteBuffer::WriteVector2, ByteBuffer::ReadVector2));
		codecs.put(Vector2Int.class, new CodecFuncs<>(ByteBuffer::WriteVector2Int, ByteBuffer::ReadVector2Int));
		codecs.put(Vector3.class, new CodecFuncs<>(ByteBuffer::WriteVector3, ByteBuffer::ReadVector3));
		codecs.put(Vector3Int.class, new CodecFuncs<>(ByteBuffer::WriteVector3Int, ByteBuffer::ReadVector3Int));
		codecs.put(Vector4.class, new CodecFuncs<>(ByteBuffer::WriteVector4, ByteBuffer::ReadVector4));
		codecs.put(Quaternion.class, new CodecFuncs<>(ByteBuffer::WriteQuaternion, ByteBuffer::ReadQuaternion));
	}

	public static <T> BiConsumer<ByteBuffer, T> createEncodeFunc(Class<T> cls) {
		@SuppressWarnings("unchecked")
		var codec = (CodecFuncs<T>)codecs.get(cls);
		if (codec != null)
			return codec.encoder;
		if (!Serializable.class.isAssignableFrom(cls))
			throw new UnsupportedOperationException("unsupported encoder type: " + cls.getName());
		return (bb, obj) -> ((Serializable)obj).encode(bb);
	}

	public static <T> Function<ByteBuffer, T> createDecodeFunc(Class<T> cls) {
		@SuppressWarnings("unchecked")
		var codec = (CodecFuncs<T>)codecs.get(cls);
		if (codec != null)
			return codec.decoder;
		if (!Serializable.class.isAssignableFrom(cls))
			throw new UnsupportedOperationException("unsupported decoder type: " + cls.getName());
		return bb -> {
			T obj;
			try {
				obj = cls.getConstructor((Class<?>[])null).newInstance((Object[])null);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			((Serializable)obj).decode(bb);
			return obj;
		};
	}

	public static <T> CodecFuncs<T> createCodec(Class<T> cls) {
		@SuppressWarnings("unchecked")
		var codec = (CodecFuncs<T>)codecs.get(cls);
		if (codec != null)
			return codec;
		if (!Serializable.class.isAssignableFrom(cls))
			throw new UnsupportedOperationException("unsupported codec type: " + cls.getName());
		return new CodecFuncs<>((bb, obj) -> ((Serializable)obj).encode(bb), bb -> {
			T obj;
			try {
				obj = cls.getConstructor((Class<?>[])null).newInstance((Object[])null);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			((Serializable)obj).decode(bb);
			return obj;
		});
	}

	public static MethodHandle createDynamicFactory(ToLongFunction<Bean> get, LongFunction<Bean> create) {
		return Reflect.supplierMH.bindTo((Supplier<?>)() -> new DynamicBean(0, get, create));
	}

	private SerializeHelper() {
	}
}
