package Zeze.Util;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Cert {
	// 对数据签字
	public static byte[] sign(PrivateKey privateKey, byte[] data)
			throws GeneralSecurityException {
		return sign(privateKey, data, 0, data.length);
	}

	// 对数据签字
	public static byte[] sign(PrivateKey privateKey, byte[] data, int offset, int count)
			throws GeneralSecurityException {
		var signer = Signature.getInstance("SHA256WithRSA");
		signer.initSign(privateKey);
		signer.update(data, offset, count);
		return signer.sign();
	}

	// 验证签字
	public static boolean verifySign(Certificate cert, byte[] data, byte[] signature)
			throws GeneralSecurityException {
		return verifySign(cert, data, 0, data.length, signature);
	}

	// 验证签字
	public static boolean verifySign(Certificate cert, byte[] data, int offset, int count, byte[] signature)
			throws GeneralSecurityException {
		var signer = Signature.getInstance("SHA256WithRSA");
		signer.initVerify(cert);
		signer.update(data, offset, count);
		return signer.verify(signature);
	}

	// PublicKey-Rsa加密小块数据
	public static byte[] encryptRsa(Certificate cert, byte[] data)
			throws GeneralSecurityException {
		return encryptRsa(cert, data, 0, data.length);
	}

	// PublicKey-Rsa加密小块数据
	public static byte[] encryptRsa(Certificate cert, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var publicKey = cert.getPublicKey();
		var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(data, offset, size);
	}

	// PrivateKey-Rsa解密小块数据
	public static byte[] decryptRsa(PrivateKey privateKey, byte[] data)
			throws GeneralSecurityException {
		return decryptRsa(privateKey, data, 0, data.length);
	}

	// PrivateKey-Rsa解密小块数据
	public static byte[] decryptRsa(PrivateKey privateKey, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(data, offset, size);
	}

	// PrivateKey-Rsa解密小块数据,NoPadding
	public static byte[] decryptRsaNoPadding(PrivateKey privateKey, byte[] data)
			throws GeneralSecurityException {
		return decryptRsaNoPadding(privateKey, data, 0, data.length);
	}

	// PrivateKey-Rsa解密小块数据,NoPadding
	public static byte[] decryptRsaNoPadding(PrivateKey privateKey, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(data, offset, size);
	}

	// 创建安全的AesKey
	public static SecretKey generateAesKey() throws NoSuchAlgorithmException {
		var KEY_SIZE = 256;
		var keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(KEY_SIZE);
		return keyGenerator.generateKey();
	}

	// 使用自定义的AesKey
	public static SecretKey createAesKey(byte[] key) {
		return new SecretKeySpec(key, "AES");
	}

	// Aes加密
	public static byte[] encryptAes(SecretKey key, byte[] iv, byte[] data)
			throws GeneralSecurityException {
		return encryptAes(key, iv, data, 0, data.length);
	}

	public static byte[] generateAesIv() {
		var iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return iv;
	}

	// Aes加密
	public static byte[] encryptAes(SecretKey key, byte[] iv, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
		return cipher.doFinal(data, offset, size);
	}

	// Aes解密
	public static byte[] decryptAes(SecretKey key, byte[] iv, byte[] data)
			throws GeneralSecurityException {
		return decryptAes(key, iv, data, 0, data.length);
	}

	// Aes解密
	public static byte[] decryptAes(SecretKey key, byte[] iv, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
		return cipher.doFinal(data, offset, size);
	}

	public static void main(String[] args) throws Exception {
		var pkcs12File = "test.pkcs12";
		var passwd = "123".toCharArray();
		var alias = "test";

		var keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(new FileInputStream(pkcs12File), passwd);
		var data = "data".getBytes(StandardCharsets.UTF_8);
		var signature = Files.readAllBytes(Path.of("signature"));
		var cert = keyStore.getCertificate(alias);
		var privateKey = (PrivateKey)keyStore.getKey(alias, passwd);
		var verify = verifySign(cert, data, signature);
		System.out.println("signature.len = " + signature.length);
		System.out.println("verify=" + verify);

		var signature2 = sign(privateKey, data);
		var verify2 = verifySign(cert, data, signature2);
		System.out.println("signature2.len = " + signature2.length);
		System.out.println("verify2=" + verify2);

		var aesKey = generateAesKey();
		var aesKeyData = aesKey.getEncoded();
		var aesKeyEnc = encryptRsa(cert, aesKeyData);
		var aesKeyDec = decryptRsa(privateKey, aesKeyEnc);
		System.out.println("aesKeyEnc.len = " + aesKeyEnc.length);
		System.out.println("aesKeyDec.len = " + aesKeyDec.length);
		System.out.println("compare AES key = " + Arrays.equals(aesKeyData, aesKeyDec));

		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		var dataEnc = encryptAes(aesKey, iv, data);
		var dataDec = decryptAes(aesKey, iv, dataEnc);
		System.out.println("dataEnc.len = " + dataEnc.length);
		System.out.println("dataDec.len = " + dataDec.length);
		System.out.println("compare data = " + Arrays.equals(data, dataDec));

		var aesKeyDecWithPadding = decryptRsaNoPadding(privateKey, aesKeyEnc);
		System.out.println("d1 = " + BitConverter.toString(aesKeyDecWithPadding));
		System.out.println("d0 = " + BitConverter.toString(aesKeyData));

		Files.write(Path.of("e.data"), dataEnc, StandardOpenOption.CREATE);
		Files.write(Path.of("iv"), iv, StandardOpenOption.CREATE);
		Files.write(Path.of("ekey"), aesKeyEnc, StandardOpenOption.CREATE);

		aesKeyEnc = new byte[]{0x05, 0x4C, (byte)0xFD, (byte)0xDB, 0x64, 0x2F, (byte)0xFB, 0x32, 0x14, 0x49, (byte)0xF5, (byte)0xC9, (byte)0x8A, 0x70, (byte)0xF2, 0x1E, 0x28, (byte)0x88, 0x64, 0x27, 0x36, (byte)0x83, (byte)0xA6, (byte)0xD0, 0x38, 0x0D, (byte)0xAF, 0x7E, 0x57, 0x7E, 0x5B, (byte)0x92, 0x43, (byte)0x90, (byte)0x96, 0x26, 0x51, 0x44, (byte)0x87, (byte)0xB3, (byte)0x91, 0x25, 0x74, (byte)0xC8, 0x0E, (byte)0xCD, 0x4F, 0x4C, (byte)0x96, (byte)0xC3, (byte)0xEB, (byte)0xF9, (byte)0xEA, (byte)0xFB, 0x06, (byte)0xC0, (byte)0xF2, (byte)0xD2, (byte)0xE3, 0x45, 0x1F, 0x76, (byte)0xBD, (byte)0xF6, 0x4E, (byte)0xAB, (byte)0xBD, 0x66, (byte)0x98, 0x42, 0x68, 0x6A, (byte)0x81, (byte)0xD7, 0x30, 0x7B, 0x28, (byte)0xA2, 0x59, 0x2B, 0x25, (byte)0xAF, (byte)0x84, (byte)0xDC, 0x30, (byte)0xC4, 0x69, 0x16, (byte)0xBE, (byte)0xC8, 0x13, (byte)0xF0, (byte)0x8C, (byte)0x9B, (byte)0x9E, 0x58, (byte)0xF6, 0x3B, (byte)0xAB, 0x33, (byte)0xB7, (byte)0x89, (byte)0xA2, (byte)0xAC, 0x4E, 0x22, 0x01, 0x0F, (byte)0xDA, (byte)0xD9, (byte)0xAF, 0x57, (byte)0xC3, 0x5C, 0x42, (byte)0x88, 0x43, 0x3C, 0x78, 0x30, (byte)0xC7, 0x46, 0x5B, 0x43, 0x7C, 0x52, (byte)0x9E, 0x03, (byte)0xD8, 0x23, 0x1D, (byte)0xF1, (byte)0x91, 0x75, 0x33, (byte)0x8F, (byte)0xAA, 0x71, 0x0E, (byte)0xAB, 0x18, (byte)0xD2, (byte)0x86, (byte)0xAA, (byte)0x90, (byte)0x8F, 0x17, 0x57, 0x4A, (byte)0xF4, 0x1E, (byte)0xD9, (byte)0xBF, (byte)0xB6, (byte)0xC3, (byte)0xCC, 0x6E, 0x22, 0x7F, 0x6F, (byte)0xDA, 0x28, 0x20, 0x31, 0x07, 0x12, 0x50, 0x11, (byte)0xB9, 0x29, (byte)0xC4, (byte)0xAB, (byte)0xA7, (byte)0xB5, 0x41, 0x5A, 0x7E, (byte)0xA0, (byte)0x97, 0x3B, 0x2B, 0x09, (byte)0xA3, (byte)0xCD, (byte)0x8F, (byte)0xB4, (byte)0x9D, (byte)0xE8, 0x11, (byte)0xB5, (byte)0xEA, 0x22, (byte)0x8F, (byte)0xBB, (byte)0x9B, 0x02, 0x00, 0x7C, 0x58, 0x61, (byte)0xF0, (byte)0xA5, 0x57, 0x7D, (byte)0x9E, 0x64, 0x4C, (byte)0xF4, 0x6D, 0x54, 0x69, (byte)0xFD, 0x79, (byte)0xA6, 0x2A, (byte)0xCB, 0x45, (byte)0x93, (byte)0xAC, (byte)0xB2, (byte)0xE6, 0x4D, 0x12, 0x2F, (byte)0xAC, (byte)0xA8, (byte)0xE9, 0x61, (byte)0xAC, 0x36, (byte)0xDA, 0x43, (byte)0xF9, (byte)0xAE, (byte)0x81, 0x0B, 0x7E, (byte)0x83, 0x68, (byte)0xEC, 0x17, (byte)0x8A, (byte)0xE7, (byte)0xF3, 0x29, (byte)0xFC, (byte)0xF9, 0x37, (byte)0xFA, (byte)0xCC, (byte)0xC3, 0x4D, 0x43, 0x0C, (byte)0x80, 0x1D};
		aesKeyDec = decryptRsa(privateKey, aesKeyEnc);
		System.out.println("aesKeyEnc.len = " + aesKeyEnc.length);
		System.out.println("aesKeyDec.len = " + aesKeyDec.length);
		System.out.println(BitConverter.toString(aesKeyDec));
		iv = new byte[]{0x0E, (byte)0x8A, (byte)0xF9, 0x2E, (byte)0xCF, (byte)0xD1, 0x2A, (byte)0x81, 0x3A, (byte)0xE6, (byte)0xBA, 0x6E, 0x49, 0x1C, (byte)0xA4, 0x07};
		dataEnc = new byte[]{0x1E, 0x50, 0x16, 0x39, 0x40, (byte)0xE4, (byte)0xA6, 0x67, 0x5F, 0x23, (byte)0xF7, (byte)0x83, (byte)0xBF, 0x22, (byte)0xE8, 0x1E};
		dataDec = decryptAes(createAesKey(aesKeyDec), iv, dataEnc, 0, dataEnc.length);
		System.out.println("compare data = " + Arrays.equals(data, dataDec));

//		var priKey = cert.keyStore.getKey("test", "123".toCharArray());
//		var cert1 = cert.keyStore.getCertificate("test");
//		var pubKey = cert1.getPublicKey();
//		System.out.println(priKey);
//		System.out.println("---");
//		System.out.println(cert1);
//		System.out.println("---");
//		System.out.println(pubKey);
	}
}
