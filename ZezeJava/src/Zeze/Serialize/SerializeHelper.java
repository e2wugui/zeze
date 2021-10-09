package Zeze.Serialize;

import Zeze.*;

public class SerializeHelper {
	public static <T> tangible.Func1Param<ByteBuffer, T> CreateDecodeFunc() {
		var type = T.class;

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: if (type == typeof(byte))
		if (type == Byte.class) {
			return (tangible.Func1Param<ByteBuffer, T>)(Delegate)((ByteBuffer arg) -> bb.ReadByte());
		}

		if (type == Short.class) {
			return (tangible.Func1Param<ByteBuffer, T>)(Delegate)((ByteBuffer arg) -> bb.ReadShort());
		}

		if (type == Integer.class) {
			return (tangible.Func1Param<ByteBuffer, T>)(Delegate)((ByteBuffer arg) -> bb.ReadInt());
		}

		if (type == Long.class) {
			return (tangible.Func1Param<ByteBuffer, T>)(Delegate)((ByteBuffer arg) -> bb.ReadLong());
		}

		if (type == String.class) {
			return (tangible.Func1Param<ByteBuffer, T>)(Delegate)((ByteBuffer arg) -> bb.ReadString());
		}

		if (type == Zeze.Net.Binary.class) {
			return (tangible.Func1Param<ByteBuffer, T>)(Delegate)((ByteBuffer arg) -> bb.ReadBinary());
		}

		if (type.IsAssignableTo(Serializable.class)) {
			return (tangible.Func1Param<ByteBuffer, T>)Delegate.CreateDelegate(tangible.Func1Param<ByteBuffer, T>.class, SerializeHelper.class.GetMethod("CreateSerialiableDecodeFunc").MakeGenericMethod(type));
		}

		return null;
	}

//C# TO JAVA CONVERTER TODO TASK: The C# 'new()' constraint has no equivalent in Java:
//ORIGINAL LINE: public static T CreateSerialiableDecodeFunc<T>(ByteBuffer buf) where T : Serializable, new()
	public static <T extends Serializable> T CreateSerialiableDecodeFunc(ByteBuffer buf) {
		var value = new T();
		value.Decode(buf);
		return value;
	}

	public static <T> tangible.Action2Param<ByteBuffer, T> CreateEncodeFunc() {
		var type = T.class;

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: if (type == typeof(byte))
		if (type == Byte.class) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return (Action<ByteBuffer, T>)(Delegate)(new Action<ByteBuffer, byte>((ByteBuffer buf, byte x) => buf.WriteByte(x)));
			return (tangible.Action2Param<ByteBuffer, T>)(Delegate)((ByteBuffer arg1, byte arg2) -> buf.WriteByte(x));
		}

		if (type == Short.class) {
			return (tangible.Action2Param<ByteBuffer, T>)(Delegate)((ByteBuffer arg1, short arg2) -> buf.WriteShort(x));
		}

		if (type == Integer.class) {
			return (tangible.Action2Param<ByteBuffer, T>)(Delegate)((ByteBuffer arg1, int arg2) -> buf.WriteInt(x));
		}

		if (type == Long.class) {
			return (tangible.Action2Param<ByteBuffer, T>)(Delegate)((ByteBuffer arg1, long arg2) -> buf.WriteLong(x));
		}

		if (type == String.class) {
			return (tangible.Action2Param<ByteBuffer, T>)(Delegate)((ByteBuffer arg1, String arg2) -> buf.WriteString(x));
		}

		if (type == Zeze.Net.Binary.class) {
			return (tangible.Action2Param<ByteBuffer, T>)(Delegate)((ByteBuffer arg1, Zeze.Net.Binary arg2) -> buf.WriteBinary(x));
		}

		if (T.class.IsSubclassOf(Serializable.class)) {
			return (ByteBuffer buf, T x) -> (x instanceof Serializable ? (Serializable)x : null).Encode(buf);
		}

		return null;
	}
}