package TestLog4jQuery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

/**
 * 测试一些mmap特性。
 * 1. 如果mmap.append采用预分配页的方式减少重建mmap。最好在开头(mmap.array())保留一段空间记录长度信息，
 * 2. mmap没有关闭操作，只能依赖垃圾回收。
 * 3. mmap打开时会限制一些文件操作不能执行，比如channel.truncate.
 */
public class TestMmap {
	public final static int ePageSize = 16;
	public final static int ePageMask = ePageSize - 1;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Test
	public void testMmap() throws Exception {
		var file = new File("testMmapFile");
		new FileOutputStream(file).close(); // create or truncate 0
		var pExists = file.toPath();
		var pMmap = Path.of(file.getName() + ".mmap");
		if (!pMmap.toFile().exists())
			Files.createLink(pMmap, pExists);
		var fileMmap = pMmap.toFile();
		try (var channel = new RandomAccessFile(fileMmap, "rw").getChannel()) {
			final var fileSize = channel.size();
			if (fileSize > 0) {
				var dst = ByteBuffer.allocate((int)fileSize);
				channel.read(dst);
				dst.flip();
				while (dst.remaining() > 0) {
					System.out.print(dst.get());
					System.out.print(",");
				}
				System.out.println("------");
			}
			final var lastPageSize = (int)(fileSize & ePageMask);
			final var mapSize = fileSize + ePageSize - lastPageSize;
			final var mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, mapSize);
			mmap.position((int)fileSize);
			mmap.put((byte)1);
			mmap.put((byte)3);
			mmap.put((byte)5);
		}

		if (fileMmap.delete())
			System.out.println("delete link ok.");

		MappedByteBuffer last;
		try (var channel = new RandomAccessFile(fileMmap, "rw").getChannel()) {
			final var fileSize = channel.size();
			if (fileSize > 0) {
				var dst = ByteBuffer.allocate((int)fileSize);
				channel.read(dst);
				dst.flip();
				while (dst.remaining() > 0) {
					System.out.print(dst.get());
					System.out.print(",");
				}
				System.out.println("------");
			}
			final var lastPageSize = (int)(fileSize & ePageMask);
			final var mapSize = fileSize + ePageSize - lastPageSize;
			final var mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, mapSize);
			mmap.position((int)fileSize);
			mmap.put((byte)2);
			mmap.put((byte)4);
			mmap.put((byte)6);
			last = mmap;
		}

		try (var channel = new RandomAccessFile(fileMmap, "rw").getChannel()) {
			final var fileSize = channel.size();
			if (fileSize > 0) {
				var dst = ByteBuffer.allocate((int)fileSize);
				channel.read(dst);
				dst.flip();
				while (dst.remaining() > 0) {
					System.out.print(dst.get());
					System.out.print(",");
				}
				System.out.println("------");
			}
		}
		//if (file.renameTo(new File(file.getName() + ".rename")))
		//	System.out.println("RENAME OK!"); // 失败！mmap打开的时候不能rename，但是mmap打开的是link，所以可以。
		last.position(0);
		while (last.remaining() > 0) {
			System.out.print(last.get());
			System.out.print(",");
		}
		System.out.println("++++++");
	}
}
