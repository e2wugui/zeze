package Zeze.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Certificate {

	public boolean verifySign(byte[] data, int offset, int count, byte[] signature)
			throws KeyStoreException, IOException, NoSuchAlgorithmException,
			InvalidKeyException, SignatureException, CertificateException {
		// todo
		var keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(new FileInputStream("receiver_keytore.p12"), "changeit".toCharArray());
		var certificate = keyStore.getCertificate("receiverKeyPair");

		var signer = Signature.getInstance("TODO"); // todo
		signer.initVerify(certificate);
		signer.update(data, offset, count);
		return signer.verify(signature);
	}

	public byte[] encryptAesKey(byte[] key, int offset, int count)
			throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		// 证书来源参数或者Rsa-Public-Key。
		var keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(new FileInputStream("receiver_keytore.p12"), "changeit".toCharArray());
		var certificate = keyStore.getCertificate("receiverKeyPair");
		var publicKey = (RSAPublicKey)certificate.getPublicKey(); // todo 强制转换，java public key 带算法的。
		var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		// c# 加密Key使用 RSAPKCS1KeyExchangeFormatter 创建的。
		return cipher.doFinal(key, offset, count);
	}

	public byte[] encryptAes(byte[] data, int offset, int count)
			throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		var encryptor = Cipher.getInstance("AES");
		// todo encryptor 初始化。
		var opmode = 0; // ?
		var key = new byte[32]; // todo random gen
		var keyEncrypted = encryptAesKey(key, 0, key.length);
		// encryptor.init(opmode, key);
		return encryptor.doFinal(data, offset, count);
	}
}
