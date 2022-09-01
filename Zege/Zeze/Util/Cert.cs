using System;
using System.IO;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;

/*
using Org.BouncyCastle.Asn1;
using Org.BouncyCastle.Asn1.X509;
using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.Crypto.Encodings;
using Org.BouncyCastle.Crypto.Engines;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Math;
using Org.BouncyCastle.Pkcs;
using Org.BouncyCastle.Security;
using Org.BouncyCastle.X509;
*/
namespace Zeze.Util;

public static class Cert
{
    public static X509Certificate2 CreateFromPkcs12(string pkcs12File, string passwd)
    {
        var pkcs12 = File.ReadAllBytes(pkcs12File);
        return new(pkcs12, passwd);
    }

    public static X509Certificate2 CreateFromPkcs12(Stream pkcs12Stream, string passwd)
    {
        var pkcs12Bytes = new byte[pkcs12Stream.Length];
        var offset = 0;
        while (offset < pkcs12Bytes.Length)
        {
            var rc = pkcs12Stream.Read(pkcs12Bytes, offset, pkcs12Bytes.Length - offset);
            if (rc == 0)
                break;
            offset += rc;
        }
        return new(pkcs12Bytes, passwd);
    }

    public static X509Certificate2 CreateFromPkcs12(byte[] pkcs12Bytes, string passwd)
    {
        return new(pkcs12Bytes, passwd);
    }

    // 使用RSA私钥对数据签名
    public static byte[] Sign(X509Certificate2 cert, byte[] data)
    {
        return cert.GetRSAPrivateKey().SignData(data, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1);
    }

    public static byte[] Sign(RSA privateKey, byte[] data)
    {
        return privateKey.SignData(data, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1);
    }

    // 使用RSA私钥对数据签名
    public static byte[] Sign(X509Certificate2 cert, byte[] data, int offset, int count)
    {
        return cert.GetRSAPrivateKey().SignData(data, offset, count, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1);
    }

    // 使用RSA公钥验证签名
    public static bool VerifySign(X509Certificate2 cert, byte[] data, byte[] signature)
    {
        return cert.GetRSAPublicKey().VerifyData(data, signature, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1);
    }

    // 使用RSA公钥验证签名
    public static bool VerifySign(X509Certificate2 cert, byte[] data, int offset, int count, byte[] signature)
    {
        return cert.GetRSAPublicKey().VerifyData(data, offset, count, signature, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1);
    }

    // 使用RSA公钥加密小块数据(data长度不超过:RSA位数/8-11)
    public static byte[] EncryptRsa(X509Certificate2 cert, byte[] data)
    {
        return cert.GetRSAPublicKey().Encrypt(data, RSAEncryptionPadding.Pkcs1);
    }

    // 使用RSA公钥加密小块数据(size不超过:RSA位数/8-11)
    public static byte[] EncryptRsa(X509Certificate2 cert, byte[] data, int offset, int size)
    {
        if (data.Length != size)
        {
            // 如果需要处理的数据不是完整的数组长度，这里不做offset合法性判断。
            var copy = new byte[size];
            Buffer.BlockCopy(data, offset, copy, 0, size);
            data = copy;
        }
        return cert.GetRSAPublicKey().Encrypt(data, RSAEncryptionPadding.Pkcs1);
    }

    // 使用RSA私钥解密小块数据
    public static byte[] DecryptRsa(X509Certificate2 cert, byte[] data)
    {
        return cert.GetRSAPrivateKey().Decrypt(data, RSAEncryptionPadding.Pkcs1);
    }

    // 使用RSA私钥解密小块数据
    public static byte[] DecryptRsa(X509Certificate2 cert, byte[] data, int offset, int size)
    {
        if (data.Length != size)
        {
            // 如果需要处理的数据不是完整的数组长度，这里不做offset合法性判断。
            var copy = new byte[size];
            Buffer.BlockCopy(data, offset, copy, 0, size);
            data = copy;
        }
        return cert.GetRSAPrivateKey().Decrypt(data, RSAEncryptionPadding.Pkcs1);
    }

    public static RSA GenerateRsa()
    {
        return RSA.Create(2048);
    }

    public static X509Certificate2 CreateFromCertAndPrivateKey(byte[] derCert, RSA rsaForPrivateKey)
    {
        var pemCert = PemEncoding.Write("PEM CERT", derCert);
        return CreateFromCertAndPrivateKey(pemCert, rsaForPrivateKey);
    }

