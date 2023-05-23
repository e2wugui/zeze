package Dbh2;

import Zeze.Serialize.ByteBuffer;

public class TmpMain {
	public static void main(String [] args) {
		byte[] key1 = new byte[] { 0x73, (byte)0xF5, (byte)0x98, 0x6D };
		byte[] key2 = new byte[] { 0x74, (byte)0xB3, (byte)0xA1, (byte)0xEC };
		byte[] key3 = new byte[] { (byte)0x8A, 0x0A, 0x38, (byte)0xED };

		System.out.println(ByteBuffer.Wrap(key1).ReadLong());
		System.out.println(ByteBuffer.Wrap(key2).ReadLong());
		System.out.println(ByteBuffer.Wrap(key3).ReadLong());
	}
}
