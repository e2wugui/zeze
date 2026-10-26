package Zeze.Transaction.Collections;

import java.util.ArrayList;
import java.util.function.Function;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** List1 payload: borrows the final ordered operations for synchronous encoding and replay. */
public class LogList1<V> extends LogBean {
	public static final class OpLog<V> {
		public static final int OP_MODIFY = 0; // op+index+value
		public static final int OP_ADD = 1;    // op+index+value
		public static final int OP_REMOVE = 2; // op+index
		public static final int OP_CLEAR = 3;  // op

		public final int op;
		public final int index;
		public final @Nullable V value;

		OpLog(int op, int index, @Nullable V value) {
			this.op = op;
			this.index = index;
			this.value = value;
		}

		static <V> OpLog<V> decode(IByteBuffer bb, @Nullable Function<IByteBuffer, V> valueDecoder) {
			int op = bb.ReadUInt();
			if (op < OP_MODIFY || op > OP_CLEAR)
				throw new IllegalStateException("invalid list operation: " + Integer.toUnsignedString(op));
			int index = op < OP_CLEAR ? bb.ReadUIntPositive() : 0;
			// A missing operation value does not make the list element type nullable.
			//noinspection DataFlowIssue,Convert2Diamond
			return new OpLog<V>(op, index, op < OP_REMOVE ? valueDecoder.apply(bb) : null);
		}

		@Override
		public String toString() {
			return "{" + op + ',' + index + ',' + value + '}';
		}
	}

	protected final Meta1<V> meta;
	protected final ArrayList<OpLog<V>> opLogs;

	public LogList1(int varId, @Nullable Bean source, Meta1<V> meta, @Nullable ArrayList<OpLog<V>> operations) {
		super(varId, source);
		this.meta = meta;
		opLogs = operations != null ? operations : new ArrayList<>();
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	public final ArrayList<OpLog<V>> getOpLogs() {
		return opLogs;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var encoder = meta.valueEncoder;
		bb.WriteUInt(opLogs.size());
		for (var opLog : opLogs) {
			bb.WriteUInt(opLog.op);
			if (opLog.op < OpLog.OP_CLEAR) {
				bb.WriteUInt(opLog.index);
				if (opLog.op < OpLog.OP_REMOVE) {
					//noinspection DataFlowIssue
					encoder.accept(bb, opLog.value);
				}
			}
		}
	}

	private void read(IByteBuffer bb) {
		var decoder = meta.valueDecoder;
		opLogs.clear();
		for (var logSize = bb.ReadUIntPositive(); --logSize >= 0; )
			opLogs.add(OpLog.decode(bb, decoder));
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" opLogs:");
		ByteBuffer.BuildSortedString(sb, opLogs);
		return sb.toString();
	}

	public static <V> LogList1<V> decode(int varId, Meta1<V> meta, IByteBuffer bb) {
		var log = new LogList1<>(varId, null, meta, null);
		log.read(bb);
		return log;
	}
}
