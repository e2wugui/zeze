package Zeze.Tikv;

import Zeze.*;
import java.io.*;

//C# TO JAVA CONVERTER WARNING: Java does not allow user-defined value types. The behavior of this class may differ from the original:
//ORIGINAL LINE: public struct GoString : IDisposable
public final class GoString implements Closeable {
	private IntPtr Str = System.IntPtr.Zero;
	public IntPtr getStr() {
		return Str;
	}
	public void setStr(IntPtr value) {
		Str = value;
	}
	private long Len;
	public long getLen() {
		return Len;
	}
	public void setLen(long value) {
		Len = value;
	}

	public GoString() {
	}

	public GoString(String str) {
		var utf8 = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		setLen(utf8.Length);
		setStr(Marshal.AllocHGlobal(utf8.Length));
		Marshal.Copy(utf8, 0, getStr(), utf8.Length);
	}

	public void Dispose() {
		Marshal.FreeHGlobal(getStr());
	}
}