    public static X509Certificate2 CreateFromCertAndPrivateKey(char[] pemCert, RSA rsaForPrivateKey)
    {
        var passwd = "".ToCharArray(); // 临时密码参数，函数结束就作废了。
        //var pkcs8Bytes = rsaForPrivateKey.ExportEncryptedPkcs8PrivateKey(passwd,
        //    new PbeParameters(PbeEncryptionAlgorithm.Aes256Cbc, HashAlgorithmName.SHA256, iterationCount: 100_000));
        var pkcs8Bytes = rsaForPrivateKey.ExportPkcs8PrivateKey();
        // TODO Pem没有加密的话。空串给CreateFromEncryptedPem是否能工作？如果必须加密，使用上面的方法导出。
        var pemPrivateKey = PemEncoding.Write("RSA PRIVATE KEY", pkcs8Bytes);
        return X509Certificate2.CreateFromEncryptedPem(pemCert, pemPrivateKey, passwd);
    }

    /*
    // 从输入流加载KeyStore(PKCS12格式的二进制密钥存储格式,有密码加密,包含私钥和公钥证书)
    public static Pkcs12Store LoadKeyStore(Stream inputStream, string passwd)
    {
        return new Pkcs12Store(inputStream, passwd.ToCharArray());
    }

    // 从KeyStore里获取公钥
    public static AsymmetricKeyParameter GetPublicKey(Pkcs12Store keyStore, string alias)
    {
        return keyStore.GetCertificate(alias).Certificate.GetPublicKey();
    }

    // 从KeyStore里获取私钥
    public static AsymmetricKeyParameter GetPrivateKey(Pkcs12Store keyStore, string alias)
    {
        return keyStore.GetKey(alias).Key;
    }

    // 生成RSA密钥对(公钥+私钥)
    public static AsymmetricCipherKeyPair GenerateRsaKeyPair()
    {
        var keyPairGen = new RsaKeyPairGenerator();
        keyPairGen.Init(new KeyGenerationParameters(new SecureRandom(), 2048));
        return keyPairGen.GenerateKeyPair();
    }

#pragma warning disable CS0618 // Obsolete
    // 为RSA公钥和私钥生成自签名的公钥证书并连同私钥保存到用密码加密的KeyStore输出流
    public static void SaveKeyStore(Stream outputStream, string passwd, string alias, AsymmetricKeyParameter publicKey,
        AsymmetricKeyParameter privateKey, string commonName, int validDays)
    {
        var attributes = new Dictionary<DerObjectIdentifier, string>
        {
            [X509Name.CN] = commonName
        };
        var ordering = new List<DerObjectIdentifier> { X509Name.CN }; // 这里是证书颁发者的信息

        var rand = new SecureRandom();
        var gen = new X509V3CertificateGenerator();
        gen.SetSerialNumber(BigInteger.ProbablePrime(120, rand)); // 设置证书序列号
        gen.SetIssuerDN(new X509Name(ordering, attributes)); // 设置颁发者dn信息
        gen.SetNotBefore(DateTime.Today.Subtract(new TimeSpan(1, 0, 0, 0))); // 设置证书生效时间
        gen.SetNotAfter(DateTime.Today.AddDays(validDays)); // 设置证书失效时间
        gen.SetSubjectDN(new X509Name(ordering, attributes)); // 设置接受者dn信息
        gen.SetPublicKey(publicKey); // 设置证书的公钥
        gen.SetSignatureAlgorithm("SHA256WithRSA"); // 设置证书的加密算法
        gen.AddExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false));
        gen.AddExtension(X509Extensions.AuthorityKeyIdentifier, true,
            new AuthorityKeyIdentifier(SubjectPublicKeyInfoFactory.CreateSubjectPublicKeyInfo(publicKey)));

        var cert = gen.Generate(privateKey); // 创建证书，如果需要cer格式的证书，到这里就可以了。如果是pfx格式的就需要加上访问密码
        var store = new Pkcs12Store();
        store.SetKeyEntry(alias, new AsymmetricKeyEntry(privateKey), new X509CertificateEntry[] { new(cert) });
        store.Save(outputStream, passwd.ToCharArray(), rand);
    }
#pragma warning restore CS0618 // Obsolete

    // 使用RSA私钥对数据签名
    public static byte[] Sign(AsymmetricKeyParameter privateKey, byte[] data)
    {
        return Sign(privateKey, data, 0, data.Length);
    }

    // 使用RSA私钥对数据签名
    public static byte[] Sign(AsymmetricKeyParameter privateKey, byte[] data, int offset, int count)
    {
        var signer = SignerUtilities.GetSigner("SHA256WithRSA");
        signer.Init(true, privateKey);
        signer.BlockUpdate(data, offset, count);
        return signer.GenerateSignature();
    }

    // 使用RSA公钥验证签名
    public static bool VerifySign(AsymmetricKeyParameter publicKey, byte[] data, byte[] signature)
    {
        return VerifySign(publicKey, data, 0, data.Length, signature);
    }

    // 使用RSA公钥验证签名
    public static bool VerifySign(AsymmetricKeyParameter publicKey, byte[] data, int offset, int count, byte[] signature)
    {
        var signer = SignerUtilities.GetSigner("SHA256WithRSA");
        signer.Init(false, publicKey);
        signer.BlockUpdate(data, offset, count);
        return signer.VerifySignature(signature);
    }

    // 使用RSA公钥加密小块数据(data长度不超过:RSA位数/8-11)
    public static byte[] EncryptRsa(AsymmetricKeyParameter publicKey, byte[] data)
    {
        return EncryptRsa(publicKey, data, 0, data.Length);
    }

    // 使用RSA公钥加密小块数据(size不超过:RSA位数/8-11)
    public static byte[] EncryptRsa(AsymmetricKeyParameter publicKey, byte[] data, int offset, int size)
    {
        var pkcs1 = new Pkcs1Encoding(new RsaBlindedEngine());
        pkcs1.Init(true, publicKey);
        return pkcs1.ProcessBlock(data, offset, size);
    }

    // 使用RSA私钥解密小块数据
    public static byte[] DecryptRsa(AsymmetricKeyParameter privateKey, byte[] data)
    {
        return DecryptRsa(privateKey, data, 0, data.Length);
    }

    // 使用RSA私钥解密小块数据
    public static byte[] DecryptRsa(AsymmetricKeyParameter privateKey, byte[] data, int offset, int size)
    {
        var pkcs1 = new Pkcs1Encoding(new RsaEngine());
        pkcs1.Init(false, privateKey);
        return pkcs1.ProcessBlock(data, offset, size);
    }

    // 使用RSA私钥解密小块数据(不处理padding的原始数据)
    public static byte[] DecryptRsaNoPadding(AsymmetricKeyParameter privateKey, byte[] data)
    {
        return DecryptRsaNoPadding(privateKey, data, 0, data.Length);
    }

    // 使用RSA私钥解密小块数据(不处理padding的原始数据)
    public static byte[] DecryptRsaNoPadding(AsymmetricKeyParameter privateKey, byte[] data, int offset, int size)
    {
        var rsa = new RsaEngine();
        rsa.Init(false, privateKey);
        return rsa.ProcessBlock(data, offset, size);
    }
    */

