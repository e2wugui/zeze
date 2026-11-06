package Zeze.Transaction.Collections;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

/** Set1 payload: borrows the final modified set for synchronous encoding and replay. */
public class LogSet1<V> extends LogBean {
	/**
	 * 加入项的哑值（同 HashSet 底层 HashMap 的 PRESENT 惯例）：set 无值语义，
	 * value 槽只作判别位——非 null（PRESENT）为加入，null 为删除标记。
	 */
	public static final Object PRESENT = new Object();

	protected final Meta1<V> meta;
	private @Nullable HashMap<V, Object> modified;

	public LogSet1(int varId, @Nullable Bean source, Meta1<V> meta, @Nullable HashMap<V, Object> modified) {
		super(varId, source);
		this.meta = meta;
		this.modified = modified;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	public final HashMap<V, Object> getModified() {
		return modified != null ? modified : (modified = new HashMap<>());
	}

	@Override
	public void encode(ByteBuffer bb) {
		var encoder = meta.valueEncoder;
		Map<V, Object> m = modified != null ? modified : Collections.emptyMap();
		// 两遍split编码（同LogMap1）：线格式与added/removed双容器时代逐字节一致。
		int addedCount = 0;
		for (var e : m.entrySet())
			if (e.getValue() != null)
				addedCount++;
		bb.WriteUInt(addedCount);
		for (var e : m.entrySet())
			if (e.getValue() != null)
				encoder.accept(bb, e.getKey());

		bb.WriteUInt(m.size() - addedCount);
		for (var e : m.entrySet())
			if (e.getValue() == null)
				encoder.accept(bb, e.getKey());
	}

	private void read(IByteBuffer bb) {
		var decoder = meta.valueDecoder;
		var m = getModified();
		for (int i = bb.ReadUIntPositive(); i > 0; i--)
			m.put(decoder.apply(bb), PRESENT);

		for (int i = bb.ReadUIntPositive(); i > 0; i--)
			m.put(decoder.apply(bb), null);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" modified:");
		ByteBuffer.BuildSortedString(sb, modified);
		return sb.toString();
	}

	public static <V> LogSet1<V> decode(int varId, Meta1<V> meta, IByteBuffer bb) {
		var log = new LogSet1<>(varId, null, meta, null);
		log.read(bb);
		return log;
	}
}
