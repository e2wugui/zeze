package Zeze.Util;

public class BitConverter {
	
	private static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	public static String toString(byte[] bytes) {
		var len = bytes.length;
		var r = new char[len*2];
		for(int i = 0; i < len; ++i) {
			byte b = bytes[i];
			r[2*i] = hexDigit[(b>>8)&0xf];
			r[2*i+1] = hexDigit[b&0xf];
		}
		return new String(r);
	}
	
	public static void main(String[] args) {
		var bytes = new byte[256];
		for(int i = 0; i < bytes.length; ++i)
			bytes[i] = (byte)i;
		System.out.println(BitConverter.toString(bytes));
	}
}


