package Zeze.Arch.Gen;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.StringBuilderCs;

class Gen {
	static final Gen Instance = new Gen();

	private static class KnownSerializer {
		final Zeze.Util.Action4<Zeze.Util.StringBuilderCs, String, String, String> Encoder;
		final Zeze.Util.Action4<Zeze.Util.StringBuilderCs, String, String, String> Decoder;
		final Zeze.Util.Action3<Zeze.Util.StringBuilderCs, String, String> Define;
		final Zeze.Util.Func0<String> TypeName;

		KnownSerializer(Zeze.Util.Action4<Zeze.Util.StringBuilderCs, String, String, String> enc,
						Zeze.Util.Action4<Zeze.Util.StringBuilderCs, String, String, String> dec,
						Zeze.Util.Action3<Zeze.Util.StringBuilderCs, String, String> def,
						Zeze.Util.Func0<String> typeName) {
			Encoder = enc;
			Decoder = dec;
			Define = def;
			TypeName = typeName;
		}
	}

	private final HashMap<Class<?>, KnownSerializer> Serializer = new HashMap<>();

	private Gen() {
		Serializer.put(Boolean.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteBool({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = {}.ReadBool();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}bool {};", prefix, varName),
				() -> "boolean")
		);
		Serializer.put(boolean.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteBool({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = {}.ReadBool();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}bool {};", prefix, varName),
				() -> "boolean")
		);
		Serializer.put(Byte.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = (byte){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}byte {1};", prefix, varName),
				() -> "byte")
		);
		Serializer.put(byte.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = (byte){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}byte {};", prefix, varName),
				() -> "byte")
		);
		Serializer.put(Short.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = (short){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}short {};", prefix, varName),
				() -> "short")
		);
		Serializer.put(short.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = (short){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}short {};", prefix, varName),
				() -> "short")
		);
		Serializer.put(Integer.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = (int){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}int {};", prefix, varName),
				() -> "int")
		);
		Serializer.put(int.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = (int){}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}int {};", prefix, varName),
				() -> "int")
		);
		Serializer.put(Long.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = {}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}long {};", prefix, varName),
				() -> "long")
		);
		Serializer.put(long.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteLong({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = {}.ReadLong();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}long {};", prefix, varName),
				() -> "long")
		);
		Serializer.put(Float.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteFloat({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = {}.ReadFloat();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}float {};", prefix, varName),
				() -> "float")
		);
		Serializer.put(float.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteFloat({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = {}.ReadFloat();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}float {};", prefix, varName),
				() -> "float")
		);
		Serializer.put(Double.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteDouble({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = {}.ReadDouble();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}double {};", prefix, varName),
				() -> "double")
		);
		Serializer.put(double.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteDouble({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = {}.ReadDouble();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}double {};", prefix, varName),
				() -> "double")
		);
		Serializer.put(String.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteString({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = {}.ReadString();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}String {};", prefix, varName),
				() -> "String")
		);
		Serializer.put(byte[].class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteBytes({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = {}.ReadBytes();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}byte[] {};", prefix, varName),
				() -> "byte[]")
		);
		Serializer.put(Binary.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteBinary({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = {}.ReadBinary();", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}Zeze.Net.Binary {};", prefix, varName),
				() -> "Zeze.Net.Binary")
		);
		Serializer.put(ByteBuffer.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteByteBuffer({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = Zeze.Serialize.ByteBuffer.Wrap({}.ReadBytes());", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}Zeze.Serialize.ByteBuffer {};", prefix, varName),
				() -> "Zeze.Serialize.ByteBuffer")
		);
	}

	String GetTypeName(Class<?> type) throws Throwable {
		var kn = Serializer.get(type);
		if (kn != null)
			return kn.TypeName.call();
		return type.getTypeName().replace('$', '.');
	}

	@SuppressWarnings("SameParameterValue")
	void GenLocalVariable(StringBuilderCs sb, String prefix, Class<?> type, String varName) throws Throwable {
		var kn = Serializer.get(type);
		if (kn != null)
			kn.Define.run(sb, prefix, varName);
		else if (Serializable.class.isAssignableFrom(type))
			sb.AppendLine("{}var {} = new {}();", prefix, varName, type.getTypeName());
		else
			sb.AppendLine("{}{} {} = null;", prefix, type.getTypeName(), varName);
	}

	void GenEncode(StringBuilderCs sb, String prefix, String bbName, Class<?> type, String varName) throws Throwable {
		var kn = Serializer.get(type);
		if (kn != null) {
			kn.Encoder.run(sb, prefix, varName, bbName);
			return;
		}

		if (Serializable.class.isAssignableFrom(type)) {
			sb.AppendLine("{}{}.Encode({});", prefix, varName, bbName);
			return;
		}

		sb.AppendLine("{}try (var _bs_ = new java.io.ByteArrayOutputStream();", prefix);
		sb.AppendLine("{}     var _os_ = new java.io.ObjectOutputStream(_bs_)) {", prefix);
		sb.AppendLine("{}    _os_.writeObject({});", prefix, varName);
		sb.AppendLine("{}    {}.WriteBytes(_bs_.toByteArray());", prefix, bbName);
		sb.AppendLine("{}} catch (java.io.IOException _e_) {", prefix);
		sb.AppendLine("{}    throw new RuntimeException(_e_);", prefix);
		sb.AppendLine("{}}", prefix);
	}

	void GenDecode(StringBuilderCs sb, String prefix, String bbName, Class<?> type, String varName) throws Throwable {
		var kn = Serializer.get(type);
		if (kn != null) {
			kn.Decoder.run(sb, prefix, varName, bbName);
			return;
		}

		if (Serializable.class.isAssignableFrom(type)) {
			sb.AppendLine("{}{}.Decode({});", prefix, varName, bbName);
			return;
		}
		sb.AppendLine("{}{", prefix);
		sb.AppendLine("{}    var _bo_ = {}.ReadByteBuffer();", prefix, bbName);
		sb.AppendLine("{}    try (var _bs_ = new java.io.ByteArrayInputStream(_bo_.Bytes, _bo_.ReadIndex, _bo_.Size());", prefix);
		sb.AppendLine("{}         var _os_ = new java.io.ObjectInputStream(_bs_)) {", prefix);
		sb.AppendLine("{}        {} = ({})_os_.readObject();", prefix, varName, GetTypeName(type));
		sb.AppendLine("{}    } catch (java.io.IOException _e_) {", prefix);
		sb.AppendLine("{}        throw new RuntimeException(_e_);", prefix);
		sb.AppendLine("{}    }", prefix);
		sb.AppendLine("{}}", prefix);
	}

	static boolean IsKnownDelegate(Class<?> type) {
		if (type.getAnnotation(FunctionalInterface.class) != null) {
			if (type.getName().startsWith("Zeze.Util.Action"))
				return true;
			throw new RuntimeException("ModuleRedirect Callback Only Support Zeze.Util.ActionN");
		}
		return false;
	}

	@SuppressWarnings("SameParameterValue")
	void GenEncode(StringBuilderCs sb, String prefix, String bbName, List<Parameter> parameters) throws Throwable {
		for (Parameter p : parameters) {
			if (!IsKnownDelegate(p.getType()))
				GenEncode(sb, prefix, bbName, p.getType(), p.getName());
		}
	}

	@SuppressWarnings("SameParameterValue")
	void GenDecode(StringBuilderCs sb, String prefix, String bbName, List<Parameter> parameters) throws Throwable {
		for (Parameter p : parameters) {
			if (!IsKnownDelegate(p.getType()))
				GenDecode(sb, prefix, bbName, p.getType(), p.getName());
		}
	}
}
