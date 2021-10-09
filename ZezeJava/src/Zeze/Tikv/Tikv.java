package Zeze.Tikv;

import Zeze.*;
import java.io.*;

public abstract class Tikv {
	public static final Tikv Driver = Create();

	private static Tikv Create() {
		if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux)) {
			return new TikvLinux();
		}
		if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows)) {
			return new TikvWindows();
		}
		/*
		if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
		    return TikvLinux();
		*/
		throw new RuntimeException("unknown platform.");
	}

	public abstract long NewClient(String pdAddrs);
	public abstract void CloseClient(long clientId);
	public abstract long Begin(long clientId);
	public abstract void Commit(long txnId);
	public abstract void Rollback(long txnId);
	public abstract void Put(long txnId, Serialize.ByteBuffer key, Serialize.ByteBuffer value);
	public abstract Serialize.ByteBuffer Get(long txnId, Serialize.ByteBuffer key);
	public abstract void Delete(long txnId, Serialize.ByteBuffer key);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public abstract long Scan(long txnId, Serialize.ByteBuffer keyprefix, Func<byte[], byte[], bool> callback);
	public abstract long Scan(long txnId, Serialize.ByteBuffer keyprefix, tangible.Func2Param<byte[], byte[], Boolean> callback);

	protected final String GetErrorString(long rc, GoSlice outerr) {
		if (rc >= 0) {
			return "";
		}
		int len = (int)-rc;
		return Marshal.PtrToStringUTF8(outerr.getData(), len);
	}
}