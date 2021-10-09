package Zeze.Net;

import Zeze.Serialize.*;
import Zeze.*;
import java.io.*;

public final class Digest {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public static byte[] Md5(byte[] message)
	public static byte[] Md5(byte[] message) {
		try (var md = MD5.Create()) {
			return md.ComputeHash(message);
		}
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public static byte[] HmacMd5(byte[] key, byte[] data, int offset, int length)
	public static byte[] HmacMd5(byte[] key, byte[] data, int offset, int length) {
		try (HashAlgorithm hash = new HMACMD5(key)) {
			hash.TransformFinalBlock(data, offset, length);
			return hash.Hash;
		}
	}
}