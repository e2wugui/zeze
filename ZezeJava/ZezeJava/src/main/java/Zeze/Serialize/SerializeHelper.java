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
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public final class SerializeHelper {
	public interface IntObjectFunction<T, R> {
		R apply(T t, int i);
	}

	@SuppressWarnings("ClassCanBeRecord")
	public static final class CodecFuncs<T> {
		public final int encodeType;
		public final @NotNull BiConsumer<ByteBuffer, T> encoder;
		public final @NotNull Function<IByteBuffer, T> decoder;
		public final @NotNull IntObjectFunction<IByteBuffer, T> decoderWithType;

		public CodecFuncs(int encodeType, @NotNull BiConsumer<ByteBuffer, T> encoder,
						  @NotNull Function<IByteBuffer, T> decoder,
						  @NotNull IntObjectFunction<IByteBuffer, T> decoderWithType) {
			this.encodeType = encodeType;
			this.encoder = encoder;
			this.decoder = decoder;
			this.decoderWithType = decoderWithType;
		}
	}

	private static final HashMap<Class<?>, CodecFuncs<?>> codecs = new HashMap<>();

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
	}

	public static <T> @NotNull BiConsumer<ByteBuffer, T> createEncodeFunc(@NotNull Class<T> cls) {
		@SuppressWarnings("unchecked")
		var codec = (CodecFuncs<T>)codecs.get(cls);
		if (codec != null)
			return codec.encoder;
		if (!Serializable.class.isAssignableFrom(cls))
			throw new UnsupportedOperationException("unsupported encoder type: " + cls.getName());
		return (bb, obj) -> ((Serializable)obj).encode(bb);
	}

	public static <T> @NotNull Function<IByteBuffer, T> createDecodeFunc(@NotNull Class<T> cls) {
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
				throw Task.forceThrow(e);
			}
			((Serializable)obj).decode(bb);
			return obj;
		};
	}

	public static <T> @NotNull CodecFuncs<T> createCodec(@NotNull Class<T> cls) {
		@SuppressWarnings("unchecked")
		var codec = (CodecFuncs<T>)codecs.get(cls);
		if (codec != null)
			return codec;
		if (!Serializable.class.isAssignableFrom(cls))
			throw new UnsupportedOperationException("unsupported codec type: " + cls.getName());
		return new CodecFuncs<>(IByteBuffer.BEAN, (bb, obj) -> ((Serializable)obj).encode(bb), bb -> {
			T obj;
			try {
				obj = cls.getConstructor((Class<?>[])null).newInstance((Object[])null);
			} catch (ReflectiveOperationException e) {
				throw Task.forceThrow(e);
			}
			((Serializable)obj).decode(bb);
			return obj;
		}, (bb, type) -> {
			T obj;
			try {
				obj = cls.getConstructor((Class<?>[])null).newInstance((Object[])null);
			} catch (ReflectiveOperationException e) {
				throw Task.forceThrow(e);
			}
			bb.ReadBean((Serializable)obj, type);
			return obj;
		});
	}

	public static @NotNull MethodHandle createDynamicFactory(@NotNull ToLongFunction<Bean> get,
															 @NotNull LongFunction<Bean> create) {
		return Reflect.supplierMH.bindTo((Supplier<?>)() -> new DynamicBean(0, get, create));
	}

	private SerializeHelper() {
	}
}
