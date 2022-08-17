using Org.BouncyCastle.Asn1;
using Org.BouncyCastle.Asn1.X509;
using Org.BouncyCastle.Crypto;
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
using System.Threading;

namespace Draft
{
    public class Program
    {
        public static void Main(string[] args)
        {
        }
    }

    public class CertStore
    {
        private Pkcs12Store Store;

        public CertStore()
        {
            Store = new();
        }

        public byte[] SignData(string alias, byte[] data, int offset, int count)
        {
            var key = Store.GetKey(alias)?.Key;
            var signer = SignerUtilities.GetSigner("");
            signer.Init(true, key);
            signer.BlockUpdate(data, offset, count);
            return signer.GenerateSignature();
        }

        public bool VerifySign(string alias, byte[] data, int offset, int count, byte[] signature)
        {
            var cert = Store.GetCertificate(alias)?.Certificate;
            var key = cert.GetPublicKey();
            var signer = SignerUtilities.GetSigner("");
            signer.Init(false, key);
            signer.BlockUpdate(data, offset, count);
            return signer.VerifySignature(signature);
        }

        public byte[] Encrypt(byte[] data, int offset, int count)
        {
            // 这是用c#的证书加密数据的实现，来自微软文档的例子。
            // 最好转换成BouncyCastle模式，统一api使用。
            // 实在不行，BouncyCastle提供了把它的证书转换成c#的证书格式来用。
            byte[] pkcs12Bytes = new byte[12];
            X509Certificate2 cert = new X509Certificate2(pkcs12Bytes);
            var rsaPublicKey = (RSA)cert.PublicKey.Key;

            using (Aes aes = Aes.Create())
            {
                // Create instance of Aes for
                // symetric encryption of the data.
                aes.KeySize = 256;
                aes.Mode = CipherMode.CBC;
                using (ICryptoTransform transform = aes.CreateEncryptor())
                {
                    RSAPKCS1KeyExchangeFormatter keyFormatter = new RSAPKCS1KeyExchangeFormatter(rsaPublicKey);
                    // aes.Key 需要自己设置一个吧？还是说下面这个函数顺带生成了一个随机的？
                    byte[] keyEncrypted = keyFormatter.CreateKeyExchange(aes.Key, aes.GetType());
                    // keyEncrypted, aed.IV 都需要返回打包。
                    return transform.TransformFinalBlock(data, offset, data.Length);
                }
            }
        }

        public byte[] Decrypt(byte[] keyEncrypted, byte[] IV, byte[] encryptedData)
        {
            // 这是用c#的证书加密数据的实现，来自微软文档的例子。
            // 最好转换成BouncyCastle模式，统一api使用。
            // 实在不行，BouncyCastle提供了把它的证书转换成c#的证书格式来用。
            byte[] pkcs12Bytes = new byte[12];
            X509Certificate2 cert = new X509Certificate2(pkcs12Bytes);
            var rsaPrivateKey = cert.GetRSAPrivateKey();
            byte[] KeyDecrypted = rsaPrivateKey.Decrypt(keyEncrypted, RSAEncryptionPadding.Pkcs1);
            // Create instance of Aes for
            // symetric decryption of the data.
            using (Aes aes = Aes.Create())
            {
                aes.KeySize = 256;
                aes.Mode = CipherMode.CBC;

                using (ICryptoTransform transform = aes.CreateDecryptor(KeyDecrypted, IV))
                {
                    return transform.TransformFinalBlock(encryptedData, 0, encryptedData.Length);
                }
            }
        }

        public void Generate()
        {
            // 自签字证书生成。
            // Generate RSA key pair
            var rsaGenerator = new RsaKeyPairGenerator();
            rsaGenerator.Init(new KeyGenerationParameters(new SecureRandom(new MyCryptoApiRandomGenerator()), 2048));
            var keyPair = rsaGenerator.GenerateKeyPair();

            // Generate certificate
            var attributes = new Hashtable();
            attributes[X509Name.E] = "e2wugui@163.com";//设置dn信息的邮箱地址
            attributes[X509Name.CN] = "e2wugui";//设置证书的用户，也就是颁发给谁
            attributes[X509Name.O] = "www.nobody.com";//设置证书的办法者
            attributes[X509Name.C] = "Zh";//证书的语言

            //这里是证书颁发者的信息
            var ordering = new ArrayList();
            ordering.Add(X509Name.E);
            ordering.Add(X509Name.CN);
            ordering.Add(X509Name.O);
            ordering.Add(X509Name.C);

            var gen = new X509V3CertificateGenerator();
            //设置证书序列化号
            gen.SetSerialNumber(BigInteger.ProbablePrime(120, new Random()));
            //设置颁发者dn信息
            gen.SetIssuerDN(new X509Name(ordering, attributes));
            //设置证书生效时间
            gen.SetNotBefore(DateTime.Today.Subtract(new TimeSpan(1, 0, 0, 0)));
            //设置证书失效时间
            gen.SetNotAfter(DateTime.Today.AddDays(365));
            //设置接受者dn信息
            gen.SetSubjectDN(new X509Name(ordering, attributes));
            //设置证书的公钥
            gen.SetPublicKey(keyPair.Public);

            //设置证书的加密算法
            gen.SetSignatureAlgorithm("SHA1WithRSA");
            gen.AddExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false));
            gen.AddExtension(X509Extensions.AuthorityKeyIdentifier, true,
                new AuthorityKeyIdentifier(SubjectPublicKeyInfoFactory.CreateSubjectPublicKeyInfo(keyPair.Public)));

            //创建证书，如果需要cer格式的证书，到这里就可以了。如果是pfx格式的就需要加上访问密码
            var cert = gen.Generate(keyPair.Private);
            Store.SetCertificateEntry("alias", new X509CertificateEntry(cert));

            /*
            string password = "213978863940714";
            byte[] pkcs12Bytes = DotNetUtilities.ToX509Certificate(x509Certificate).Export(X509ContentType.Pfx, password);
            var certificate = new X509Certificate2(pkcs12Bytes, password);

            var array = certificate.Export(X509ContentType.Pfx, password);
            var cerArray = certificate.Export(X509ContentType.Cert);

            string path = "hello.pfx";
            string pathcer = "hello.cer";

            FileStream fsCA = new FileStream(path, FileMode.Create);
            //将byte数组写入文件中
            fsCA.Write(array, 0, array.Length);
            fsCA.Close();

            FileStream fscer = new FileStream(pathcer, FileMode.Create);
            //将byte数组写入文件中
            fscer.Write(cerArray, 0, cerArray.Length);
            fscer.Close();
            */
        }
    }

    public class MyCryptoApiRandomGenerator : IRandomGenerator
    {
        public MyCryptoApiRandomGenerator()
        {
        }

        public virtual void AddSeedMaterial(byte[] seed)
        {
            // I don't care about the seed
        }

        public virtual void AddSeedMaterial(long seed)
        {
            // I don't care about the seed
        }

        public virtual void NextBytes(byte[] bytes)
        {
            Buffer.BlockCopy(RandomNumberGenerator.GetBytes(bytes.Length), 0, bytes, 0, bytes.Length);
        }

        public virtual void NextBytes(byte[] bytes, int offset, int count)
        {
            Buffer.BlockCopy(RandomNumberGenerator.GetBytes(count), 0, bytes, offset, count);
        }
    }
}
