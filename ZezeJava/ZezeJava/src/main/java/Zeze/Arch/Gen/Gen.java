package Zeze.Arch.Gen;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Action3;
import Zeze.Util.Action4;
import Zeze.Util.StringBuilderCs;

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

	private final HashMap<Class<?>, KnownSerializer> serializer = new HashMap<>();

	private Gen() {
		serializer.put(Boolean.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteBool({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadBool();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}boolean {};", prefix, varName),
				() -> "Boolean")
		);
		serializer.put(boolean.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteBool({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadBool();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}boolean {};", prefix, varName),
				() -> "boolean")
		);
		serializer.put(Byte.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (byte){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}byte {1};", prefix, varName),
				() -> "Byte")
		);
		serializer.put(byte.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (byte){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}byte {};", prefix, varName),
				() -> "byte")
		);
		serializer.put(Short.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (short){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}short {};", prefix, varName),
				() -> "Short")
		);
		serializer.put(short.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (short){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}short {};", prefix, varName),
				() -> "short")
		);
		serializer.put(Integer.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (int){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}int {};", prefix, varName),
				() -> "Integer")
		);
		serializer.put(int.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = (int){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}int {};", prefix, varName),
				() -> "int")
		);
		serializer.put(Long.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}long {};", prefix, varName),
				() -> "Long")
		);
		serializer.put(long.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}long {};", prefix, varName),
				() -> "long")
		);
		serializer.put(Float.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteFloat({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadFloat();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}float {};", prefix, varName),
				() -> "Float")
		);
		serializer.put(float.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteFloat({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadFloat();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}float {};", prefix, varName),
				() -> "float")
		);
		serializer.put(Double.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteDouble({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadDouble();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}double {};", prefix, varName),
				() -> "Double")
		);
		serializer.put(double.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteDouble({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadDouble();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}double {};", prefix, varName),
				() -> "double")
		);
		serializer.put(String.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteString({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadString();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}String {};", prefix, varName),
				() -> "String")
		);
		serializer.put(byte[].class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteBytes({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadBytes();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}byte[] {};", prefix, varName),
				() -> "byte[]")
		);
		serializer.put(Binary.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteBinary({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = {}.ReadBinary();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Zeze.Net.Binary {};", prefix, varName),
				() -> "Zeze.Net.Binary")
		);
		serializer.put(ByteBuffer.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{}.WriteByteBuffer({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.appendLine("{}{} = Zeze.Serialize.ByteBuffer.Wrap({}.ReadBytes());", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.appendLine("{}Zeze.Serialize.ByteBuffer {};", prefix, varName),
				() -> "Zeze.Serialize.ByteBuffer")
		);
	}

	String getTypeName(Type type) {
		if (type instanceof Class) {
			var kn = serializer.get(type);
			return kn != null ? kn.typeName.get() : type.getTypeName().replace('$', '.');
		}
		return type.toString().replace('$', '.'); // ParameterizedType
	}

	@SuppressWarnings("SameParameterValue")
	void genLocalVariable(StringBuilderCs sb, String prefix, Parameter param) throws Throwable {
		var type = param.getType();
		var name = param.getName();
		var kn = serializer.get(type);
		if (kn != null)
			kn.define.run(sb, prefix, name);
		else if (Serializable.class.isAssignableFrom(type))
			sb.appendLine("{}var {} = new {}();", prefix, name, type.getTypeName());
		else
			sb.appendLine("{}{} {};", prefix, getTypeName(param.getParameterizedType()), name);
	}

	void genEncode(StringBuilderCs sb, String prefix, String bbName, Class<?> type, String varName) throws Throwable {
		var kn = serializer.get(type);
		if (kn != null)
			kn.encoder.run(sb, prefix, varName, bbName);
		else if (Serializable.class.isAssignableFrom(type))
			sb.appendLine("{}{}.encode({});", prefix, varName, bbName);
		else {
			sb.appendLine("{}try (var _bs_ = new java.io.ByteArrayOutputStream();", prefix);
			sb.appendLine("{}     var _os_ = new java.io.ObjectOutputStream(_bs_)) {", prefix);
			sb.appendLine("{}    _os_.writeObject({});", prefix, varName);
			sb.appendLine("{}    {}.WriteBytes(_bs_.toByteArray());", prefix, bbName);
			sb.appendLine("{}} catch (java.io.IOException _e_) {", prefix);
			sb.appendLine("{}    throw new RuntimeException(_e_);", prefix);
			sb.appendLine("{}}", prefix);
		}
	}

	void genDecode(StringBuilderCs sb, String prefix, String bbName, Class<?> type, Type paramType, String varName) throws Throwable {
		var kn = serializer.get(type);
		if (kn != null)
			kn.decoder.run(sb, prefix, varName, bbName);
		else if (Serializable.class.isAssignableFrom(type))
			sb.appendLine("{}{}.decode({});", prefix, varName, bbName);
		else {
			sb.appendLine("{}{", prefix);
			sb.appendLine("{}    var _bo_ = {}.ReadByteBuffer();", prefix, bbName);
			sb.appendLine("{}    try (var _bs_ = new java.io.ByteArrayInputStream(_bo_.Bytes, _bo_.ReadIndex, _bo_.Size());", prefix);
			sb.appendLine("{}         var _os_ = new java.io.ObjectInputStream(_bs_)) {", prefix);
			if (type == Object.class)
				sb.appendLine("{}        {} = Zeze.Util.Reflect.cast(_os_.readObject());", prefix, varName);
			else
				sb.appendLine("{}        {} = ({})_os_.readObject();", prefix, varName, getTypeName(paramType));
			sb.appendLine("{}    } catch (java.io.IOException _e_) {", prefix);
			sb.appendLine("{}        throw new RuntimeException(_e_);", prefix);
			sb.appendLine("{}    }", prefix);
			sb.appendLine("{}}", prefix);
		}
	}

	@SuppressWarnings("SameParameterValue")
	void genEncode(StringBuilderCs sb, String prefix, String bbName, List<Parameter> parameters) throws Throwable {
		for (Parameter p : parameters)
			genEncode(sb, prefix, bbName, p.getType(), p.getName());
	}

	@SuppressWarnings("SameParameterValue")
	void genDecode(StringBuilderCs sb, String prefix, String bbName, List<Parameter> parameters) throws Throwable {
		for (Parameter p : parameters)
			genDecode(sb, prefix, bbName, p.getType(), p.getParameterizedType(), p.getName());
	}
}
