package Zeze.Transaction.Collections;

import java.util.Objects;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Transaction.LogDynamic;
import Zeze.Transaction.Savepoint;
import Zeze.Util.IntHashMap;
import org.jspecify.annotations.Nullable;

/** Bean 自身的日志：聚合各成员变量的子日志，按 variableId 索引，随事务收集向上传递。 */
public class LogBean extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Collections.LogBean");

	private final @Nullable Bean self;
	private @Nullable IntHashMap<Log> variables;

	public LogBean(@Nullable Bean belong, int varId, @Nullable Bean self) {
		super(belong, varId);
		this.self = self;
	}

	@Override
	public Category category() {
		return Category.eHistory;
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

	@Override
	public void commit() {
		throw new UnsupportedOperationException();
	}

	// LogBean仅在_final_commit的Collect过程中创建，不会参与Savepoint。
	@Override
	public Log beginSavepoint() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encode(ByteBuffer bb) {
		var vars = variables;
		if (vars != null) {
			bb.WriteUInt(vars.size());
			for (var it = vars.iterator(); it.moveToNext(); ) {
				Log log = it.value();
				bb.WriteInt4(Objects.requireNonNull(log).getTypeId());
				bb.WriteUInt(log.getVariableId());
				log.encode(bb);
			}
		} else
			bb.WriteUInt(0);
	}

	@Override
	public void decode(IByteBuffer bb) {
		int n = bb.ReadUInt();
		if (n > 0) {
			var variables = getVariablesOrNew();
			variables.clear();
			for (; n > 0; --n) {
				int typeId = bb.ReadInt4();
				int varId = bb.ReadUInt();
				Log log = create(typeId, varId);
				log.decode(bb);
				variables.put(varId, log);
			}
		} else if (variables != null)
			variables.clear();
	}

	// 仅发生在事务执行期间。decode-Apply不会执行到这里。
	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		if (getVariablesOrNew().putIfAbsent(vlog.getVariableId(), vlog) == null)
			changes.collect(recent, this); // 向上传递
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildSortedString(sb, variables);
		return sb.toString();
	}

	public static void encodeLogBean(ByteBuffer bb, LogBean logBean) {
		// 使用byte，未来可能扩展其他LogBean子类。
		if (logBean instanceof LogDynamic)
			bb.WriteByte(1);
		else // 全部都是LogBean子类，只能else。
			bb.WriteByte(0);
		logBean.encode(bb);
	}

	public static LogBean decodeLogBean(IByteBuffer bb) {
		int type = bb.ReadByte();
		LogBean logBean = switch (type) {
			case 0 -> new LogBean(null, 0, null);
			case 1 -> new LogDynamic(null, 0, null);
			default -> throw new RuntimeException("unknown logBean subclass type=" + type);
		};
		logBean.decode(bb);
		return logBean;
	}
}
