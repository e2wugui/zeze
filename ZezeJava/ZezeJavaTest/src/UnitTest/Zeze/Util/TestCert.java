package UnitTest.Zeze.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAKey;
import junit.framework.TestCase;
import org.junit.Assert;
import static Zeze.Util.Cert.*;

public class TestCert extends TestCase {
	private static final int RSA_BLOCK_SIZE = 2048 / 8; // 256
	private static final int AES_BLOCK_SIZE = 128 / 8; // 16
	private static final int AES_KEY_SIZE = 256 / 8; // 32

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
		var aesKeyEnc = encryptRsa(publicKey, aesKeyData);
		var aesKeyDec = decryptRsa(privateKey, aesKeyEnc);
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

		var t = System.currentTimeMillis();
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
}
