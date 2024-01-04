package Zeze.World.Astar;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

/**
 * 使用多个MappedByteBuffer映射同一个文件。
 * HugeMapped 使用long类型的 offset, size。
 * 使得整个表达空间扩展到long长度。
 */
public class HugeMappedFile implements Closeable {
	private final File file;
	private final FileChannel channel;
	private final long fileSize;
	private final HashMap<Long, MappedByteBuffer> mapped = new HashMap<>();

	public HugeMappedFile(File file) throws Exception {
		this.file = file;
		this.channel = new RandomAccessFile(file, "rw").getChannel();
		this.fileSize = channel.size();
	}

	public File getFile() {
		return file;
	}

	public long getFileSize() {
		return fileSize;
	}

	public byte get(long index) {
		var mapped = mapped(index);
		var blockIndex = (int)index & BlockMask;
		return mapped.get(blockIndex);
	}

	public void set(long index, byte b) {
		var mapped = mapped(index);
		var blockIndex = (int)index & BlockMask;
		mapped.put(blockIndex, b);
	}

	public void set(long index, byte[] bytes) {
		var mapped = mapped(index);
		var blockIndex = (int)index & BlockMask;
		// todo jdk 13 以后改成 mapped.put(blockIndex, bytes);
		mapped.position(blockIndex);
		mapped.put(bytes);
	}

	private static final int MaxBlockSize = 1 << 30; // 1G
	private static final int BlockMask = MaxBlockSize - 1;

	private MappedByteBuffer mapped(long offset) {
		if (offset < 0 || offset >= this.fileSize)
			throw new RuntimeException("offset is out of range.");

		var blockIndex = offset / MaxBlockSize;
		return mapped.computeIfAbsent(blockIndex, __ -> {
			try {
				// 每一个mmap块的size为MaxBlockSize，除了最后一块。
				var position = blockIndex * MaxBlockSize; // 当前这一块映射的起始位置。
				var wishEndSize = position + MaxBlockSize; // 当前这一块的【期望】结束位置。
				var size = wishEndSize < this.fileSize ? MaxBlockSize : (this.fileSize - position);
				return channel.map(FileChannel.MapMode.READ_WRITE, position, size);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void close() throws IOException {
		for (var m : mapped.values())
			m.force();
		this.mapped.clear();
		channel.close();
	}
}