package Zeze.Hot;

public class Distribute {
	public static void main(String [] args) throws Exception {
		// 搜索classes目录，自动识别Module并打包。
		// 每个Module打成两个包。一个interface，一个其他。
		// Module除外的打成一个包，不热更（可能有例外）。
		// 例外：
		// build zezex 的时候，redirect.class生成到build目录了，这个不是即时编译，仅在内存的吗？
		// 是不是zezex特殊处理了（好像是）。
	}
}
