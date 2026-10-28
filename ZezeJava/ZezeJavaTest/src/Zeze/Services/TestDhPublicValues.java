package Zeze.Services;

import java.math.BigInteger;
import Zeze.Services.Handshake.Helper;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDhPublicValues {
	private static final int[] GROUPS = {1, 2, 5, 14, 15, 16, 17, 18};

	private static BigInteger modulus(int group) throws Exception {
		var field = Helper.class.getDeclaredField("dh_group");
		field.setAccessible(true);
		return ((BigInteger[])field.get(null))[group];
	}

	@Test
	void rejectsDegenerateAndOutOfDomainPublicValues() throws Exception {
		for (int group : GROUPS) {
			var p = modulus(group);
			for (var value : new BigInteger[]{BigInteger.valueOf(-2), BigInteger.ZERO, BigInteger.ONE,
					p.subtract(BigInteger.ONE), p, p.add(BigInteger.TWO)}) {
				assertThrows(IllegalArgumentException.class,
						() -> Helper.computeDHKey(group, value, BigInteger.valueOf(13)), "group=" + group + " value=" + value);
			}
		}
	}

	@Test
	void existingSignedBigEndianEncodingStillInteroperates() {
		for (int group : GROUPS) {
			var a = new BigInteger("fb9a12073ac84ffd1234ff88ee532971", 16);
			var b = new BigInteger("bed8219ab42894765ff1746ab7819367", 16);
			var publicA = new BigInteger(Helper.generateDHResponse(group, a).toByteArray());
			var publicB = new BigInteger(Helper.generateDHResponse(group, b).toByteArray());
			assertEquals(Helper.computeDHKey(group, publicA, b), Helper.computeDHKey(group, publicB, a));
		}
	}

	@Test
	void supportedGroupsHaveTheDocumentedSafePrimeShape() throws Exception {
		int[] bits = {768, 1024, 1536, 2048, 3072, 4096, 6144, 8192};
		for (int i = 0; i < GROUPS.length; i++) {
			var p = modulus(GROUPS[i]);
			assertEquals(bits[i], p.bitLength());
			// g=2 has order q=(p-1)/2 in these RFC 2409 / RFC 3526 groups.
			assertEquals(BigInteger.ONE, BigInteger.TWO.modPow(p.shiftRight(1), p));
		}
	}
}
