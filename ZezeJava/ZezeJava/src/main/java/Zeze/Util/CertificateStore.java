package Zeze.Util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

public class CertificateStore {
	private KeyStore keyStore;

	public CertificateStore(String pkcs12File, String passwd)
			throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
		keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(new FileInputStream(pkcs12File), passwd.toCharArray());
	}

	public boolean verifySign(String alias, byte[] data, int offset, int count, byte[] signature)
			throws KeyStoreException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		var certificate = keyStore.getCertificate(alias);
		var signer = Signature.getInstance("SHA256WithRSA"); // todo java name
		signer.initVerify(certificate);
		signer.update(data, offset, count);
		return signer.verify(signature);
	}

	public byte[] encryptAesKey(String alias, byte[] key, int offset, int count)
			throws KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		var certificate = keyStore.getCertificate(alias);
		var publicKey = certificate.getPublicKey();
		var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(key, offset, count);
	}

	public byte[] encryptAes(String alias, byte[] data, int offset, int count)
			throws KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, IOException {
		var cipher = Cipher.getInstance("AES/CBC");
		var KEY_SIZE = 32;
		var keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(KEY_SIZE);
		var key = keyGenerator.generateKey();
		var keyEncrypted = encryptAesKey(alias, key.getEncoded(), 0, key.getEncoded().length);
		var opmode = 0; // ?
		cipher.init(opmode, key);
		return cipher.doFinal(data, offset, count);
	}

	public static void main(String args[]) throws Exception {
		var cert = new CertificateStore("test.pkcs12", "123");
		var data = "data".getBytes(StandardCharsets.UTF_8);
		var signature = Files.readAllBytes(Path.of("signature"));
		var verify = cert.verifySign("test", data, 0, data.length, signature);
		System.out.println("verify=" + verify);
	}
}
