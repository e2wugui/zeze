package UnitTest.Zeze.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.util.Date;
import junit.framework.TestCase;
import org.junit.Assert;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;
import static Zeze.Util.Cert.*;

// 编译时需要: --add-exports java.base/sun.security.x509=ALL-UNNAMED
public class TestCert extends TestCase {
	private static final int RSA_BLOCK_SIZE = 2048 / 8; // 256
	private static final int AES_BLOCK_SIZE = 128 / 8; // 16
	private static final int AES_KEY_SIZE = 256 / 8; // 32

	// 用owner的公钥为owner生成证书,并用issuer的私钥为证书签名,返回该证书
	public static X509Certificate generateRsaCert(String ownerName, PublicKey publicKey, String issuerName,
												  PrivateKey privateKeyForSign, int validDays)
			throws GeneralSecurityException, IOException {
		var certInfo = new X509CertInfo();
		certInfo.setVersion(new CertificateVersion(CertificateVersion.V3));
		certInfo.setSerialNumber(new CertificateSerialNumber(new BigInteger(160, new SecureRandom())));
		certInfo.setSubject(new X500Name("CN=" + ownerName));
		certInfo.setIssuer(new X500Name("CN=" + issuerName));
		var nowTime = System.currentTimeMillis();
		certInfo.setValidity(new CertificateValidity(new Date(nowTime), new Date(nowTime + validDays * 86400_000L)));
		certInfo.setKey(new CertificateX509Key(publicKey));
		certInfo.setAlgorithmId(new CertificateAlgorithmId(new AlgorithmId(AlgorithmId.SHA256withRSA_oid)));
		return X509CertImpl.newSigned(certInfo, privateKeyForSign, "SHA256withRSA");
	}

	// 为RSA公钥和私钥生成自签名的公钥证书并连同私钥保存到用密码加密的KeyStore输出流
	public static void saveKeyStore(OutputStream outputStream, String passwd, String alias, PublicKey publicKey,
									PrivateKey privateKey, String ownerName, int validDays)
			throws GeneralSecurityException, IOException {
		var cert = generateRsaCert(ownerName, publicKey, ownerName, privateKey, validDays);
		cert.verify(publicKey); // 此行可选

		var keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(null, null);
		// keyStore.setCertificateEntry(alias, cert);
		keyStore.setKeyEntry(alias, privateKey, null, new Certificate[]{cert});
		keyStore.store(outputStream, passwd != null ? passwd.toCharArray() : null);
	}

