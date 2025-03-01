package Zeze.Arch.Gen;

import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Util.Action3;
import Zeze.Util.Action4;
import Zeze.Util.StringBuilderCs;
import org.jetbrains.annotations.NotNull;

final class Gen {
	static final Gen instance = new Gen();

	private static final class KnownSerializer {
		final Action4<StringBuilderCs, String, String, String> encoder;
		final Action4<StringBuilderCs, String, String, String> decoder;
		final Action3<StringBuilderCs, String, String> define;
		final Supplier<String> typeName;

		KnownSerializer(Action4<StringBuilderCs, String, String, String> enc,
						Action4<StringBuilderCs, String, String, String> dec,
						Action3<StringBuilderCs, String, String> def,
						Supplier<String> typeName) {
			encoder = enc;
			decoder = dec;
			define = def;
			this.typeName = typeName;
		}
	}

	private final HashMap<Class<?>, KnownSerializer> serializers = new HashMap<>();

	private Gen() {
		serializers.put(Boolean.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteBool({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadBool();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}boolean {};", prefix, varName),
				() -> "Boolean")
		);
		serializers.put(boolean.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteBool({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadBool();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}boolean {};", prefix, varName),
				() -> "boolean")
		);
		serializers.put(Byte.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (byte){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}byte {1};", prefix, varName),
				() -> "Byte")
		);
		serializers.put(byte.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (byte){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}byte {};", prefix, varName),
				() -> "byte")
		);
		serializers.put(Short.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (short){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}short {};", prefix, varName),
				() -> "Short")
		);
		serializers.put(short.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (short){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}short {};", prefix, varName),
				() -> "short")
		);
		serializers.put(Integer.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (int){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}int {};", prefix, varName),
				() -> "Integer")
		);
		serializers.put(int.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (int){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}int {};", prefix, varName),
				() -> "int")
		);
		serializers.put(Long.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}long {};", prefix, varName),
				() -> "Long")
		);
		serializers.put(long.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}long {};", prefix, varName),
				() -> "long")
		);
		serializers.put(Float.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteFloat({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadFloat();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}float {};", prefix, varName),
				() -> "Float")
		);
		serializers.put(float.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteFloat({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadFloat();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}float {};", prefix, varName),
				() -> "float")
		);
		serializers.put(Double.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteDouble({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadDouble();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}double {};", prefix, varName),
				() -> "Double")
		);
		serializers.put(double.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteDouble({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadDouble();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}double {};", prefix, varName),
				() -> "double")
		);
		serializers.put(String.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteString({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadString();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}String {};", prefix, varName),
				() -> "String")
		);
		serializers.put(byte[].class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteBytes({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadBytes();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}byte[] {};", prefix, varName),
				() -> "byte[]")
		);
		serializers.put(Binary.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteBinary({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadBinary();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Zeze.Net.Binary {};", prefix, varName),
				() -> "Zeze.Net.Binary")
		);
		serializers.put(ByteBuffer.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteByteBuffer({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = Zeze.Serialize.ByteBuffer.Wrap({}.ReadBytes());", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Zeze.Serialize.ByteBuffer {};", prefix, varName),
				() -> "Zeze.Serialize.ByteBuffer")
		);
	}

	String getTypeName(Type type) {
		if (type instanceof Class) {
			var kn = serializers.get(type);
			return kn != null ? kn.typeName.get() : type.getTypeName().replace('$', '.');
		}
		return type.toString().replace('$', '.'); // ParameterizedType
	}

	private static boolean isAbstract(@NotNull Class<?> klass) {
		return (klass.getModifiers() & (Modifier.INTERFACE | Modifier.ABSTRACT)) != 0;
	}

	private static Class<?> getCollectionType(@NotNull Class<?> klass) {
		if (isAbstract(klass)) {
			if (klass.isAssignableFrom(ArrayList.class))
				klass = ArrayList.class;
			else if (klass.isAssignableFrom(HashSet.class))
				klass = HashSet.class;
			else if (klass.isAssignableFrom(TreeSet.class))
				klass = TreeSet.class;
			else if (klass.isAssignableFrom(ArrayDeque.class))
				klass = ArrayDeque.class;
			else if (klass.isAssignableFrom(LinkedList.class))
				klass = LinkedList.class;
			else
				throw new UnsupportedOperationException("unsupported collection type: " + klass.getName());
		}
		return klass;
	}

	private static Class<?> getMapType(@NotNull Class<?> klass) {
		if (isAbstract(klass)) {
			if (klass.isAssignableFrom(HashMap.class))
				klass = HashMap.class;
			else if (klass.isAssignableFrom(TreeMap.class))
				klass = TreeMap.class;
			else if (klass.isAssignableFrom(LinkedHashMap.class))
				klass = LinkedHashMap.class;
			else
				throw new UnsupportedOperationException("unsupported map type: " + klass.getName());
		}
		return klass;
	}

	@SuppressWarnings("SameParameterValue")
	void genLocalVariable(StringBuilderCs sb, String prefix, Parameter param) throws Exception {
		var type = param.getType();
		var name = param.getName();
		var kn = serializers.get(type);
		if (kn != null) {
			kn.define.run(sb, prefix, name);
			return;
		}
		if (Serializable.class.isAssignableFrom(type)) {
			if (type != Bean.class)
				sb.appendLine("{}var {} = new {}();", prefix, name, type.getTypeName().replace('$', '.'));
			return;
		}
		var paramType = param.getParameterizedType();
		if (Collection.class.isAssignableFrom(type) && paramType instanceof ParameterizedType) {
			var elemType = ((ParameterizedType)paramType).getActualTypeArguments()[0];
			if (elemType instanceof Class) {
				var elemClass = (Class<?>)elemType;
				var serializer = serializers.get(elemClass);
				if (!isAbstract(elemClass) && (serializer != null || Serializable.class.isAssignableFrom(elemClass))) {
					sb.appendLine("{}var {} = new {}<{}>();", prefix, name,
							getCollectionType(type).getTypeName().replace('$', '.'),
							elemType.getTypeName().replace('$', '.'));
					return;
				}
			}
		}
		if (Map.class.isAssignableFrom(type) && paramType instanceof ParameterizedType) {
			var keyType = ((ParameterizedType)paramType).getActualTypeArguments()[0];
			var valueType = ((ParameterizedType)paramType).getActualTypeArguments()[1];
			if (keyType instanceof Class && valueType instanceof Class) {
				var keyClass = (Class<?>)keyType;
				var valueClass = (Class<?>)valueType;
				var keySerializer = serializers.get(keyClass);
				var valueSerializer = serializers.get(valueClass);
				if (!isAbstract(keyClass) && (keySerializer != null || Serializable.class.isAssignableFrom(keyClass)) &&
						!isAbstract(valueClass) && (valueSerializer != null || Serializable.class.isAssignableFrom(valueClass))) {
					sb.appendLine("{}var {} = new {}<{}, {}>();", prefix, name,
							getMapType(type).getTypeName().replace('$', '.'),
							keyType.getTypeName().replace('$', '.'),
							valueType.getTypeName().replace('$', '.'));
					return;
				}
			}
		}
		sb.appendLine("{}{} {};", prefix, getTypeName(paramType), name);
	}

	void genEncode(StringBuilderCs sb, String prefix, String bbName, Class<?> type, Type paramType, String varName) throws Exception {
		var kn = serializers.get(type);
		if (kn != null) {
			kn.encoder.run(sb, prefix, varName, bbName);
			return;
		}
		if (Serializable.class.isAssignableFrom(type)) {
			if (type == Bean.class)
				sb.appendLine("{}{}.WriteLong({}.typeId());", prefix, bbName, varName);
			sb.appendLine("{}{}.encode({});", prefix, varName, bbName);
			return;
		}
		if (Collection.class.isAssignableFrom(type) && paramType instanceof ParameterizedType) {
			var elemType = ((ParameterizedType)paramType).getActualTypeArguments()[0];
			if (elemType instanceof Class) {
				var elemClass = (Class<?>)elemType;
				var serializer = serializers.get(elemClass);
				if (!isAbstract(elemClass) && (serializer != null || Serializable.class.isAssignableFrom(elemClass))) {
					sb.appendLine("{}{}.WriteUInt({}.size());", prefix, bbName, varName);
					sb.appendLine("{}for (var _e_ : {})", prefix, varName);
					if (serializer != null)
						serializer.encoder.run(sb, prefix + "    ", "_e_", bbName);
					else
						sb.appendLine("{}    _e_.encode({});", prefix, bbName);
					return;
				}
			}
		}
		if (Map.class.isAssignableFrom(type) && paramType instanceof ParameterizedType) {
			var keyType = ((ParameterizedType)paramType).getActualTypeArguments()[0];
			var valueType = ((ParameterizedType)paramType).getActualTypeArguments()[1];
			if (keyType instanceof Class && valueType instanceof Class) {
				var keyClass = (Class<?>)keyType;
				var valueClass = (Class<?>)valueType;
				var keySerializer = serializers.get(keyClass);
				var valueSerializer = serializers.get(valueClass);
				if (!isAbstract(keyClass) && (keySerializer != null || Serializable.class.isAssignableFrom(keyClass)) &&
						!isAbstract(valueClass) && (valueSerializer != null || Serializable.class.isAssignableFrom(valueClass))) {
					sb.appendLine("{}{}.WriteUInt({}.size());", prefix, bbName, varName);
					sb.appendLine("{}for (var _e_ : {}.entrySet()) {", prefix, varName);
					if (keySerializer != null)
						keySerializer.encoder.run(sb, prefix + "    ", "_e_.getKey()", bbName);
					else
						sb.appendLine("{}    _e_.getKey().encode({});", prefix, bbName);
					if (valueSerializer != null)
						valueSerializer.encoder.run(sb, prefix + "    ", "_e_.getValue()", bbName);
					else
						sb.appendLine("{}    _e_.getValue().encode({});", prefix, bbName);
					sb.appendLine("{}}", prefix);
					return;
				}
				if (!java.io.Serializable.class.isAssignableFrom(keyClass) || !java.io.Serializable.class.isAssignableFrom(valueClass))
					throw new UnsupportedOperationException("unsupported param type: " + paramType.getTypeName());
			} else
				throw new UnsupportedOperationException("unsupported param type: " + paramType.getTypeName());
		}
		if (java.io.Serializable.class.isAssignableFrom(type)) {
			sb.appendLine("{}{}.WriteJavaObject({});", prefix, bbName, varName);
			return;
		}
		throw new UnsupportedOperationException("unsupported param type: " + type.getName());
	}

	void genDecode(StringBuilderCs sb, String prefix, String bbName, Class<?> type, Type paramType, String varName) throws Exception {
		var kn = serializers.get(type);
		if (kn != null) {
			kn.decoder.run(sb, prefix, varName, bbName);
			return;
		}
		if (Serializable.class.isAssignableFrom(type)) {
			if (type == Bean.class)
				sb.appendLine("{}var {} = beanFactory.createBeanFromSpecialTypeId({}.ReadLong());", prefix, varName, bbName);
			sb.appendLine("{}{}.decode({});", prefix, varName, bbName);
			return;
		}
		if (Collection.class.isAssignableFrom(type) && paramType instanceof ParameterizedType) {
			var elemType = ((ParameterizedType)paramType).getActualTypeArguments()[0];
			if (elemType instanceof Class) {
				var elemClass = (Class<?>)elemType;
				var serializer = serializers.get(elemClass);
				if (!isAbstract(elemClass) && (serializer != null || Serializable.class.isAssignableFrom(elemClass))) {
					sb.appendLine("{}for (int _n_ = {}.ReadUInt(); _n_ > 0; _n_--) {", prefix, bbName);
					var prefix1 = prefix + "    ";
					if (serializer != null) {
						serializer.define.run(sb, prefix1, "_e_");
						serializer.decoder.run(sb, prefix1, "_e_", bbName);
					} else {
						sb.appendLine("{}var _e_ = new {}();", prefix1, elemType.getTypeName().replace('$', '.'));
						sb.appendLine("{}_e_.decode({});", prefix1, bbName);
					}
					sb.appendLine("{}{}.add(_e_);", prefix1, varName);
					sb.appendLine("{}}", prefix);
					return;
				}
			}
		}
		if (Map.class.isAssignableFrom(type) && paramType instanceof ParameterizedType) {
			var keyType = ((ParameterizedType)paramType).getActualTypeArguments()[0];
			var valueType = ((ParameterizedType)paramType).getActualTypeArguments()[1];
			if (keyType instanceof Class && valueType instanceof Class) {
				var keyClass = (Class<?>)keyType;
				var valueClass = (Class<?>)valueType;
				var keySerializer = serializers.get(keyClass);
				var valueSerializer = serializers.get(valueClass);
				if (!isAbstract(keyClass) && (keySerializer != null || Serializable.class.isAssignableFrom(keyClass)) &&
						!isAbstract(valueClass) && (valueSerializer != null || Serializable.class.isAssignableFrom(valueClass))) {
					sb.appendLine("{}for (int _n_ = {}.ReadUInt(); _n_ > 0; _n_--) {", prefix, bbName);
					var prefix1 = prefix + "    ";
					if (keySerializer != null) {
						keySerializer.define.run(sb, prefix1, "_k_");
						keySerializer.decoder.run(sb, prefix1, "_k_", bbName);
					} else {
						sb.appendLine("{}var _k_ = new {}();", prefix1, keyType.getTypeName().replace('$', '.'));
						sb.appendLine("{}_k_.decode({});", prefix1, bbName);
					}
					if (valueSerializer != null) {
						valueSerializer.define.run(sb, prefix1, "_v_");
						valueSerializer.decoder.run(sb, prefix1, "_v_", bbName);
					} else {
						sb.appendLine("{}var _v_ = new {}();", prefix1, valueType.getTypeName().replace('$', '.'));
						sb.appendLine("{}_v_.decode({});", prefix1, bbName);
					}
					sb.appendLine("{}{}.put(_k_, _v_);", prefix1, varName);
					sb.appendLine("{}}", prefix);
					return;
				}
				if (!java.io.Serializable.class.isAssignableFrom(keyClass) || !java.io.Serializable.class.isAssignableFrom(valueClass))
					throw new UnsupportedOperationException("unsupported param type: " + paramType.getTypeName());
			} else
				throw new UnsupportedOperationException("unsupported param type: " + paramType.getTypeName());
		}
		if (java.io.Serializable.class.isAssignableFrom(type)) {
			sb.appendLine("{}{} = {}.ReadJavaObject();", prefix, varName, bbName);
			return;
		}
		throw new UnsupportedOperationException("unsupported param type: " + type.getName());
	}

	@SuppressWarnings("SameParameterValue")
	void genEncode(StringBuilderCs sb, String prefix, String bbName, List<Parameter> parameters) throws Exception {
		for (Parameter p : parameters)
			genEncode(sb, prefix, bbName, p.getType(), p.getParameterizedType(), p.getName());
	}

	@SuppressWarnings("SameParameterValue")
	void genDecode(StringBuilderCs sb, String prefix, String bbName, List<Parameter> parameters) throws Exception {
		for (Parameter p : parameters)
			genDecode(sb, prefix, bbName, p.getType(), p.getParameterizedType(), p.getName());
	}
}
