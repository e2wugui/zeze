package Zeze.Transaction;


/** 表遍历回调接口（原始字节键值）：handle 返回 true 继续遍历、false 中断。 */
@FunctionalInterface
public interface TableWalkHandleRaw {
	boolean handle(byte [] key, byte [] value) throws Exception;
}
