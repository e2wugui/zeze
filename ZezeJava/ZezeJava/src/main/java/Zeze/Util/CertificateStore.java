package Zeze.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class CertificateStore {
	private final KeyStore keyStore;

	public CertificateStore(String pkcs12File, String passwd) throws GeneralSecurityException, IOException {
		keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(new FileInputStream(pkcs12File), passwd.toCharArray());
	}

	public byte[] sign(String alias, String passwd, byte[] data, int offset, int count) throws GeneralSecurityException {
		var signer = Signature.getInstance("SHA256WithRSA");
		signer.initSign((PrivateKey)keyStore.getKey(alias, passwd.toCharArray()));
		signer.update(data, offset, count);
		return signer.sign();
	}

	public boolean verifySign(String alias, byte[] data, int offset, int count, byte[] signature) throws GeneralSecurityException {
		var certificate = keyStore.getCertificate(alias);
		var signer = Signature.getInstance("SHA256WithRSA");
		signer.initVerify(certificate);
		signer.update(data, offset, count);
		return signer.verify(signature);
	}

	public byte[] encryptRsa(String alias, byte[] data, int offset, int size) throws GeneralSecurityException {
		var certificate = keyStore.getCertificate(alias);
		var publicKey = certificate.getPublicKey();
		var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(data, offset, size);
	}

	public byte[] decryptRsa(String alias, String passwd, byte[] data, int offset, int size) throws GeneralSecurityException {
		var privateKey = keyStore.getKey(alias, passwd.toCharArray());
		var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(data, offset, size);
	}

	public static SecretKey generateAesKey() throws NoSuchAlgorithmException {
		var KEY_SIZE = 256;
		var keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(KEY_SIZE);
		return keyGenerator.generateKey();
	}

	public static byte[] encryptAes(SecretKey key, byte[] iv, byte[] data, int offset, int size) throws GeneralSecurityException {
		var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
		return cipher.doFinal(data, offset, size);
	}

	public static byte[] decryptAes(SecretKey key, byte[] iv, byte[] data, int offset, int size) throws GeneralSecurityException {
		var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
		return cipher.doFinal(data, offset, size);
	}

	public static void main(String[] args) throws Exception {
		var cert = new CertificateStore("test.pkcs12", "123");
		var data = "data".getBytes(StandardCharsets.UTF_8);
		var signature = Files.readAllBytes(Path.of("signature"));
		var verify = cert.verifySign("test", data, 0, data.length, signature);
		System.out.println("signature.len = " + signature.length);
		System.out.println("verify=" + verify);

		var signature2 = cert.sign("test", "123", data, 0, data.length);
		var verify2 = cert.verifySign("test", data, 0, data.length, signature2);
		System.out.println("signature2.len = " + signature2.length);
		System.out.println("verify2=" + verify2);

		var aesKey = generateAesKey();
		var aesKeyData = aesKey.getEncoded();
		var aesKeyEnc = cert.encryptRsa("test", aesKeyData, 0, aesKeyData.length);
		var aesKeyDec = cert.decryptRsa("test", "123", aesKeyEnc, 0, aesKeyEnc.length);
		System.out.println("aesKeyEnc.len = " + aesKeyEnc.length);
		System.out.println("aesKeyDec.len = " + aesKeyDec.length);
		System.out.println("compare AES key = " + Arrays.equals(aesKeyData, aesKeyDec));

		var dataEnc = encryptAes(aesKey, new byte[16], data, 0, data.length);
		var dataDec = decryptAes(aesKey, new byte[16], dataEnc, 0, dataEnc.length);
		System.out.println("dataEnc.len = " + dataEnc.length);
		System.out.println("dataDec.len = " + dataDec.length);
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
