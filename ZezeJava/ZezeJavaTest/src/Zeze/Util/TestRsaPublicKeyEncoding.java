package Zeze.Util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestRsaPublicKeyEncoding {
	@Test
	void exportsStandardRsaKeyWithoutProviderEncoding() throws Exception {
		var pair = Cert.generateRsaKeyPair();
		var original = (RSAPublicKey)pair.getPublic();
		RSAPublicKey independentKey = new RSAPublicKey() {
			private static final long serialVersionUID = 1L;

			@Override
			public BigInteger getModulus() {
				return original.getModulus();
			}

			@Override
			public BigInteger getPublicExponent() {
				return original.getPublicExponent();
			}

			@Override
			public String getAlgorithm() {
				return "RSA";
			}

			@Override
			public String getFormat() {
				return "X.509";
			}

			@Override
			public byte[] getEncoded() {
				throw new UnsupportedOperationException("PKCS#1导出只需标准RSA参数");
			}
		};

		var encoded = Cert.exportRsaPublicKeyToPkcs1(independentKey);
		assertArrayEquals(Cert.exportRsaPublicKeyToPkcs1(original), encoded);
		var restored = (RSAPublicKey)Cert.loadRsaPublicKeyByPkcs1(encoded);
		assertEquals(original.getModulus(), restored.getModulus());
		assertEquals(original.getPublicExponent(), restored.getPublicExponent());
		var message = "标准RSA公钥编码".getBytes(StandardCharsets.UTF_8);
		assertTrue(Cert.verifySignRsa(restored, message, Cert.signRsa(pair.getPrivate(), message)));
	}
}
