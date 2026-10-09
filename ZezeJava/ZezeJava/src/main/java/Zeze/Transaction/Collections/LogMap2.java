package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

/** PMap2 的变更日志：在 replaced/removed 增量之上另记 changed（值 Bean 的原位修改日志，按 key 关联）。 */
public class LogMap2<K, V extends Bean> extends LogMap1<K, V> {
	private final Set<LogBean> changed = new HashSet<>(); // changed V logs. using in collect.
	private final HashMap<K, LogBean> changedWithKey = new HashMap<>(); // changed with key. using in encode/decode followerApply
	private boolean built; // changedWithKey 已构建（encode/decode/mergeChangedToReplaced 触发）
	private boolean decoded; // 解码后的 changedWithKey 是完整日志，不能从无源 Bean 的 changed 重建。
	// mergeChangedToReplaced 的已合并标志，独立于 built——History 开启时 collect 阶段
	// encode 先行置 built=true，监听器合并若复用 built 会被短路成 no-op，增量通知丢失原位修改。
	private boolean merged;

	public LogMap2(Bean belong, int varId, Bean self, @NotNull org.pcollections.PMap<K, V> value,
				   @NotNull Meta2<K, V> meta) {
		super(belong, varId, self, value, meta);
	}

	public final @NotNull Set<LogBean> getChanged() {
		return changed;
	}

	public final @NotNull HashMap<K, LogBean> getChangedWithKey() {
		return changedWithKey;
	}

	@Override
	public @NotNull Log beginSavepoint() {
		return new LogMap2<>(getBelong(), getVariableId(), getThis(), getValue(), meta);
	}

	public boolean buildChangedWithKey() {
		if (!built) {
			built = true;
			for (var c : changed) {
				@SuppressWarnings("unchecked")
				K k = (K)c.getThis().mapKey();
				if (!getReplaced().containsKey(k) // 新增的值是最新的，它的changed忽略。
						&& !getRemoved().contains(k) // 删除的值不用管了，它的changed忽略。
						&& c.getThis() == getValue().get(k) // 必须是key当前值本身：put覆盖后旧bean不
						// detach、mapKey不清，陈旧引用的字段日志不得应用到覆盖后的新值上。
				)
					changedWithKey.put(k, c);
			}
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public void mergeChangedToReplaced() {
		// leader-only：解码态的changed为空、changedWithKey才是完整回放日志，merge会把
		// 解码重建的bean塞进replaced——语义未定义（增量丢失/错挂）且无报错，显式拒绝。
		if (decoded)
			throw new IllegalStateException("mergeChangedToReplaced on decoded log (leader-only)");
		if (!merged) {
			merged = true;
			buildChangedWithKey(); // encode 先行时 changedWithKey 已构建（built==true），直接复用
			for (var e : changedWithKey.entrySet())
				getReplaced().put(e.getKey(), (V)e.getValue().getThis());
		}
	}

	@Override
	protected boolean isValueChanged(V oldValue, V newValue) {
		// 2系值可变，按身份判定（1系的equals过滤只对不可变值成立）：原位修改（changed按
		// 身份关联当前值）后再putAll一个与修改后内容equals相等的新实例时，equals过滤
		// 不记replaced，changed又因旧bean非当前值被丢弃——该键增量整体丢失，
		// follower/History回放停在旧值。身份不同即记replaced，编码携带完整新值。
		return oldValue != newValue;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		if (!decoded) {
			built = false;
			changedWithKey.clear();
			buildChangedWithKey();
		}

		bb.WriteUInt(changedWithKey.size());
		var keyEncoder = meta.keyEncoder;
		for (var e : changedWithKey.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			encodeLogBean(bb, e.getValue());
		}

		bb.WriteUInt(getReplaced().size());
		for (var e : getReplaced().entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			e.getValue().encode(bb);
		}
		bb.WriteUInt(getRemoved().size());
		for (K k : getRemoved())
			keyEncoder.accept(bb, k);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		changed.clear();
		merged = false;
		changedWithKey.clear();
		var keyDecoder = meta.keyDecoder;
		for (int i = bb.ReadUInt(); i > 0; i--) {
			K k = keyDecoder.apply(bb);
			changedWithKey.put(k, decodeLogBean(bb));
		}
		built = true;

		getReplaced().clear();
		try {
			for (int i = bb.ReadUInt(); i > 0; i--) {
				K k = keyDecoder.apply(bb);
				@SuppressWarnings("unchecked")
				V v = (V)meta.valueFactory.invoke();
				v.decode(bb);
				getReplaced().put(k, v);
			}
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
		getRemoved().clear();
		for (int i = bb.ReadUInt(); i > 0; i--)
			getRemoved().add(keyDecoder.apply(bb));
		decoded = true;
	}

	@Override
	public void collect(@NotNull Changes changes, @NotNull Bean recent, @NotNull Log vlog) {
		if (changed.add((LogBean)vlog))
			changes.collect(recent, this);
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		sb.append(" replaced:");
		ByteBuffer.BuildSortedString(sb, getReplaced());
		sb.append(" removed:");
		ByteBuffer.BuildSortedString(sb, getRemoved());
		sb.append(" changed:");
		if (built)
			ByteBuffer.BuildSortedString(sb, changedWithKey);
		else
			ByteBuffer.BuildSortedString(sb, changed);
		return sb.toString();
	}
}
