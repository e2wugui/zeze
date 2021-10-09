package Zeze.Tikv;

import Zeze.*;
import java.io.*;

//C# TO JAVA CONVERTER WARNING: Java does not allow user-defined value types. The behavior of this class may differ from the original:
//ORIGINAL LINE: public struct GoSlice : IDisposable
public final class GoSlice implements Closeable {
	private IntPtr Data = System.IntPtr.Zero;
	public IntPtr getData() {
		return Data;
	}
	public void setData(IntPtr value) {
		Data = value;
	}
	private long Len;
	public long getLen() {
		return Len;
	}
	public void setLen(long value) {
		Len = value;
	}
	private long Cap;
	public long getCap() {
		return Cap;
	}
	public void setCap(long value) {
		Cap = value;
	}

	public GoSlice() {
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public GoSlice(byte [] bytes, int offset, int size)
	public GoSlice(byte[] bytes, int offset, int size) {
		setLen(size);
		setCap(size);
		setData(Marshal.AllocHGlobal(size));
		Marshal.Copy(bytes, offset, getData(), size);
	}

	public GoSlice(int allcateOnly) {
		setLen(allcateOnly); // 如果是0，传入go时是空的。本来还以为cap此时能被用上。
		setCap(allcateOnly);
		setData(Marshal.AllocHGlobal(allcateOnly));
	}

	public void Dispose() {
		Marshal.FreeHGlobal(getData());
	}
}