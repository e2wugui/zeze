package Zeze.Arch.Gen;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Net.Binary;
import Zeze.Util.Str;

public class Gen {
	private final HashMap<Class<?>, KnownSerializer> Serializer = new HashMap<>();
	public AtomicLong TmpVarNameId = new AtomicLong();

	public static Gen Instance = new Gen();

	private Gen() {
		Serializer.put(Binary.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteBinary({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadBinary();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}Binary {};", prefix, varName)),
				() -> "Binary")
		);
		Serializer.put(Boolean.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteBool({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadBool();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}bool {};", prefix, varName)),
				() -> "boolean")
		);
		Serializer.put(boolean.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteBool({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadBool();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}bool {};", prefix, varName)),
				() -> "boolean")
		);
		Serializer.put(Byte.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteByte({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadByte();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}byte {1};", prefix, varName)),
				() -> "byte")
		);
		Serializer.put(byte.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteByte({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadByte();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}byte {};", prefix, varName)),
				() -> "byte")
		);
		Serializer.put(Zeze.Serialize.ByteBuffer.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteByteBuffer({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = Zeze.Serialize.ByteBuffer.Wrap({}.ReadBytes());", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}Zeze.Serialize.ByteBuffer {};", prefix, varName)),
				() -> "Zeze.Serialize.ByteBuffer")
		);
		Serializer.put(byte[].class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteBytes({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadBytes();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}byte[] {};", prefix, varName)),
				() -> "byte[]")
		);
		Serializer.put(Double.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteDouble({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadDouble();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}double {};", prefix, varName)),
				() -> "double")
		);
		Serializer.put(double.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteDouble({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadDouble();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}double {};", prefix, varName)),
				() -> "double")
		);
		Serializer.put(Float.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteFloat({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadFloat();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}float {};", prefix, varName)),
				() -> "float")
		);
		Serializer.put(float.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteFloat({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadFloat();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}float {};", prefix, varName)),
				() -> "float")
		);
		Serializer.put(Integer.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteInt({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadInt();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}int {};", prefix, varName)),
				() -> "int")
		);
		Serializer.put(int.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteInt({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadInt();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}int {};", prefix, varName)),
				() -> "int")
		);
		Serializer.put(Long.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteLong({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadLong();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}long {};", prefix, varName)),
				() -> "long")
		);
		Serializer.put(long.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteLong({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadLong();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}long {};", prefix, varName)),
				() -> "long")
		);
		Serializer.put(Short.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteShort({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadShort();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}short {};", prefix, varName)),
				() -> "short")
		);
		Serializer.put(short.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteShort({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadShort();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}short {};", prefix, varName)),
				() -> "short")
		);
		Serializer.put(String.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{}.WriteString({});", prefix, bbName, varName)),
				(sb, prefix, varName, bbName) -> sb.AppendLine(Str.format("{}{} = {}.ReadString();", prefix, varName, bbName)),
				(sb, prefix, varName) -> sb.AppendLine(Str.format("{}string {};", prefix, varName)),
				() -> "String")
		);
	}

	public String GetTypeName(Class<?> type) throws Throwable {
		var kn = Serializer.get(type);
		if (null != kn)
			return kn.TypeName.call();

//		if (Zeze.Serialize.Serializable.class.isAssignableFrom(type))
//			return type.getTypeName();

		return type.getTypeName();
	}

	public void GenLocalVariable(Zeze.Util.StringBuilderCs sb, String prefix, Class<?> type, String varName) throws Throwable {
		var kn = Serializer.get(type);
		if (null != kn) {
			kn.Define.run(sb, prefix, varName);
			return;
		}

		if (Zeze.Serialize.Serializable.class.isAssignableFrom(type)) {
			sb.AppendLine(Str.format("{}{} {} = new {}();",
					prefix, type.getTypeName(), varName, type.getTypeName()));
			return;
		}

		sb.AppendLine(Str.format("{}{} {} = null;", prefix, type.getTypeName(), varName));
	}

	public void GenEncode(Zeze.Util.StringBuilderCs sb, String prefix, String bbName, Class<?> type, String varName) throws Throwable {
		var kn = Serializer.get(type);
		if (null != kn) {
			kn.Encoder.run(sb, prefix, varName, bbName);
			return;
		}

		if (Zeze.Serialize.Serializable.class.isAssignableFrom(type)) {
			sb.AppendLine(Str.format("{}{}.Encode({});", prefix, varName, bbName));
			return;
		}

		sb.AppendLine(Str.format("{}try {", prefix));
		sb.AppendLine(Str.format("{}    try (var output = new java.io.ByteArrayOutputStream(); var objOutput = new java.io.ObjectOutputStream(output))", prefix));
		sb.AppendLine(Str.format("{}    {", prefix));
		sb.AppendLine(Str.format("{}        objOutput.writeObject({});", prefix, varName));
		sb.AppendLine(Str.format("{}        {}.WriteBytes(output.toByteArray());", prefix, bbName));
		sb.AppendLine(Str.format("{}	   }", prefix));
		sb.AppendLine(Str.format("{}} catch (Throwable e) {", prefix));
		sb.AppendLine(Str.format("{}    throw new RuntimeException(e);", prefix));
		sb.AppendLine(Str.format("{}}", prefix));
	}

	public void GenDecode(Zeze.Util.StringBuilderCs sb, String prefix, String bbName, Class<?> type, String varName) throws Throwable {
		var kn = Serializer.get(type);
		if (null != kn) {
			kn.Decoder.run(sb, prefix, varName, bbName);
			return;
		}

		if (Zeze.Serialize.Serializable.class.isAssignableFrom(type)) {
			sb.AppendLine(Str.format("{}{}.Decode({});", prefix, varName, bbName));
			return;
		}
		String tmp1 = "tmp" + TmpVarNameId.incrementAndGet();
		String tmp2 = "tmp" + TmpVarNameId.incrementAndGet();
		//String tmp3 = "tmp" + TmpVarNameId.incrementAndGet();
		sb.AppendLine(Str.format("{}try {", prefix));
		sb.AppendLine(Str.format("{}    var {} = {}.ReadByteBuffer();", prefix, tmp1, bbName));
		sb.AppendLine(Str.format("{}    try (var {} = new java.io.ByteArrayInputStream({}.Bytes, {}.ReadIndex, {}.Size()); var objInput = new java.io.ObjectInputStream({})) {",
				prefix, tmp2, tmp1, tmp1, tmp1, tmp2));
		sb.AppendLine(Str.format("{}        {} = ({})objInput.readObject();", prefix, varName, GetTypeName(type)));
		sb.AppendLine(Str.format("{}    }", prefix));
		sb.AppendLine(Str.format("{}} catch (Throwable e) {", prefix));
		sb.AppendLine(Str.format("{}    throw new RuntimeException(e);", prefix));
		sb.AppendLine(Str.format("{}}", prefix));
	}

	private boolean IsOut(@SuppressWarnings("unused") Class<?> type) {
		return false;
		// return type == Zeze.Util.OutObject.class;
	}

	@SuppressWarnings("unused")
	private boolean IsRef(Class<?> type) {
		return false;
		//return type == Zeze.Util.RefObject.class;
	}

	public void GenEncode(Zeze.Util.StringBuilderCs sb, String prefix, String bbName, List<Parameter> parameters) throws Throwable {
		for (Parameter p : parameters) {
			if (IsOut(p.getType()))
				continue;
			if (IsKnownDelegate(p.getType()))
				continue;
			GenEncode(sb, prefix, bbName, p.getType(), p.getName());
		}
	}

	public static boolean IsKnownDelegate(Class<?> type) {
		if (type.getAnnotation(FunctionalInterface.class) != null) {
			if (type.getName().startsWith("Zeze.Util.Action"))
				return true;
			if (type == Zeze.Arch.RedirectAllDoneHandle.class)
				return true;
			throw new RuntimeException("ModuleRedirect Callback Only Support Zeze.Util.ActionN");
		}
		return false;
	}

	public void GenDecode(Zeze.Util.StringBuilderCs sb, String prefix, String bbName, List<java.lang.reflect.Parameter> parameters) throws Throwable {
		for (Parameter p : parameters) {
			if (IsOut(p.getType()))
				continue;
			if (IsKnownDelegate(p.getType()))
				continue;
			GenDecode(sb, prefix, bbName, p.getType(), p.getName());
		}
	}
}
