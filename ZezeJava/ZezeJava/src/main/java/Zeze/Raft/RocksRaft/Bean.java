package Zeze.Raft.RocksRaft;

import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.HasManagedException;
import Zeze.Util.Str;

/**
 * RocksRaft 的 Bean 基类：objectId/rootInfo 挂接管理、修改日志的生成与 leader/follower 应用接口。
 */
public abstract class Bean implements Serializable {
	public static final int OBJECT_ID_STEP = 4096;
	public static final int MAX_VARIABLE_ID = OBJECT_ID_STEP - 1;
	private static final AtomicLong objectIdGen = new AtomicLong();

	private transient final long objectId = objectIdGen.addAndGet(OBJECT_ID_STEP);
	private transient Record.@Nullable RootInfo rootInfo;
	private transient @Nullable Bean parent;
	private transient int variableId;

	public Bean() {
	}

	public Bean(int varId) {
		variableId = varId;
	}

	public final long objectId() {
		return objectId;
	}

	public final @Nullable Bean parent() {
		return parent;
	}

	public final int variableId() {
		return variableId;
	}

	public final void variableId(int value) {
		variableId = value;
	}

	public final Record.@Nullable RootInfo rootInfo() {
		return rootInfo;
	}

	public final boolean isManaged() {
		return rootInfo != null;
	}

	public final @Nullable TableKey tableKey() {
		return rootInfo != null ? rootInfo.getTableKey() : null;
	}

	public @Nullable Object mapKey() {
		throw new UnsupportedOperationException();
	}

	public void mapKey(Object mapKey) {
		throw new UnsupportedOperationException();
	}

	public final void initRootInfo(Record.@Nullable RootInfo rootInfo, @Nullable Bean parent) {
		if (isManaged())
			throw new HasManagedException();
		this.rootInfo = rootInfo;
		this.parent = parent;
		initChildrenRootInfo(rootInfo);
	}

	// 用在第一次加载Bean时，需要初始化它的root
	protected abstract void initChildrenRootInfo(Record.@Nullable RootInfo root);

	@Override
	public abstract void encode(ByteBuffer bb);

	@Override
	public abstract void decode(IByteBuffer bb);

	public abstract Bean copy();

	public LogBean createLogBean() {
		LogBean tempVar = new LogBean();
		tempVar.setBelong(parent);
		tempVar.setThis(this);
		tempVar.setVariableId(variableId);
		return tempVar;
	}

	public abstract void followerApply(Log log);

	public abstract void leaderApplyNoRecursive(Log log);

	@Override
	public long typeId() {
		return Zeze.Transaction.Bean.hash64(getClass().getName());
	}

	public void buildString(StringBuilder sb, int level) {
		sb.append(Str.indent(level)).append("{}\n");
	}
}
