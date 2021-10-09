package Zeze.Serialize;

import Zeze.*;

public final class SerializeHelper<T> extends SerializeHelper {
	private static tangible.Func1Param<ByteBuffer, T> Decode = Zeze.Serialize.SerializeHelper.<T>CreateDecodeFunc ();
	public static tangible.Func1Param<ByteBuffer, T> getDecode() {
		return Decode;
	}
	private static tangible.Action2Param<ByteBuffer, T> Encode = Zeze.Serialize.SerializeHelper.<T>CreateEncodeFunc ();
	public static tangible.Action2Param<ByteBuffer, T> getEncode() {
		return Encode;
	}
}