    // 创建安全随机的AES密钥(固定256位)
    public static Aes GenerateAesKey()
    {
        var aes = Aes.Create();
        aes.KeySize = 256; // default: 256
        aes.Mode = CipherMode.CBC; // default: CBC
        return aes;
    }

    // 加载自定义的AES密钥
    public static Aes LoadAesKey(byte[] key)
    {
        var aes = Aes.Create();
        aes.KeySize = 256; // default: 256
        aes.Mode = CipherMode.CBC; // default: CBC
        aes.Key = key;
        return aes;
    }

    // 创建安全随机的IV(固定128位)
    public static byte[] GenerateAesIv()
    {
        return RandomNumberGenerator.GetBytes(16);
    }

    // 使用AES加密数据(CBC模式需要提供IV,带padding)
    public static byte[] EncryptAes(Aes aes, byte[] iv, byte[] data)
    {
        return EncryptAes(aes, iv, data, 0, data.Length);
    }

    // 使用AES加密数据(CBC模式需要提供IV,带padding)
    public static byte[] EncryptAes(Aes aes, byte[] iv, byte[] data, int offset, int size)
    {
        using var encryptor = aes.CreateEncryptor(aes.Key, iv);
        return encryptor.TransformFinalBlock(data, offset, size);
    }

    // 使用AES解密数据(CBC模式需要提供IV,带padding)
    public static byte[] DecryptAes(Aes aes, byte[] iv, byte[] data)
    {
        return DecryptAes(aes, iv, data, 0, data.Length);
    }

    // 使用AES解密数据(CBC模式需要提供IV,带padding)
    public static byte[] DecryptAes(Aes aes, byte[] iv, byte[] data, int offset, int size)
    {
        using var decryptor = aes.CreateDecryptor(aes.Key, iv);
        return decryptor.TransformFinalBlock(data, offset, size);
    }
}
