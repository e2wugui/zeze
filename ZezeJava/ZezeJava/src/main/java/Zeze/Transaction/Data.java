package Zeze.Transaction;

import Zeze.Serialize.Serializable;
import Zeze.Util.Str;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

/** Bean 的纯数据形态：与 Bean 可互转（toBean/assign），用于脱离事务的快照拷贝与传输。 */
public abstract class Data implements Serializable, Cloneable {
	// 必须兼容旧的Bean，
	@Override
	public long typeId() {
		return toBean().typeId();
	}

	public abstract void assign(@NotNull Bean b); // 用于DynamicBeanData.assign(DynamicBean);

	public abstract @NotNull Bean toBean();

	public abstract void reset(); // 重置Data的所有字段

	public abstract @NotNull Data copy();

	public void buildString(@NotNull StringBuilder sb, int level) {
		sb.append(Str.indent(level)).append('{').append(this).append('}');
	}

	/**
	 * @return 浅拷贝. 深拷贝应该用copy方法
	 */
	@Override
	public @NotNull Data clone() {
		try {
			return (Data)super.clone();
		} catch (CloneNotSupportedException e) {
			throw Task.forceThrow(e);
		}
	}
}
