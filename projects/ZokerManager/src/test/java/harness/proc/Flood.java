package harness.proc;

/**
 * 向 stdout 持续写出约 256KB 后以 0 退出——探"无人消费的输出管道写满 64KB 缓冲后
 * 阻塞、进程永不退出"的修复形态（替代 cmd for /l 循环 echo）。
 */
public final class Flood {
	public static void main(String[] args) throws Exception {
		var line = "0123456789012345678901234567890123456789012345678901234567890123456789012345678\r\n";
		var out = System.out;
		for (int i = 0; i < 3200; i++) // 3200 × 81B ≈ 256KB ≈ 4×64KB 管道缓冲
			out.write(line.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
		out.flush();
	}

	private Flood() {
	}
}
