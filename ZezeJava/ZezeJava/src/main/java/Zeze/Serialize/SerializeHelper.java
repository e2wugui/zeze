package Zeze.Serialize;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import Zeze.Net.Binary;

public final class SerializeHelper {
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
	}

	public static <T> BiConsumer<ByteBuffer, T> createEncodeFunc(Class<T> cls) {
		@SuppressWarnings("unchecked")
		var codec = (CodecFuncs<T>)codecs.get(cls);
		if (codec != null)
			return codec.encoder;
		if (!Serializable.class.isAssignableFrom(cls))
			throw new UnsupportedOperationException("unsupported encoder type: " + cls.getName());
		return (bb, obj) -> ((Serializable)obj).Encode(bb);
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
			((Serializable)obj).Decode(bb);
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
		return new CodecFuncs<>((bb, obj) -> ((Serializable)obj).Encode(bb),
				bb -> {
					T obj;
					try {
						obj = cls.getConstructor((Class<?>[])null).newInstance((Object[])null);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
					((Serializable)obj).Decode(bb);
					return obj;
				});
	}

	private SerializeHelper() {
	}
}
