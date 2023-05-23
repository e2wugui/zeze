package Dbh2;

import Zeze.Serialize.ByteBuffer;

public class TmpMain {
	public static void main(String [] args) {
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x71, (byte)0xF9, (byte)0xC6, (byte)0xE0 }).ReadLong());
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x72, (byte)0x58, (byte)0x96, (byte)0x1E }).ReadLong());
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x72, (byte)0xFC, (byte)0xA2, (byte)0x39 }).ReadLong());
	}
}
