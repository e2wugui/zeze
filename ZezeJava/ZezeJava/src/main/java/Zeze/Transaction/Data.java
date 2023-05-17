package Zeze.Transaction;

import Zeze.Serialize.Serializable;
import Zeze.Util.Str;
import org.jetbrains.annotations.NotNull;

public abstract class Data implements Serializable, Cloneable {
	// 必须兼容旧的Bean，
	public long typeId() {
		var clsName = getClass().getName();
		return Bean.hash64(clsName, clsName.length() - 4); // 4 == "Data".length()
	}

	public abstract void assign(@NotNull Bean b); // 用于DynamicBeanData.assign(DynamicBean);

	public abstract @NotNull Bean toBean();

	public abstract @NotNull Data copy();

	public void buildString(@NotNull StringBuilder sb, int level) {
		sb.append(Str.indent(level)).append('{').append(this).append('}');
	}

	@Override
	public Data clone() {
		try {
			return (Data)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
