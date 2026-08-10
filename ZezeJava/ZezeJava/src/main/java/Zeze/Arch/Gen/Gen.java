package Zeze.Arch.Gen;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
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
import Zeze.Transaction.Data;
import Zeze.Util.Action3;
import Zeze.Util.Action4;
import Zeze.Util.StringBuilderCs;
import org.jetbrains.annotations.NotNull;

final class Gen {
	static final Gen instance = new Gen();

	private record KnownSerializer(Action4<StringBuilderCs, String, String, String> encoder,
	                               Action4<StringBuilderCs, String, String, String> decoder,
	                               Action3<StringBuilderCs, String, String> define,
	                               Action3<StringBuilderCs, String, String> definePrim,
	                               Action3<StringBuilderCs, String, String> assignDef,
	                               Supplier<String> typeName) {
	}

	private final HashMap<Class<?>, KnownSerializer> serializers = new HashMap<>();

	private Gen() {
		serializers.put(Boolean.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteBool({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadBool();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Boolean {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}boolean {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = null;", prefix, varName),
				() -> "Boolean")
		);
		serializers.put(boolean.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteBool({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadBool();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}boolean {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}boolean {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = false;", prefix, varName),
				() -> "boolean")
		);
		serializers.put(Byte.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (byte){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Byte {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}byte {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = null;", prefix, varName),
				() -> "Byte")
		);
		serializers.put(byte.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (byte){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}byte {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}byte {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = 0;", prefix, varName),
				() -> "byte")
		);
		serializers.put(Short.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (short){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Short {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}short {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = null;", prefix, varName),
				() -> "Short")
		);
		serializers.put(short.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (short){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}short {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}short {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = 0;", prefix, varName),
				() -> "short")
		);
		serializers.put(Integer.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (int){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Integer {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}int {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = null;", prefix, varName),
				() -> "Integer")
		);
		serializers.put(int.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (int){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}int {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}int {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = 0;", prefix, varName),
				() -> "int")
		);
		serializers.put(Long.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Long {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}long {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = null;", prefix, varName),
				() -> "Long")
		);
		serializers.put(long.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}long {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}long {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = 0;", prefix, varName),
				() -> "long")
		);
		serializers.put(Float.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteFloat({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadFloat();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Float {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}float {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = null;", prefix, varName),
				() -> "Float")
		);
		serializers.put(float.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteFloat({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadFloat();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}float {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}float {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = 0;", prefix, varName),
				() -> "float")
		);
		serializers.put(Double.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteDouble({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadDouble();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Double {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}double {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = null;", prefix, varName),
				() -> "Double")
		);
		serializers.put(double.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteDouble({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadDouble();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}double {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}double {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = 0;", prefix, varName),
				() -> "double")
		);
		serializers.put(String.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteString({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadString();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}String {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}String {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = null;", prefix, varName),
				() -> "String")
		);
		serializers.put(byte[].class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteBytes({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadBytes();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}byte[] {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}byte[] {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = null;", prefix, varName),
				() -> "byte[]")
		);
		serializers.put(Binary.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteBinary({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadBinary();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Zeze.Net.Binary {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}Zeze.Net.Binary {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = null;", prefix, varName),
				() -> "Zeze.Net.Binary")
		);
		serializers.put(ByteBuffer.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteByteBuffer({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = Zeze.Serialize.ByteBuffer.Wrap({}.ReadBytes());", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Zeze.Serialize.ByteBuffer {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}Zeze.Serialize.ByteBuffer {};", prefix, varName),
				(sb, prefix, varName) -> sb.appendLine("{}{} = null;", prefix, varName),
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
		var paramType = param.getParameterizedType();
		if (Collection.class.isAssignableFrom(type) && paramType instanceof ParameterizedType) {
			var elemType = ((ParameterizedType)paramType).getActualTypeArguments()[0];
			if (elemType instanceof Class<?> elemClass) {
				var serializer = serializers.get(elemClass);
				if (!isAbstract(elemClass) && (serializer != null || Serializable.class.isAssignableFrom(elemClass))) {
					sb.appendLine("{}{}<{}> {};", prefix, getCollectionType(type).getTypeName().replace('$', '.'),
							elemType.getTypeName().replace('$', '.'), name);
					return;
				}
			}
		}
		if (Map.class.isAssignableFrom(type) && paramType instanceof ParameterizedType) {
			var keyType = ((ParameterizedType)paramType).getActualTypeArguments()[0];
			var valueType = ((ParameterizedType)paramType).getActualTypeArguments()[1];
			if (keyType instanceof Class<?> keyClass && valueType instanceof Class<?> valueClass) {
				var keySerializer = serializers.get(keyClass);
				var valueSerializer = serializers.get(valueClass);
				if (!isAbstract(keyClass) && (keySerializer != null || Serializable.class.isAssignableFrom(keyClass)) &&
						!isAbstract(valueClass) && (valueSerializer != null || Serializable.class.isAssignableFrom(valueClass))) {
					sb.appendLine("{}{}<{}, {}> {};", prefix, getMapType(type).getTypeName().replace('$', '.'),
							keyType.getTypeName().replace('$', '.'), valueType.getTypeName().replace('$', '.'), name);
					return;
				}
			}
		}
		sb.appendLine("{}{} {};", prefix, getTypeName(paramType), name);
	}

	private void genEncode(StringBuilderCs sb, String prefix, String bbName, Class<?> type, Type paramType,
	                       String varName) throws Exception {
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
			if (elemType instanceof Class<?> elemClass) {
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
			if (keyType instanceof Class<?> keyClass && valueType instanceof Class<?> valueClass) {
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

	private void genDecode(StringBuilderCs sb, String prefix, String bbName, Class<?> type, Type paramType,
	                       String varName, boolean isField) throws Exception {
		var kn = serializers.get(type);
		if (kn != null) {
			kn.decoder.run(sb, prefix, varName, bbName);
			return;
		}
		if (Serializable.class.isAssignableFrom(type)) {
			if (type == Bean.class)
				sb.appendLine("{}{} = beanFactory.createBeanFromSpecialTypeId({}.ReadLong());", prefix, varName, bbName);
			else if (type == Data.class)
				sb.appendLine("{}{} = beanFactory.createDataFromSpecialTypeId({}.ReadLong());", prefix, varName, bbName);
			else if (!isAbstract(type) || !isField)
				sb.appendLine("{}{} = new {}();", prefix, varName, getTypeName(paramType));
			sb.appendLine("{}{}.decode({});", prefix, varName, bbName);
			return;
		}
		if (Collection.class.isAssignableFrom(type) && paramType instanceof ParameterizedType) {
			var elemType = ((ParameterizedType)paramType).getActualTypeArguments()[0];
			if (elemType instanceof Class<?> elemClass) {
				var serializer = serializers.get(elemClass);
				if (serializer != null || Serializable.class.isAssignableFrom(elemClass)) {
					if (!isAbstract(type) || !isField) {
						sb.appendLine("{}{} = new {}<>();", prefix, varName,
								getCollectionType(type).getTypeName().replace('$', '.'));
					}
					sb.appendLine("{}for (int _n_ = {}.ReadUInt(); _n_ > 0; _n_--) {", prefix, bbName);
					var prefix1 = prefix + "    ";
					if (serializer != null) {
						serializer.definePrim.run(sb, prefix1, "_e_");
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
			if (keyType instanceof Class<?> keyClass && valueType instanceof Class<?> valueClass) {
				var keySerializer = serializers.get(keyClass);
				var valueSerializer = serializers.get(valueClass);
				if ((keySerializer != null || Serializable.class.isAssignableFrom(keyClass)) &&
						(valueSerializer != null || Serializable.class.isAssignableFrom(valueClass))) {
					if (!isAbstract(type) || !isField) {
						sb.appendLine("{}{} = new {}<>();", prefix, varName,
								getMapType(type).getTypeName().replace('$', '.'));
					}
					sb.appendLine("{}for (int _n_ = {}.ReadUInt(); _n_ > 0; _n_--) {", prefix, bbName);
					var prefix1 = prefix + "    ";
					if (keySerializer != null) {
						keySerializer.definePrim.run(sb, prefix1, "_k_");
						keySerializer.decoder.run(sb, prefix1, "_k_", bbName);
					} else {
						sb.appendLine("{}var _k_ = new {}();", prefix1, keyType.getTypeName().replace('$', '.'));
						sb.appendLine("{}_k_.decode({});", prefix1, bbName);
					}
					if (valueSerializer != null) {
						valueSerializer.definePrim.run(sb, prefix1, "_v_");
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
	void genEncode(StringBuilderCs sb, String prefix, String bbName, String mName,
	               String fieldPrefix, List<? extends AnnotatedElement> elements,
	               Parameter redirectKeyParam) throws Exception {
		if (elements.isEmpty())
			return;
		if (elements.size() > 64)
			throw new IllegalArgumentException("too many element count: " + elements.size() + " > 64");
		sb.appendLine("{}var {} = 0L;", prefix, mName);
		int i = 0;
		for (AnnotatedElement e : elements) {
			Class<?> cls;
			String name;
			if (e instanceof Parameter p) {
				cls = p.getType();
				name = p.getName();
			} else if (e instanceof Field f) {
				cls = f.getType();
				name = fieldPrefix + f.getName();
			} else
				throw new IllegalArgumentException("unsupported element type: " + (e != null ? e.getClass().getName() : null));
			if (!cls.isPrimitive() && e != redirectKeyParam) {
				sb.appendLine("{}if ({} == null)", prefix, name);
				sb.appendLine("{}    {} += 0x{}L;", prefix, mName, Long.toHexString(1L << i));
			}
			i++;
		}
		sb.appendLine("{}{}.WriteULong({});", prefix, bbName, mName);
		var prefix1 = prefix + "    ";
		i = 0;
		for (AnnotatedElement e : elements) {
			Class<?> cls;
			Type type;
			String name;
			boolean isField;
			if (e instanceof Parameter p) {
				cls = p.getType();
				type = p.getParameterizedType();
				name = p.getName();
				isField = false;
			} else if (e instanceof Field f) {
				cls = f.getType();
				type = f.getGenericType();
				name = fieldPrefix + f.getName();
				isField = true;
			} else
				throw new IllegalArgumentException("unsupported element type: " + (e != null ? e.getClass().getName() : null));
			if (cls.isPrimitive() || e == redirectKeyParam)
				genEncode(sb, prefix, bbName, cls, type, name);
			else {
				if (isField)
					sb.appendLine("{}if (({} & 0x{}L) == 0) {", prefix, mName, Long.toHexString(1L << i)); // 字段值有小概率不稳定,为了避免序列化出问题,这里以之前的判空为准
				else
					sb.appendLine("{}if ({} != null) {", prefix, name); // 让下面的判空分析通过,参数变量是稳定的,两次判空结果一定相同
				genEncode(sb, prefix1, bbName, cls, type, name);
				sb.appendLine("{}}", prefix);
			}
			i++;
		}
	}

	void genDecode(StringBuilderCs sb, String prefix, String bbName, String mName, String fieldPrefix,
	               List<? extends AnnotatedElement> elements) throws Exception {
		if (elements.isEmpty())
			return;
		if (elements.size() > 64)
			throw new IllegalArgumentException("too many element count: " + elements.size() + " > 64");
		sb.appendLine("{}var {} = {}.ReadULong();", prefix, mName, bbName);
		var prefix1 = prefix + "    ";
		int i = 0;
		for (AnnotatedElement e : elements) {
			Class<?> cls;
			Type type;
			String name;
			boolean isField;
			if (e instanceof Parameter p) {
				cls = p.getType();
				type = p.getParameterizedType();
				name = p.getName();
				isField = false;
			} else if (e instanceof Field f) {
				cls = f.getType();
				type = f.getGenericType();
				name = fieldPrefix + f.getName();
				isField = true;
			} else
				throw new IllegalArgumentException("unsupported element type: " + (e != null ? e.getClass().getName() : null));
			if (cls.isPrimitive()) {
				sb.appendLine("{}if (({} & 0x{}L) == 0) {", prefix, mName, Long.toHexString(1L << i));
				genDecode(sb, prefix1, bbName, cls, type, name, isField);
				sb.appendLine("{}} else", prefix);
				serializers.get(cls).assignDef.run(sb, prefix1, name);
			} else {
				sb.appendLine("{}if (({} & 0x{}L) == 0) {", prefix, mName, Long.toHexString(1L << i));
				genDecode(sb, prefix1, bbName, cls, type, name, isField);
				sb.appendLine("{}} else", prefix);
				sb.appendLine("{}{} = null;", prefix1, name);
			}
			i++;
		}
	}
}
