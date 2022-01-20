using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Net;
using Zeze.Serialize;

namespace UnitTest.Zeze.Net
{
    [TestClass]
    public class TestCodec
    {
        [TestMethod]
        public void TestEncrypt()
        {
            BufferCodec b2flush = new BufferCodec();
            byte[] key = { 1 };
            {
                Encrypt en = new Encrypt(b2flush, key);
                en.update(1);
                en.flush();
                en.update(2);
                en.flush();
            }
            BufferCodec b1flush = new BufferCodec();
            {
                Encrypt en = new Encrypt(b1flush, key);
                en.update(1);
                en.update(2);
                en.flush();
            }
            Assert.AreEqual(b2flush.Buffer, b1flush.Buffer);

            BufferCodec bdecrypt = new BufferCodec();
            {
                Decrypt de = new Decrypt(bdecrypt, key);
                de.update(b2flush.Buffer.Bytes, b2flush.Buffer.ReadIndex, b2flush.Buffer.Size);
                de.flush();
            }
            Assert.AreEqual(2, bdecrypt.Buffer.Size);
            Assert.AreEqual(1, bdecrypt.Buffer.Bytes[0]);
            Assert.AreEqual(2, bdecrypt.Buffer.Bytes[1]);
        }

        [TestMethod]
        public void TestEncrypt2()
        {
            Random rand = new Random();

            byte[] key = { 1,2,3,4,5 };

            int[] sizes = new int[1000];
            for (int i = 0; i < sizes.Length; ++i)
            {
                sizes[i] = rand.Next(10 * 1024);
            }
            foreach (int size in sizes)
            {
                byte[] buffer = new byte[size];
                rand.NextBytes(buffer);

                BufferCodec encrypt = new BufferCodec();
                Encrypt en = new Encrypt(encrypt, key);
                en.update(buffer, 0, buffer.Length);
                en.flush();

                if (buffer.Length > 0)
                    Assert.AreNotEqual(ByteBuffer.Wrap(buffer), encrypt.Buffer);
                else
                    Assert.AreEqual(ByteBuffer.Wrap(buffer), encrypt.Buffer);

                BufferCodec decrypt = new BufferCodec();
                Decrypt de = new Decrypt(decrypt, key);
                de.update(encrypt.Buffer.Bytes, encrypt.Buffer.ReadIndex, encrypt.Buffer.Size);
                de.flush();

                Assert.AreEqual(ByteBuffer.Wrap(buffer), decrypt.Buffer);
            }
        }

        [TestMethod]
        public void TestCompress()
        {
            Random rand = new Random(); 
            int[] sizes = new int[1000];
            for (int i = 0; i < sizes.Length; ++i)
            {
                sizes[i] = rand.Next(10 * 1024);
            }
            foreach (int size in sizes)
            {
                BufferCodec bufcp = new BufferCodec();
                Compress cp = new Compress(bufcp);
                byte[] buffer = new byte[size];
                rand.NextBytes(buffer);
                cp.update(buffer, 0, buffer.Length);
                cp.flush();

                BufferCodec bufdp = new BufferCodec();
                Decompress dp = new Decompress(bufdp);
                dp.update(bufcp.Buffer.Bytes, bufcp.Buffer.ReadIndex, bufcp.Buffer.Size);
                dp.flush();

                Assert.AreEqual(ByteBuffer.Wrap(buffer), bufdp.Buffer);
            }
        }
    }
}
