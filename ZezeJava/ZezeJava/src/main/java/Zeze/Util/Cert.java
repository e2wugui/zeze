package Zeze.Util;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import sun.security.rsa.RSAPublicKeyImpl;
import sun.security.rsa.RSAUtil;
import sun.security.util.DerValue;
import sun.security.x509.X509CertImpl;

// 生成KeyStore: keytool -genkeypair -keyalg RSA -keysize 2048 -keystore test.ks -storetype pkcs12 -storepass 123456 -alias test -validity 365 -dname "cn=CommonName, ou=OrgName, o=Org, c=Country"
// 查看KeyStore: keytool -list -keystore test.ks -storepass 123456 -v
// 导出PEM格式公钥证书: keytool -exportcert -keystore test.ks -storepass 123456 -alias test -rfc -file test.pem
// 导出DER格式公钥证书: keytool -exportcert -keystore test.ks -storepass 123456 -alias test -file test.der
// 以上两种格式都可以用.cer或.crt后缀名, PEM和DER两种编码格式可以用OpenSSL工具转换
// 参考: https://docs.oracle.com/en/java/javase/11/tools/keytool.html
// 参考: https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html
// 编译时需要: --add-exports java.base/sun.security.x509=ALL-UNNAMED
public final class Cert {
	static {
		try {
			var m = Module.class.getDeclaredMethod("implAddOpensToAllUnnamed", String.class);
			Json.setAccessible(m); // force accessible
			m.invoke(Certificate.class.getModule(), "sun.security.rsa"); // --add-opens java.base/sun.security.rsa=ALL-UNNAMED
			m.invoke(Certificate.class.getModule(), "sun.security.util"); // --add-opens java.base/sun.security.util=ALL-UNNAMED
			m.invoke(Certificate.class.getModule(), "sun.security.x509"); // --add-opens java.base/sun.security.x509=ALL-UNNAMED
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	// 从输入流加载KeyStore(PKCS12格式的二进制密钥存储格式,有密码加密,包含私钥和公钥证书)
	public static KeyStore loadKeyStore(InputStream inputStream, String passwd)
			throws GeneralSecurityException, IOException {
		var keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(inputStream, passwd != null ? passwd.toCharArray() : null);
		return keyStore;
	}

	// 从KeyStore里获取公钥证书
	public static Certificate getCertificate(KeyStore keyStore, String alias) throws KeyStoreException {
		return keyStore.getCertificate(alias);
	}

	// 从KeyStore里获取公钥
	public static PublicKey getPublicKey(KeyStore keyStore, String alias) throws KeyStoreException {
		return keyStore.getCertificate(alias).getPublicKey();
	}

	// 从KeyStore里获取私钥
	public static PrivateKey getPrivateKey(KeyStore keyStore, String passwd, String alias)
			throws GeneralSecurityException {
		return (PrivateKey)keyStore.getKey(alias, passwd != null ? passwd.toCharArray() : null);
	}

	// 以DER二进制编码加载X509公钥证书(此二进制编码即Certificate.getEncoded()的结果)
	public static X509Certificate loadCertificate(byte[] encodedCertificate) throws GeneralSecurityException {
		return new X509CertImpl(encodedCertificate);
	}

	// 从DER或PEM编码的输入流加载X509公钥证书
	public static X509Certificate loadCertificate(InputStream encodedCertificate) throws GeneralSecurityException {
		return new X509CertImpl(encodedCertificate);
	}

	// 以PKCS#8的二进制编码加载RSA公钥(此二进制编码即PublicKey.getEncoded()的结果)
	public static PublicKey loadRsaPublicKey(byte[] encodedPublicKey) throws GeneralSecurityException {
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encodedPublicKey));
	}

