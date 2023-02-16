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
        public static void Main(string[] args)
        {
            Console.WriteLine(Tx<int, Program>.x);
            Console.WriteLine(Tx<int, A>.x);
        }
    }
}
