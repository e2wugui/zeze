package Zeze.Arch.Gen;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Net.Binary;

public class Gen {
	public static final Gen Instance = new Gen();

	private final HashMap<Class<?>, KnownSerializer> Serializer = new HashMap<>();
	public final AtomicLong TmpVarNameId = new AtomicLong();

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
				(sb, prefix, varName) -> sb.AppendLine("{}Binary {};", prefix, varName),
				() -> "Binary")
		);
		Serializer.put(Zeze.Serialize.ByteBuffer.class, new KnownSerializer(
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{}.WriteByteBuffer({});", prefix, bbName, varName),
				(sb, prefix, varName, bbName) -> sb.AppendLine("{}{} = Zeze.Serialize.ByteBuffer.Wrap({}.ReadBytes());", prefix, varName, bbName),
				(sb, prefix, varName) -> sb.AppendLine("{}Zeze.Serialize.ByteBuffer {};", prefix, varName),
				() -> "Zeze.Serialize.ByteBuffer")
		);
	}

	public String GetTypeName(Class<?> type) throws Throwable {
		var kn = Serializer.get(type);
		if (kn != null)
			return kn.TypeName.call();

//		if (Zeze.Serialize.Serializable.class.isAssignableFrom(type))
//			return type.getTypeName();

		return type.getTypeName().replace('$', '.');
	}

	public void GenLocalVariable(Zeze.Util.StringBuilderCs sb, String prefix, Class<?> type, String varName) throws Throwable {
		var kn = Serializer.get(type);
		if (kn != null) {
			kn.Define.run(sb, prefix, varName);
			return;
		}

		if (Zeze.Serialize.Serializable.class.isAssignableFrom(type))
			sb.AppendLine("{}var {} = new {}();", prefix, /*type.getTypeName(),*/ varName, type.getTypeName());
		else
			sb.AppendLine("{}{} {} = null;", prefix, type.getTypeName(), varName);
	}

	public void GenEncode(Zeze.Util.StringBuilderCs sb, String prefix, String bbName, Class<?> type, String varName) throws Throwable {
		var kn = Serializer.get(type);
		if (kn != null) {
			kn.Encoder.run(sb, prefix, varName, bbName);
			return;
		}

		if (Zeze.Serialize.Serializable.class.isAssignableFrom(type)) {
			sb.AppendLine("{}{}.Encode({});", prefix, varName, bbName);
			return;
		}

		sb.AppendLine("{}try (var output = new java.io.ByteArrayOutputStream();", prefix);
		sb.AppendLine("{}     var objOutput = new java.io.ObjectOutputStream(output)) {", prefix);
		sb.AppendLine("{}    objOutput.writeObject({});", prefix, varName);
		sb.AppendLine("{}    {}.WriteBytes(output.toByteArray());", prefix, bbName);
		sb.AppendLine("{}} catch (Throwable e) {", prefix);
		sb.AppendLine("{}    throw new RuntimeException(e);", prefix);
		sb.AppendLine("{}}", prefix);
	}

	public void GenDecode(Zeze.Util.StringBuilderCs sb, String prefix, String bbName, Class<?> type, String varName) throws Throwable {
		var kn = Serializer.get(type);
		if (kn != null) {
			kn.Decoder.run(sb, prefix, varName, bbName);
			return;
		}

		if (Zeze.Serialize.Serializable.class.isAssignableFrom(type)) {
			sb.AppendLine("{}{}.Decode({});", prefix, varName, bbName);
			return;
		}
		String tmp1 = "tmp" + TmpVarNameId.incrementAndGet();
		String tmp2 = "tmp" + TmpVarNameId.incrementAndGet();
		//String tmp3 = "tmp" + TmpVarNameId.incrementAndGet();
		sb.AppendLine("{}var {} = {}.ReadByteBuffer();", prefix, tmp1, bbName);
		sb.AppendLine("{}try (var {} = new java.io.ByteArrayInputStream({}.Bytes, {}.ReadIndex, {}.Size());", prefix, tmp2, tmp1, tmp1, tmp1);
		sb.AppendLine("{}     var objInput = new java.io.ObjectInputStream({})) {", prefix, tmp2);
		sb.AppendLine("{}    {} = ({})objInput.readObject();", prefix, varName, GetTypeName(type));
		sb.AppendLine("{}} catch (Throwable e) {", prefix);
		sb.AppendLine("{}    throw new RuntimeException(e);", prefix);
		sb.AppendLine("{}}", prefix);
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
