package Temp;

import Zeze.Util.ZstdFactory;

public final class TestZstd {
	public static void main(String[] args) {
		var bbC = Zeze.Serialize.ByteBuffer.Allocate();
		try (var zstdC = ZstdFactory.newCompressStream(256, 1, 10)) {
			var src = new byte[256];
			for (int i = 0; i < 256; i++)
				src[i] = (byte)i;

			zstdC.compress(src, 0, src.length, bbC);
			System.out.format("bbC.WriteIndex: %d\n", bbC.WriteIndex);

			zstdC.flush(bbC);
			System.out.format("bbC.WriteIndex: %d\n", bbC.WriteIndex);

			zstdC.compress(src, 0, src.length, bbC);
			System.out.format("bbC.WriteIndex: %d\n", bbC.WriteIndex);

			zstdC.flush(bbC);
			System.out.format("bbC.WriteIndex: %d\n", bbC.WriteIndex);
		}

		var bbD = Zeze.Serialize.ByteBuffer.Allocate();
		try (var zstdD = ZstdFactory.newDecompressStream()) {
			int middleIndex = (bbC.ReadIndex + bbC.WriteIndex) / 2;

			zstdD.decompress(bbC.Bytes, bbC.ReadIndex, middleIndex, bbD);
			System.out.format("bbD.WriteIndex: %d\n", bbD.WriteIndex);

			zstdD.decompress(bbC.Bytes, middleIndex, bbC.WriteIndex, bbD);
			System.out.format("bbD.WriteIndex: %d\n", bbD.WriteIndex);

			for (int i = 0; i < 256; i++) {
				if ((bbD.Bytes[i] & 0xff) != i)
					throw new AssertionError();
			}
			System.out.println("check OK!");
		}
	}
}