	// 以PKCS#8的二进制编码加载椭圆曲线公钥(此二进制编码即PublicKey.getEncoded()的结果)
	public static PublicKey loadEcPublicKey(byte[] encodedPublicKey) throws GeneralSecurityException {
		return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(encodedPublicKey));
	}

	// 以PKCS#1的二进制编码加载RSA公钥
	// 编译和运行时需要: --add-exports/--add-opens java.base/sun.security.rsa=ALL-UNNAMED
	// 编译和运行时需要: --add-exports/--add-opens java.base/sun.security.util=ALL-UNNAMED
	public static PublicKey loadRsaPublicKeyByPkcs1(byte[] encodedPublicKey) throws InvalidKeyException {
		try {
			var dv = new DerValue(encodedPublicKey);
			if (dv.tag != DerValue.tag_Sequence)
				throw new IOException("Not a SEQUENCE");
			var n = dv.data.getPositiveBigInteger();
			var e = dv.data.getPositiveBigInteger();
			if (dv.data.available() != 0)
				throw new IOException("Extra data available");
			return loadRsaPublicKey(n, e);
		} catch (IOException ex) {
			throw new InvalidKeyException("Invalid PKCS#1 encoding", ex);
		}
	}

	public static PublicKey loadRsaPublicKey(BigInteger n, BigInteger e) throws InvalidKeyException {
		return RSAPublicKeyImpl.newKey(RSAUtil.KeyType.RSA, null, n, e);
	}

	// 从二进制编码加载RSA私钥(二进制编码即PrivateKey.getEncoded()的结果)
	public static PrivateKey loadRsaPrivateKey(byte[] encodedPrivateKey) throws GeneralSecurityException {
		return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encodedPrivateKey));
	}

	// 从二进制编码加载椭圆曲线私钥(二进制编码即PrivateKey.getEncoded()的结果)
	public static PrivateKey loadEcPrivateKey(byte[] encodedPrivateKey) throws GeneralSecurityException {
		return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(encodedPrivateKey));
	}

	private static int getDerValueSize(int dataSize) {
		if (dataSize < 0x80)
			return 1 + 1 + dataSize; // tag+size+data
		if (dataSize < 0x100)
			return 1 + 1 + 1 + dataSize; // tag+sizeLen+size+data
		if (dataSize < 0x1_0000)
			return 1 + 1 + 2 + dataSize;
		throw new IllegalArgumentException(); // 暂时不支持更长的
	}

	private static int encodeDerValueHeader(byte[] encoded, int offset, int tag, int dataSize) {
		encoded[offset++] = (byte)tag;
		if (dataSize < 0x80)
			encoded[offset++] = (byte)dataSize;
		else if (dataSize < 0x100) {
			encoded[offset++] = (byte)0x81;
			encoded[offset++] = (byte)dataSize;
		} else if (dataSize < 0x1_0000) {
			encoded[offset++] = (byte)0x82;
			encoded[offset++] = (byte)(dataSize >> 8);
			encoded[offset++] = (byte)dataSize;
		} else
			throw new IllegalArgumentException(); // 暂时不支持更长的
		return offset;
	}

	private static int encodeDerValue(byte[] encoded, int offset, byte[] data) {
		int dataSize = data.length;
		offset = encodeDerValueHeader(encoded, offset, 2, dataSize);
		System.arraycopy(data, 0, encoded, offset, dataSize);
		return offset + dataSize;
	}

	public static byte[] exportRsaPublicKeyToPkcs1(PublicKey publicKey) {
		var rpk = (RSAPublicKeyImpl)publicKey;
		var mod = rpk.getModulus().toByteArray();
		var exp = rpk.getPublicExponent().toByteArray();
		var modExpDerSize = getDerValueSize(mod.length) + getDerValueSize(exp.length);
		var encoded = new byte[getDerValueSize(modExpDerSize)]; // 2048位的RSA公钥通常编码成269~270字节的PKCS#1二进制数据
		var offset = encodeDerValueHeader(encoded, 0, 0x30, modExpDerSize);
		offset = encodeDerValue(encoded, offset, mod);
		offset = encodeDerValue(encoded, offset, exp);
		assert offset == encoded.length;
		return encoded;
	}

	// 生成RSA密钥对(公钥+私钥)
	public static KeyPair generateRsaKeyPair() throws GeneralSecurityException {
		var keyPairGen = KeyPairGenerator.getInstance("RSA");
		keyPairGen.initialize(new RSAKeyGenParameterSpec(2048, BigInteger.valueOf(65537)));
		return keyPairGen.generateKeyPair();
	}

	// 生成椭圆曲线密钥对(公钥+私钥)
	public static KeyPair generateEcKeyPair() throws GeneralSecurityException {
		var keyPairGen = KeyPairGenerator.getInstance("EC");
		keyPairGen.initialize(256);
		return keyPairGen.generateKeyPair();
	}

	// 使用RSA私钥对数据签名
	public static byte[] signRsa(PrivateKey privateKey, byte[] data) throws GeneralSecurityException {
		return signRsa(privateKey, data, 0, data.length);
	}

	// 使用RSA私钥对数据签名
	public static byte[] signRsa(PrivateKey privateKey, byte[] data, int offset, int count)
			throws GeneralSecurityException {
		var signer = Signature.getInstance("SHA256WithRSA");
		signer.initSign(privateKey);
		signer.update(data, offset, count);
		return signer.sign();
	}

	// 使用RSA公钥验证签名
	public static boolean verifySignRsa(PublicKey publicKey, byte[] data, byte[] signature)
			throws GeneralSecurityException {
		return verifySignRsa(publicKey, data, 0, data.length, signature);
	}

	// 使用RSA公钥验证签名
	public static boolean verifySignRsa(PublicKey publicKey, byte[] data, int offset, int count, byte[] signature)
			throws GeneralSecurityException {
		var signer = Signature.getInstance("SHA256WithRSA");
		signer.initVerify(publicKey);
		signer.update(data, offset, count);
		return signer.verify(signature);
	}

	// 使用椭圆曲线私钥对数据签名
	public static byte[] signEc(PrivateKey privateKey, byte[] data) throws GeneralSecurityException {
		return signEc(privateKey, data, 0, data.length);
	}

	// 使用椭圆曲线私钥对数据签名
	public static byte[] signEc(PrivateKey privateKey, byte[] data, int offset, int count)
			throws GeneralSecurityException {
		var signer = Signature.getInstance("SHA256withECDSA");
		signer.initSign(privateKey);
		signer.update(data, offset, count);
		return signer.sign();
	}

	// 使用椭圆曲线公钥验证签名
	public static boolean verifySignEc(PublicKey publicKey, byte[] data, byte[] signature)
			throws GeneralSecurityException {
		return verifySignEc(publicKey, data, 0, data.length, signature);
	}

	// 使用椭圆曲线公钥验证签名
	public static boolean verifySignEc(PublicKey publicKey, byte[] data, int offset, int count, byte[] signature)
			throws GeneralSecurityException {
		var signer = Signature.getInstance("SHA256withECDSA");
		signer.initVerify(publicKey);
		signer.update(data, offset, count);
		return signer.verify(signature);
	}

	// 使用RSA公钥加密小块数据(data长度不超过:RSA位数/8-11)
	public static byte[] encryptRsa(PublicKey publicKey, byte[] data) throws GeneralSecurityException {
		return encryptRsa(publicKey, data, 0, data.length);
	}

	// 使用RSA公钥加密小块数据(size不超过:RSA位数/8-11)
	public static byte[] encryptRsa(PublicKey publicKey, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(data, offset, size);
	}

	// 使用RSA私钥解密小块数据
	public static byte[] decryptRsa(PrivateKey privateKey, byte[] data) throws GeneralSecurityException {
		return decryptRsa(privateKey, data, 0, data.length);
	}

	// 使用RSA私钥解密小块数据
	public static byte[] decryptRsa(PrivateKey privateKey, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(data, offset, size);
	}

	// 使用RSA私钥解密小块数据(不处理padding的原始数据)
	public static byte[] decryptRsaNoPadding(PrivateKey privateKey, byte[] data) throws GeneralSecurityException {
		return decryptRsaNoPadding(privateKey, data, 0, data.length);
	}

	// 使用RSA私钥解密小块数据(不处理padding的原始数据)
	public static byte[] decryptRsaNoPadding(PrivateKey privateKey, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(data, offset, size);
	}

	// 创建安全随机的AES密钥(固定256位)
	public static SecretKey generateAesKey() throws NoSuchAlgorithmException {
		var keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(256);
		return keyGenerator.generateKey();
	}

	// 加载自定义的AES密钥
	public static SecretKey loadAesKey(byte[] key) {
		return new SecretKeySpec(key, "AES");
	}

	// 创建安全随机的IV(固定128位)
	public static byte[] generateAesIv() {
		var iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return iv;
	}

	// 使用AES加密数据(CBC模式需要提供IV,带padding)
	public static byte[] encryptAes(SecretKey key, byte[] iv, byte[] data) throws GeneralSecurityException {
		return encryptAes(key, iv, data, 0, data.length);
	}

	// 使用AES加密数据(CBC模式需要提供IV,带padding)
	public static byte[] encryptAes(SecretKey key, byte[] iv, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
		return cipher.doFinal(data, offset, size);
	}

	// 使用AES解密数据(CBC模式需要提供IV,带padding)
	public static byte[] decryptAes(SecretKey key, byte[] iv, byte[] data) throws GeneralSecurityException {
		return decryptAes(key, iv, data, 0, data.length);
	}

	// 使用AES解密数据(CBC模式需要提供IV,带padding)
	public static byte[] decryptAes(SecretKey key, byte[] iv, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
		return cipher.doFinal(data, offset, size);
	}

	private Cert() {
	}
}