	public void testAll() throws Exception {
		var pkcs12File = "test.ks";
		var passwd = "123";
		var alias = "test";
		var data = "data".getBytes(StandardCharsets.UTF_8);

		if (!new File(pkcs12File).exists()) {
			var keyPair = generateRsaKeyPair();
			try (var fs = new FileOutputStream(pkcs12File)) {
				saveKeyStore(fs, passwd, alias, keyPair.getPublic(), keyPair.getPrivate(), "test", 365);
			}
		}

		var keyStore = loadKeyStore(new FileInputStream(pkcs12File), passwd);
		var publicKey = getPublicKey(keyStore, alias);
		var privateKey = getPrivateKey(keyStore, null, alias);

		if (new File("signature").exists()) {
			var signature = Files.readAllBytes(Path.of("signature"));
			var verify = verifySignRsa(publicKey, data, signature);
			Assert.assertEquals(RSA_BLOCK_SIZE, signature.length);
			Assert.assertTrue(verify);
		}

		var signature2 = signRsa(privateKey, data);
		System.out.println("RSA sign.length=" + signature2.length);
		var verify2 = verifySignRsa(publicKey, data, signature2);
		Assert.assertEquals(RSA_BLOCK_SIZE, signature2.length);
		Assert.assertTrue(verify2);

		var aesKey = generateAesKey();
		var aesKeyData = aesKey.getEncoded();
		var t = System.nanoTime();
		var aesKeyEnc = encryptRsa(publicKey, aesKeyData);
		System.out.println("RSA encrypt " + (System.nanoTime() - t) + " ns");
		t = System.nanoTime();
		var aesKeyDec = decryptRsa(privateKey, aesKeyEnc);
		System.out.println("RSA decrypt " + (System.nanoTime() - t) + " ns");
		Assert.assertEquals(RSA_BLOCK_SIZE, aesKeyEnc.length);
		Assert.assertEquals(AES_KEY_SIZE, aesKeyDec.length);
		Assert.assertArrayEquals(aesKeyData, aesKeyDec);

		var iv = generateAesIv();
		var dataEnc = encryptAes(aesKey, iv, data);
		var dataDec = decryptAes(aesKey, iv, dataEnc);
		Assert.assertEquals(AES_BLOCK_SIZE, dataEnc.length);
		Assert.assertEquals(data.length, dataDec.length);
		Assert.assertArrayEquals(data, dataDec);

//		var aesKeyDecWithPadding = decryptRsaNoPadding(privateKey, aesKeyEnc);
//		System.out.println("d1 = " + BitConverter.toString(aesKeyDecWithPadding));
//		System.out.println("d0 = " + BitConverter.toString(aesKeyData));

//		Files.write(Path.of("e.data"), dataEnc, StandardOpenOption.CREATE);
//		Files.write(Path.of("iv"), iv, StandardOpenOption.CREATE);
//		Files.write(Path.of("ekey"), aesKeyEnc, StandardOpenOption.CREATE);

//		var publicKeyData = ((RSAKey)publicKey).getModulus().toByteArray();
//		System.out.println("rsa modulus = [" + publicKeyData.length + "] " + BitConverter.toString(publicKeyData));

		t = System.currentTimeMillis();
		var keyPair = generateRsaKeyPair();
		System.out.println("generateRsaKeyPair: " + (System.currentTimeMillis() - t) + " ms");
		var encodedPublicKey = keyPair.getPublic().getEncoded();
		System.out.println("RSA pubKey.encodeSize=" + encodedPublicKey.length);
		publicKey = loadRsaPublicKey(encodedPublicKey);
		Assert.assertArrayEquals(encodedPublicKey, publicKey.getEncoded());
		var encodedPrivateKey = keyPair.getPrivate().getEncoded();
		System.out.println("RSA priKey.encodeSize=" + encodedPrivateKey.length);
		privateKey = loadRsaPrivateKey(encodedPrivateKey);
		Assert.assertArrayEquals(encodedPrivateKey, privateKey.getEncoded());
		Assert.assertEquals(((RSAKey)publicKey).getModulus(), ((RSAKey)privateKey).getModulus());

		var cert = getCertificate(keyStore, alias);
		var certData = cert.getEncoded();
		cert = loadCertificate(certData);
		Assert.assertArrayEquals(certData, cert.getEncoded());

		var pkcs1 = exportRsaPublicKeyToPkcs1(keyPair.getPublic());
		var pubKey = loadRsaPublicKeyByPkcs1(pkcs1);
		Assert.assertArrayEquals(keyPair.getPublic().getEncoded(), pubKey.getEncoded());

//		saveKeyStore(new FileOutputStream("save.ks"), "123456", "test", keyPair.getPublic(), keyPair.getPrivate(), "test", 365);

		t = System.currentTimeMillis();
		keyPair = generateEcKeyPair();
		System.out.println("generateEcKeyPair:  " + (System.currentTimeMillis() - t) + " ms");
		encodedPublicKey = keyPair.getPublic().getEncoded();
		System.out.println("EC  pubKey.encodeSize=" + encodedPublicKey.length);
		publicKey = loadEcPublicKey(encodedPublicKey);
		Assert.assertArrayEquals(encodedPublicKey, publicKey.getEncoded());
		encodedPrivateKey = keyPair.getPrivate().getEncoded();
		System.out.println("EC  priKey.encodeSize=" + encodedPrivateKey.length);
		privateKey = loadEcPrivateKey(encodedPrivateKey);
		Assert.assertArrayEquals(encodedPrivateKey, privateKey.getEncoded());

		signature2 = signEc(privateKey, data);
		System.out.println("EC  sign.length=" + signature2.length);
		verify2 = verifySignEc(publicKey, data, signature2);
		Assert.assertTrue(verify2);
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 100; i++)
			new TestCert().testAll();
	}
}
