package Zeze.Util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 单文件原子替换原语：写temp→fsync→原子rename→目录fsync（POSIX尽力）。
 * 任何崩溃点留下的都是完整旧版或完整新版（old-or-new）；Windows无目录fsync，物理
 * 保证按OS尽力。同一目标的并发写各自原子、终态其一（要顺序的调用方自行串行化）；
 * 跨进程互斥由调用方加锁文件负责。
 */
public final class AtomicFileWriter {
	private AtomicFileWriter() {
	}

	/**
	 * 流式写入口（大文件不得走内存全量路线）。目标父目录必须已存在。
	 * 句柄三态：close=换版生效；abort=丢弃temp不算失败；都未调而进程死亡=留.tmp，
	 * tmp皆垃圾，由调用方自行启动清扫。
	 */
	public static @NotNull AtomicOutputFile openOutput(@NotNull Path target) throws IOException {
		return new AtomicOutputFile(target);
	}

	/** 便捷重载：全量内容一次换版（小文件用）。 */
	public static void replace(@NotNull Path target, byte[] content) throws IOException {
		try (AtomicOutputFile out = openOutput(target)) {
			out.write(content);
		}
	}

	/** 对已写完的文件补齐fsync（分块接收等按路径写的落盘收口）：force必须先于任何rename。 */
	public static void fsync(@NotNull Path file) throws IOException {
		try (var channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
			channel.force(true);
		}
	}
}
