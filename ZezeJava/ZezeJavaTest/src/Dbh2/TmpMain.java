package Dbh2;

import Zeze.Serialize.ByteBuffer;

public class TmpMain {
	public static void main(String [] args) {
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x72, (byte)0xFD, (byte)0x82, (byte)0xD4 }).ReadLong());
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x72, (byte)0x3B, (byte)0xDB, (byte)0x76 }).ReadLong());
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x72, (byte)0xFC, (byte)0xA2, (byte)0x39 }).ReadLong());
	}
}
