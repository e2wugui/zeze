package Zeze.Transaction;

import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;

public abstract class Data implements Serializable {
	private transient int variableId;

	public Data() {
	}

	public Data(int varId) {
		variableId = varId;
	}

	public final int variableId() {
		return variableId;
	}

	// 这个方法应该仅用于内部。
	public final void variableId(int value) {
		variableId = value;
	}

	// 必须兼容旧的Bean，
	public long typeId() {
		var clsName = getClass().getName();
		return Bean.hash64(clsName, clsName.length() - 4); // 4 == "Data".length()
	}

	public abstract void assign(@NotNull Bean b); // 用于DynamicBeanData.assign(DynamicBean);

	public abstract @NotNull Bean toBean();

	public abstract @NotNull Data copy();

	public void buildString(@NotNull StringBuilder sb, int level) {
	}
}
