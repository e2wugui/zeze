package Zeze.Transaction.Collections;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** Encodable Map1 change payload, independent from transaction state. */
public class LogMap1<K, V> extends LogBean {
	protected final Map1Meta<K, V> meta;
	/** 结构增量单容器：value 非 null 为替换项，null 为删除标记（1系值域非null，tombstone无歧义）。 */
	private @Nullable HashMap<K, @Nullable V> modified;

	public LogMap1(int varId, @Nullable Bean source, Map1Meta<K, V> meta, @Nullable HashMap<K, @Nullable V> modified) {
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

	public final HashMap<K, @Nullable V> getModified() {
		return modified != null ? modified : (modified = new HashMap<>());
	}

	@Override
	public void encode(ByteBuffer bb) {
		Map<K, @Nullable V> m = modified != null ? modified : Collections.emptyMap();
		var keyEncoder = meta.keyEncoder;
		var valueEncoder = meta.valueCodec.encoder;
		// 两遍split编码：线格式与replaced/removed双容器时代逐字节一致（两个size前缀段），
		// 旧journal回放与混版本集群无感；非null=替换段(键+值)，null=删除段(仅键)。
		int replacedCount = 0;
		for (var e : m.entrySet())
			if (e.getValue() != null)
				replacedCount++;
		bb.WriteUInt(replacedCount);
		for (var e : m.entrySet()) {
			var v = e.getValue();
			if (v != null) {
				keyEncoder.accept(bb, e.getKey());
				valueEncoder.accept(bb, v);
			}
		}

		bb.WriteUInt(m.size() - replacedCount);
		for (var e : m.entrySet())
			if (e.getValue() == null)
				keyEncoder.accept(bb, e.getKey());
	}

	private void read(IByteBuffer bb) {
		var keyDecoder = meta.keyDecoder;
		var valueDecoder = meta.valueCodec.decoder;
		var m = getModified();
		for (int i = bb.ReadUIntPositive(); i > 0; --i)
			m.put(keyDecoder.apply(bb), valueDecoder.apply(bb));

		for (int i = bb.ReadUIntPositive(); i > 0; --i)
			m.put(keyDecoder.apply(bb), null);
	}

	public static <K, V> LogMap1<K, V> decode(int varId, Map1Meta<K, V> meta, IByteBuffer bb) {
		var log = new LogMap1<>(varId, null, meta, null);
		log.read(bb);
		return log;
	}
}
