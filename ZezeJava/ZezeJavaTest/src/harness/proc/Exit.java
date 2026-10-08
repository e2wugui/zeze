package harness.proc;

/**
 * 立即以给定退出码退出（替代 cmd /c exit N 形态）。用法：javaw -cp &lt;cp&gt; harness.proc.Exit 7
 */
public final class Exit {
	public static void main(String[] args) {
		System.exit(args.length > 0 ? Integer.parseInt(args[0]) : 0);
	}

	private Exit() {
	}
}
