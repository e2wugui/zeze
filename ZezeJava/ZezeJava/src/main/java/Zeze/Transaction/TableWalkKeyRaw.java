package Zeze.Transaction;


/** 表键遍历回调接口（原始字节键）：handle 返回 true 继续遍历、false 中断。 */
@FunctionalInterface
public interface TableWalkKeyRaw {
	boolean handle(byte [] key) throws Exception;
}
