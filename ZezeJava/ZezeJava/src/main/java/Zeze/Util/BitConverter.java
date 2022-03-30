package Zeze.Util;

public class BitConverter {
	
	private static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	public static String toString(byte[] bytes, int offset, int len) {
		var sb = new StringBuilder();
		for(int i = 0; i < len; ++i) {
			byte b = bytes[i + offset];
			if (i > 0)
				sb.append('-');
			sb.append(hexDigit[(b>>4)&0xf]);
			sb.append(hexDigit[b&0xf]);
		}
		return sb.toString();
	}

	public static String toString(byte[] bytes) {
		return toString(bytes, 0, bytes.length);
	}
	
	public static void main(String[] args) {
		var bytes = new byte[256];
		for(int i = 0; i < bytes.length; ++i)
			bytes[i] = (byte)i;
		System.out.println(BitConverter.toString(bytes));
	}
}


