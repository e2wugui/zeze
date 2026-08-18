package Zeze.Util;

import java.util.Objects;
import Zeze.Transaction.DispatchMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * OneByOneSpec 实现类的共同部分：零装箱 key 存储与共同可选字段。
 * int key 符号扩展后按位无损保存在 long rawKey 中，取回用 (int)rawKey；
 * 与旧 Execute 重载的重载绑定一致：字面量 int/long 绑定原生版本，显式包装类型走 Object 版（hashCode）。
 */
abstract class AbstractOneByOneSpec {
	static final byte KEY_OBJECT = 0;
	static final byte KEY_INT = 1;
	static final byte KEY_LONG = 2;

	final byte keyType;
	final Object objectKey; // 仅 KEY_OBJECT 有值
	final long rawKey; // KEY_INT 时低32位按位保存 int（符号扩展无损），KEY_LONG 时为 key 本身

	@Nullable String name;
	@Nullable DispatchMode mode; // null 等同 DispatchMode.Normal
	@Nullable Action0 cancel; // 队列 shutdown(true) 时对未执行任务的回调；TaskOneByOneByKey2 不支持

	AbstractOneByOneSpec(@NotNull Object key) {
		this.keyType = KEY_OBJECT;
		this.objectKey = Objects.requireNonNull(key);
		this.rawKey = 0;
	}

	AbstractOneByOneSpec(int key) {
		this.keyType = KEY_INT;
		this.objectKey = null;
		this.rawKey = key;
	}

	AbstractOneByOneSpec(long key) {
		this.keyType = KEY_LONG;
		this.objectKey = null;
		this.rawKey = key;
	}

	DispatchMode modeOrDefault() {
		return mode != null ? mode : DispatchMode.Normal;
	}

	/**
	 * 按 keyType 分发到 {@link TaskOneByOneBase} 的 protected execute(int/long/Object, task)，
	 * 与旧 Execute 重载的重载选择逐帧相同（避免装箱）。
	 */
	void executeByKey(@NotNull TaskOneByOneBase oneByOne, @NotNull TaskOneByOneQueue.Task task) {
		switch (keyType) {
		case KEY_INT:
			oneByOne.execute((int)rawKey, task);
			break;
		case KEY_LONG:
			oneByOne.execute(rawKey, task);
			break;
		default:
			assert objectKey != null;
			oneByOne.execute(objectKey, task);
			break;
		}
	}

	/**
	 * 计算 {@link TaskOneByOneByKey2} 的 int hashKey：
	 * int 直接返回 / long 转 Long.hashCode / Object 转 hashCode()，与 Key2 旧 long/Object 重载的委托一致。
	 */
	int hashKey() {
		return switch (keyType) {
			case KEY_INT -> (int)rawKey;
			case KEY_LONG -> Long.hashCode(rawKey);
			default -> {
				assert objectKey != null;
				yield objectKey.hashCode();
			}
		};
	}
}
