using System.IO;
using System.Security.Cryptography;
using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.Crypto.Encodings;
using Org.BouncyCastle.Crypto.Engines;
using Org.BouncyCastle.Pkcs;
using Org.BouncyCastle.Security;

namespace Zeze.Util;

public static class Cert
{
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
        var iv = new byte[16];
        new SecureRandom().NextBytes(iv);
        return iv;
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
