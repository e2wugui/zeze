package Zeze.Transaction;

import Zeze.Serialize.Serializable;

public abstract class Data implements Serializable {
	// 必须兼容旧的Bean，
	public long typeId() {
		var clsName = getClass().getName();
		return Bean.hash64(clsName.substring(0, clsName.length() - 4));
	}

	public abstract Data copy();

	private transient int variableId;

	public final int variableId() {
		return variableId;
	}

	// 这个方法应该仅用于内部。
	public final void variableId(int value) {
		variableId = value;
	}

	public Data() {

	}

	public Data(int varId) {
		variableId = varId;
	}

	public void buildString(StringBuilder sb, int level) {

	}

	public abstract void assign(Bean b); // 用于DynamicBeanData.assign(DynamicBean);
}
