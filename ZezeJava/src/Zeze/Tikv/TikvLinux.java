package Zeze.Tikv;

import Zeze.*;
import java.io.*;

public final class TikvLinux extends Tikv {
	private static native long NewClient(GoString pdAddrs, GoSlice outerr);
	static {
		System.loadLibrary("tikv.so");
	}
	private static native long CloseClient(long clientId, GoSlice outerr);
	private static native long Begin(long clientId, GoSlice outerr);
	private static native long Commit(long txnId, GoSlice outerr);
	private static native long Rollback(long txnId, GoSlice outerr);
	private static native long Put(long txnId, GoSlice key, GoSlice value, GoSlice outerr);
	private static native long Get(long txnId, GoSlice key, GoSlice outvalue, GoSlice outerr);
	private static native long Delete(long txnId, GoSlice key, GoSlice outerr);
	private static native long Scan(long txnId, GoSlice keyprefix, Walker walker, GoSlice outerr);
	@FunctionalInterface
	public interface Walker {
		int invoke(IntPtr key, int keylen, IntPtr value, int valuelen);
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public override long Scan(long txnId, Serialize.ByteBuffer keyprefix, Func<byte[], byte[], bool> callback)
	@Override
	public long Scan(long txnId, Serialize.ByteBuffer keyprefix, tangible.Func2Param<byte[], byte[], Boolean> callback) {
		try (var _keyprefix = new GoSlice(keyprefix.getBytes(), keyprefix.getReadIndex(), keyprefix.getSize())) {
			try (var error = new GoSlice(1024)) {
				long rc = Scan(txnId, _keyprefix.clone(), (IntPtr key, int keylen, IntPtr value, int valuelen) -> {
								var _key = new byte[keylen];
								var _value = new byte[valuelen];
								Marshal.Copy(key, _key, 0, _key.length);
								Marshal.Copy(value, _value, 0, _value.length);
								return callback.invoke(_key, _value) ? 0 : -1;
				}, error.clone());
        
				if (rc < 0) {
					throw new RuntimeException(GetErrorString(rc, error.clone()));
				}
        
				return rc;
			}
		}
	}

	@Override
	public long NewClient(String pdAddrs) {
		try (var _pdAddrs = new GoString(pdAddrs)) {
			try (var error = new GoSlice(1024)) {
				long rc = NewClient(_pdAddrs.clone(), error.clone());
				if (rc < 0) {
					throw new RuntimeException(GetErrorString(rc, error.clone()));
				}
				return rc;
			}
		}
	}

	@Override
	public void CloseClient(long clientId) {
		try (var error = new GoSlice(1024)) {
			long rc = CloseClient(clientId, error.clone());
			if (rc < 0) {
				throw new RuntimeException(GetErrorString(rc, error.clone()));
			}
		}
	}

	@Override
	public long Begin(long clientId) {
		try (var error = new GoSlice(1024)) {
			long rc = Begin(clientId, error.clone());
			if (rc < 0) {
				throw new RuntimeException(GetErrorString(rc, error.clone()));
			}
			return rc;
		}
	}

	@Override
	public void Commit(long txnId) {
		try (var error = new GoSlice(1024)) {
			long rc = Commit(txnId, error.clone());
			if (rc < 0) {
				throw new RuntimeException(GetErrorString(rc, error.clone()));
			}
		}
	}

	@Override
	public void Rollback(long txnId) {
		try (var error = new GoSlice(1024)) {
			long rc = Rollback(txnId, error.clone());
			if (rc < 0) {
				throw new RuntimeException(GetErrorString(rc, error.clone()));
			}
		}
	}

	@Override
	public void Put(long txnId, Serialize.ByteBuffer key, Serialize.ByteBuffer value) {
		try (var _key = new GoSlice(key.getBytes(), key.getReadIndex(), key.getSize())) {
			try (var _value = new GoSlice(value.getBytes(), value.getReadIndex(), value.getSize())) {
				try (var error = new GoSlice(1024)) {
					long rc = Put(txnId, _key.clone(), _value.clone(), error.clone());
					if (rc < 0) {
						throw new RuntimeException(GetErrorString(rc, error.clone()));
					}
				}
			}
		}
	}

	@Override
	public Serialize.ByteBuffer Get(long txnId, Serialize.ByteBuffer key) {
		int outValueBufferLen = 64 * 1024;
		while (true) {
			try (var _key = new GoSlice(key.getBytes(), key.getReadIndex(), key.getSize())) {
				try (var error = new GoSlice(1024)) {
					try (var outvalue = new GoSlice(outValueBufferLen)) {
						long rc = Get(txnId, _key.clone(), outvalue.clone(), error.clone());
						if (rc < 0) {
							var str = GetErrorString(rc, error.clone());
							if (str.equals("key not exist")) { // 这是tikv clieng.go 返回的错误。
								return null;
							}
							if (str.equals("ZezeSpecialError: value is nil.")) {
								return null;
							}
							var strBufferNotEnough = "ZezeSpecialError: outvalue buffer not enough. BufferNeed=";
							if (str.startsWith(strBufferNotEnough)) {
								outValueBufferLen = Integer.parseInt(str.substring(strBufferNotEnough.length()));
								continue;
							}
							throw new RuntimeException(str);
						}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] rcvalue = new byte[rc];
						byte[] rcvalue = new byte[rc];
						Marshal.Copy(outvalue.getData(), rcvalue, 0, rcvalue.length);
						return Serialize.ByteBuffer.Wrap(rcvalue);
					}
				}
			}
		}
	}

	@Override
	public void Delete(long txnId, Serialize.ByteBuffer key) {
		try (var _key = new GoSlice(key.getBytes(), key.getReadIndex(), key.getSize())) {
			try (var error = new GoSlice(1024)) {
				long rc = Delete(txnId, _key.clone(), error.clone());
				if (rc < 0) {
					throw new RuntimeException(GetErrorString(rc, error.clone()));
				}
			}
		}
	}
}