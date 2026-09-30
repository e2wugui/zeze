package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

/**
 * 管理 Bean 的 Map 容器增量日志：结构操作记 putted/removed，值 bean 的字段修改记 changedWithKey，
 * encode 侧按最终 map 现值过滤陈旧条目。
 */
public class LogMap2<K, V extends Bean> extends LogMap1<K, V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogMap2<");

	private final Set<LogBean> changed = new HashSet<>(); // changed V logs. using in collect.
	private final HashMap<K, LogBean> changedWithKey = new HashMap<>(); // changed with key. using in encode/decode followerApply
	private final MethodHandle valueFactory;
	private boolean decoded; // 解码后的索引是完整日志，不从旧的源Map重新构建。

	public LogMap2(Class<K> keyClass, Class<V> valueClass) {
		super(Zeze.Transaction.Bean.hashLog(logTypeIdHead, keyClass, valueClass), keyClass, valueClass);
		valueFactory = Reflect.getDefaultConstructor(valueClass);
	}

	LogMap2(int typeId, SerializeHelper.CodecFuncs<K> keyCodecFuncs, MethodHandle valueFactory) {
		super(typeId, keyCodecFuncs, null);
		this.valueFactory = valueFactory;
	}

	public final Set<LogBean> getChanged() {
		return changed;
	}

	public final HashMap<K, LogBean> getChangedWithKey() {
		return changedWithKey;
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogMap2<K, V>(getTypeId(), keyCodecFuncs, valueFactory);
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void encode(@NotNull ByteBuffer bb) {
		if (!decoded && getValue() != null) {
			changedWithKey.clear(); // live日志重建最终视图；decode日志保留wire中的changed。
			for (var c : changed) {
				Object pkey = c.getThis().mapKey();
				// 第三条过滤对齐Transaction.Collections.LogMap2.buildChangedWithKey：
				// 编辑时bean已不在最终map（remove不detach，编辑已删bean的陈旧引用仍会被collect，
				// 其key来自更早事务，putted/removed两条拦不住），不过滤则follower侧必取到null；
				// 且必须是key当前值本身——put覆盖后旧bean同样不detach，陈旧引用的字段日志
				// 不得应用到覆盖后的新值上。
				//noinspection SuspiciousMethodCalls
				if (!getPutted().containsKey(pkey) && !getRemoved().contains(pkey)
						&& c.getThis() == getValue().get(pkey))
					changedWithKey.put((K)pkey, c);
			}
		}
		bb.WriteUInt(changedWithKey.size());
		var keyEncoder = keyCodecFuncs.encoder;
		for (var e : changedWithKey.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			e.getValue().encode(bb);
		}

		// putted/removed 手工编码（不走 super.encode）：putted 的 value 是 Bean，用 bean.encode 而非 codec。
		bb.WriteUInt(getPutted().size());
		for (var p : getPutted().entrySet()) {
			keyEncoder.accept(bb, p.getKey());
			p.getValue().encode(bb);
		}
		bb.WriteUInt(getRemoved().size());
		for (var r : getRemoved())
			keyEncoder.accept(bb, r);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(@NotNull IByteBuffer bb) {
		changed.clear();
		changedWithKey.clear();
		var keyDecoder = keyCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = keyDecoder.apply(bb);
			var value = new LogBean();
			value.decode(bb);
			changedWithKey.put(key, value);
		}

		// putted/removed 手工解码（不走 super.decode）：putted 的 value 是 Bean，经 valueFactory 构造后 decode。
		getPutted().clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var key = keyDecoder.apply(bb);
			V value;
			try {
				value = (V)valueFactory.invoke();
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
			value.decode(bb);
			getPutted().put(key, value);
		}
		getRemoved().clear();
		for (int i = bb.ReadUInt(); i > 0; i--)
			getRemoved().add(keyDecoder.apply(bb));
		decoded = true;
	}

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		if (changed.add((LogBean)vlog))
			changes.collect(recent, this);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" Putted:");
		ByteBuffer.BuildSortedString(sb, getPutted());
		sb.append(" Removed:");
		ByteBuffer.BuildSortedString(sb, getRemoved());
		sb.append(" Changed:");
		ByteBuffer.BuildSortedString(sb, changed);
		return sb.toString();
	}
}
