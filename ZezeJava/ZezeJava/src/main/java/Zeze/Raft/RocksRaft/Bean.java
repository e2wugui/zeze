package Zeze.Raft.RocksRaft;

import java.util.concurrent.atomic.AtomicLong;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Str;

public abstract class Bean implements Serializable {
	private static final AtomicLong ObjectIdGenerator = new AtomicLong();
	public static final int ObjectIdStep = 4096;
	public static final int MaxVariableId = ObjectIdStep - 1;

	private final long ObjectId = ObjectIdGenerator.addAndGet(ObjectIdStep);
	private Bean Parent;
	public int VariableId;
	private Record.RootInfo RootInfo;

	public Bean() {
	}

	public Bean(int varId) {
		VariableId = varId;
	}

	public final long getObjectId() {
		return ObjectId;
	}

	public final Bean getParent() {
		return Parent;
	}

	public final int getVariableId() {
		return VariableId;
	}

	public final void setVariableId(int value) {
		VariableId = value;
	}

	public final Record.RootInfo getRootInfo() {
		return RootInfo;
	}

	public final boolean isManaged() {
		return RootInfo != null;
	}

	public final TableKey getTableKey() {
		return RootInfo != null ? RootInfo.getTableKey() : null;
	}

	public Object getMapKey() {
		throw new UnsupportedOperationException();
	}

	public void setMapKey(Object mapKey) {
		throw new UnsupportedOperationException();
	}

	public final void InitRootInfo(Record.RootInfo rootInfo, Bean parent) {
		if (isManaged())
			throw new Zeze.Transaction.HasManagedException();
		RootInfo = rootInfo;
		Parent = parent;
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
		tempVar.setBelong(Parent);
		tempVar.setThis(this);
		tempVar.setVariableId(VariableId);
		return tempVar;
	}

	public abstract void FollowerApply(Log log);

	public abstract void LeaderApplyNoRecursive(Log log);

	public long getTypeId() {
		return Zeze.Transaction.Bean.Hash64(getClass().getName());
	}

	public void BuildString(StringBuilder sb, int level) {
		sb.append(Str.indent(level)).append("{}").append(System.lineSeparator());
	}
}
