package UnitTest.Zeze.Net;

import Zeze.Net.*;
import Zeze.Serialize.*;
import UnitTest.*;
import java.util.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestCodec
public class TestCodec {
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestEncrypt()
	public final void TestEncrypt() {
		BufferCodec b2flush = new BufferCodec();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] key = { 1 };
		byte[] key = {1}; {
			Encrypt en = new Encrypt(b2flush, key);
			en.update((byte)1);
			en.flush();
			en.update((byte)2);
			en.flush();
		}
		BufferCodec b1flush = new BufferCodec(); {
			Encrypt en = new Encrypt(b1flush, key);
			en.update((byte)1);
			en.update((byte)2);
			en.flush();
		}
		assert b2flush.Buffer == b1flush.Buffer;

		BufferCodec bdecrypt = new BufferCodec(); {
			Decrypt de = new Decrypt(bdecrypt, key);
			de.update(b2flush.Buffer.Bytes, b2flush.Buffer.ReadIndex, b2flush.Buffer.Size);
			de.flush();
		}
		assert 2 == bdecrypt.Buffer.Size;
		assert 1 == bdecrypt.Buffer.Bytes[0];
		assert 2 == bdecrypt.Buffer.Bytes[1];
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestEncrypt2()
	public final void TestEncrypt2() {
		Random rand = new Random();

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] key = { 1,2,3,4,5 };
		byte[] key = {1, 2, 3, 4, 5};

		int[] sizes = new int[1000];
		for (int i = 0; i < sizes.length; ++i) {
			sizes[i] = rand.nextInt(10 * 1024);
		}
		for (int size : sizes) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] buffer = new byte[size];
			byte[] buffer = new byte[size];
			rand.nextBytes(buffer);

			BufferCodec encrypt = new BufferCodec();
			Encrypt en = new Encrypt(encrypt, key);
			en.update(buffer, 0, buffer.length);
			en.flush();

			BufferCodec decrypt = new BufferCodec();
			Decrypt de = new Decrypt(decrypt, key);
			de.update(encrypt.Buffer.Bytes, encrypt.Buffer.ReadIndex, encrypt.Buffer.Size);
			de.flush();

			assert ByteBuffer.Wrap(buffer) == decrypt.Buffer;
		}
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestCompress()
	public final void TestCompress() {
		Random rand = new Random();
		int[] sizes = new int[1000];
		for (int i = 0; i < sizes.length; ++i) {
			sizes[i] = rand.nextInt(10 * 1024);
		}
		for (int size : sizes) {
			BufferCodec bufcp = new BufferCodec();
			Compress cp = new Compress(bufcp);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] buffer = new byte[size];
			byte[] buffer = new byte[size];
			rand.nextBytes(buffer);
			cp.update(buffer, 0, buffer.length);
			cp.flush();

			BufferCodec bufdp = new BufferCodec();
			Decompress dp = new Decompress(bufdp);
			dp.update(bufcp.Buffer.Bytes, bufcp.Buffer.ReadIndex, bufcp.Buffer.Size);
			dp.flush();

			assert ByteBuffer.Wrap(buffer) == bufdp.Buffer;
		}
	}
}