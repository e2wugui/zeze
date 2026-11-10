package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.DynamicField;
import Zeze.Transaction.LogRegistry;
import Zeze.Transaction.LogDynamicValue;
import Zeze.Util.IntHashMap;
import org.jspecify.annotations.Nullable;

/** Bean 自身的日志：聚合各成员变量的子日志，按 variableId 索引，随事务收集向上传递。 */
public class LogBean extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Collections.LogBean");
	private static final int NESTED_BEAN = 0;
	private static final int NESTED_DYNAMIC = 1;

	@Nullable DynamicField dynamicField() {
		return null;
	}

	private final @Nullable Bean self;
	private @Nullable IntHashMap<Log> variables;

	public LogBean(int varId, @Nullable Bean self) {
		super(varId);
		this.self = self;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	public final @Nullable IntHashMap<Log> getVariables() {
		return variables;
	}

	public final IntHashMap<Log> getVariablesOrNew() {
		var variables = this.variables;
		if (variables == null)
			this.variables = variables = new IntHashMap<>();
		return variables;
	}

	public final @Nullable Bean getThis() {
		return self;
	}

	// LogBean仅在_final_commit的Collect过程中创建，不会参与Savepoint。

	@Override
	public void encode(ByteBuffer bb) {
		var vars = variables;
		if (vars != null) {
			bb.WriteUInt(vars.size());
			for (var it = vars.iterator(); it.moveToNext(); ) {
				Log log = it.value();
				var field = log instanceof LogBean beanLog ? beanLog.dynamicField() : null;
				LogRegistry.encodeChildHeader(bb, log, field);
				log.encode(bb);
			}
		} else
			bb.WriteUInt(0);
	}

	private void read(IByteBuffer bb) {
		int n = bb.ReadUIntPositive();
		if (n > 0) {
			var variables = getVariablesOrNew();
			variables.clear();
			for (; n > 0; --n) {
				int typeId = bb.ReadInt4();
				int varId = bb.ReadUInt();
				Log log = LogRegistry.decode(typeId, varId, bb);
				variables.put(varId, log);
			}
		} else if (variables != null)
			variables.clear();
	}

	// 仅发生在事务执行期间。decode-Apply不会执行到这里。

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildSortedString(sb, variables);
		return sb.toString();
	}

	public static void encodeLogBean(ByteBuffer bb, LogBean logBean) {
		bb.WriteByte(logBean instanceof LogDynamicValue ? NESTED_DYNAMIC : NESTED_BEAN);
		logBean.encode(bb);
	}

	public static LogBean decodeLogBean(IByteBuffer bb) {
		int tag = bb.ReadByte();
		return switch (tag) {
			case NESTED_BEAN -> decode(0, bb);
			case NESTED_DYNAMIC -> LogDynamicValue.decode(0, bb);
			default -> throw new IllegalArgumentException("unknown nested log tag=" + tag);
		};
	}

	/** Return whether the node must be attached to its parent. No upward traversal here. */
	public boolean addChild(Bean source, Log child) {
		return getVariablesOrNew().putIfAbsent(child.getVariableId(), child) == null;
	}

	public static LogBean decode(int varId, IByteBuffer bb) {
		var log = new LogBean(varId, null);
		log.read(bb);
		return log;
	}

	/** Release temporary construction indexes after the whole tree has been collected. */
	public void finishCollect() {
	}
}
