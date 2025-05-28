using Org.BouncyCastle.Asn1.X509;
using Org.BouncyCastle.Asn1.X509.Qualified;
using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.Crypto.Encodings;
using Org.BouncyCastle.Crypto.Engines;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Crypto.Prng;
using Org.BouncyCastle.Math;
using Org.BouncyCastle.Pkcs;
using Org.BouncyCastle.Security;
using Org.BouncyCastle.X509;
using System;
using System.Collections;
using System.Runtime.ConstrainedExecution;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Threading;
using Zeze.Net;

namespace Draft
{
    class Tx<T, E>
    {
        public readonly static string x = typeof(T).FullName + ", " + typeof(E).FullName;
    }
    class A
    { 
    }
    public class Program
    {
        public static void Test2()
        {
            var so = new TcpSocket(new Service("connector"), "127.0.0.1", 9999);
            so.SetOutputSecurityCodec(new byte[]{1}, 1);
            var r = new Random(1234);
            var s = new byte[1024 * 1024 * 2];
            for (;;)
            {
                for (int i = 0; i < s.Length; i++)
                    s[i] = (byte)r.Next(16);
                Console.WriteLine(so.Send(s));
                Thread.Sleep(1000);
            }
        }
        
        public static void Test1()
        {
            var bc = new BufferCodec();
            var c = new Compress(new Encrypt(bc, new byte[1] { 1 }));
            var r = new Random(1234);
            var s = new byte[1024 * 1024 * 2];
            for (int i = 0; i < s.Length; i++)
                s[i] = (byte)r.Next(16);
            c.update(s, 0, s.Length);
            c.flush();
        
            File.WriteAllBytes("test0.bin", s);
            File.WriteAllBytes("test1.bin", bc.Buffer.Copy());
            Console.WriteLine("OK");
        
        }
        public static void Main(string[] args)
        {
            Test2();
            
            Console.WriteLine(Tx<int, Program>.x);
            Console.WriteLine(Tx<int, A>.x);
        }
    }
}
