package UnitTest.Zeze.Net;

import Zeze.Net.*;
import Zeze.Serialize.*;
import junit.framework.TestCase;
import org.junit.Assert;
import java.util.*;

public class TestCodec extends TestCase{
	
	public final void testEncrypt() {
		BufferCodec b2flush = new BufferCodec();
		byte[] key = {1}; 
		{
			Encrypt en = new Encrypt(b2flush, key);
			en.update((byte)1);
			en.flush();
			en.update((byte)2);
			en.flush();
		}
		BufferCodec b1flush = new BufferCodec(); 
		{
			Encrypt en = new Encrypt(b1flush, key);
			en.update((byte)1);
			en.update((byte)2);
			en.flush();
		}
		Assert.assertEquals(b2flush.getBuffer(), b1flush.getBuffer());

		BufferCodec bdecrypt = new BufferCodec(); {
			Decrypt de = new Decrypt(bdecrypt, key);
			de.update(b2flush.getBuffer().Bytes, b2flush.getBuffer().ReadIndex, b2flush.getBuffer().Size());
			de.flush();
		}
		Assert.assertEquals(2, bdecrypt.getBuffer().Size());
		Assert.assertEquals(1, bdecrypt.getBuffer().Bytes[0]);
		Assert.assertEquals(2, bdecrypt.getBuffer().Bytes[1]);
	}

	public final void testEncrypt2() {
		Random rand = new Random();

		byte[] key = {1, 2, 3, 4, 5};

		int[] sizes = new int[1000];
		for (int i = 0; i < sizes.length; ++i) {
			sizes[i] = rand.nextInt(10 * 1024);
		}
		for (int size : sizes) {
			byte[] buffer = new byte[size];
			rand.nextBytes(buffer);

			BufferCodec encrypt = new BufferCodec();
			Encrypt en = new Encrypt(encrypt, key);
			en.update(buffer, 0, buffer.length);
			en.flush();

			BufferCodec decrypt = new BufferCodec();
			Decrypt de = new Decrypt(decrypt, key);
			de.update(encrypt.getBuffer().Bytes, encrypt.getBuffer().ReadIndex, encrypt.getBuffer().Size());
			de.flush();

			Assert.assertEquals(ByteBuffer.Wrap(buffer), decrypt.getBuffer());
		}
	}

	public final void testCompress() {
		Random rand = new Random();
		int[] sizes = new int[1000];
		for (int i = 0; i < sizes.length; ++i) {
			sizes[i] = rand.nextInt(10 * 1024);
		}
		for (int size : sizes) {
			BufferCodec bufcp = new BufferCodec();
			Compress cp = new Compress(bufcp);
			byte[] buffer = new byte[size];
			rand.nextBytes(buffer);
			cp.update(buffer, 0, buffer.length);
			cp.flush();

			BufferCodec bufdp = new BufferCodec();
			Decompress dp = new Decompress(bufdp);
			dp.update(bufcp.getBuffer().Bytes, bufcp.getBuffer().ReadIndex, bufcp.getBuffer().Size());
			dp.flush();
			Assert.assertEquals(ByteBuffer.Wrap(buffer), bufdp.getBuffer());
		}
	}
}