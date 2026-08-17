package Zeze.Util;

import Zeze.Transaction.DispatchMode;
import org.jetbrains.annotations.Nullable;

/**
 * TaskSpec 实现类的共同可选字段。
 * timeout 使用 -1 作为哨兵值：终结方法执行时才读取 Task.defaultTimeout，
 * 保证与旧的在调用时读默认值语义 100% 等价。
 */
abstract class AbstractTaskSpec {
	@Nullable String name;
	@Nullable DispatchMode mode; // null 等同 DispatchMode.Normal
	long timeout = -1; // 哨兵值：&lt;0 表示未设置，终结方法内取 Task.defaultTimeout

	long timeoutOrDefault() {
		return timeout < 0 ? Task.defaultTimeout : timeout;
	}
}
