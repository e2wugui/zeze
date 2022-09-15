package Zeze.Raft.RocksRaft;

import java.util.concurrent.atomic.AtomicLong;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Str;

public abstract class Bean implements Serializable {
	public static final int ObjectIdStep = 4096;
	public static final int MaxVariableId = ObjectIdStep - 1;
	private static final AtomicLong objectIdGenerator = new AtomicLong();

	private transient final long objectId = objectIdGenerator.addAndGet(ObjectIdStep);
	private transient Bean parent;
	public transient int VariableId;
	private transient Record.RootInfo rootInfo;

	public Bean() {
	}

	public Bean(int varId) {
		VariableId = varId;
	}

	public final long objectId() {
		return objectId;
	}

	public final Bean parent() {
		return parent;
	}

	public final int variableId() {
		return VariableId;
	}

	public final void variableId(int value) {
		VariableId = value;
	}

	public final Record.RootInfo rootInfo() {
		return rootInfo;
	}

	public final boolean isManaged() {
		return rootInfo != null;
	}

	public final TableKey tableKey() {
		return rootInfo != null ? rootInfo.getTableKey() : null;
	}

	public Object mapKey() {
		throw new UnsupportedOperationException();
	}

	public void mapKey(Object mapKey) {
		throw new UnsupportedOperationException();
	}

	public final void InitRootInfo(Record.RootInfo rootInfo, Bean parent) {
		if (isManaged())
			throw new Zeze.Transaction.HasManagedException();
		this.rootInfo = rootInfo;
		this.parent = parent;
		InitChildrenRootInfo(rootInfo);
	}

	// 用在第一次加载Bean时，需要初始化它的root
	protected abstract void InitChildrenRootInfo(Record.RootInfo root);

	@Override
	public abstract void Encode(ByteBuffer bb);

	@Override
	public abstract void Decode(ByteBuffer bb);

	public abstract Bean CopyBean();

	public LogBean CreateLogBean() {
		LogBean tempVar = new LogBean();
		tempVar.setBelong(parent);
		tempVar.setThis(this);
		tempVar.setVariableId(VariableId);
		return tempVar;
	}

	public abstract void FollowerApply(Log log);

	public abstract void LeaderApplyNoRecursive(Log log);

	public long typeId() {
		return Zeze.Transaction.Bean.hash64(getClass().getName());
	}

	public void BuildString(StringBuilder sb, int level) {
		sb.append(Str.indent(level)).append("{}").append(System.lineSeparator());
	}
}
