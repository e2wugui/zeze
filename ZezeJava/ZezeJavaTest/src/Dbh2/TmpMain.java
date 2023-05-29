package Dbh2;

import Zeze.Serialize.ByteBuffer;

public class TmpMain {
	public static void main(String [] args) {
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x72, (byte)0xFB, (byte)0x57, (byte)0xC4 }).ReadLong());
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x71, (byte)0x7D, (byte)0x9C, (byte)0xE2 }).ReadLong());
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x74, (byte)0x78, (byte)0x83, (byte)0xC9 }).ReadLong());
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x73, (byte)0xB9, (byte)0xAF, (byte)0x99 }).ReadLong());
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x70, (byte)0xBF, (byte)0x00, (byte)0x13 }).ReadLong());
	}
}
