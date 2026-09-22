package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.BeanKeyMeta;
import Zeze.Transaction.Log;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

/** BeanKey（值语义）字段的修改日志：整体替换，typeId 按值类型派生。 */
public class LogBeanKey<T extends Serializable> extends Log {
	private final @NotNull BeanKeyMeta<T> meta;
	private final VarHandle vh;
	public T value;

	// meta 由两端各自按声明类预建（写端：生成器meta_静态字段；
	// 读端：History/Helper.registerLogBeanKey 注册工厂），天然对称，勿改用
	// 子类实例构造本Log（生成器保证BeanKey无子类）。
	public LogBeanKey(Bean belong, int varId, VarHandle vh, @NotNull BeanKeyMeta<T> meta, @NotNull T value) {
		super(belong, varId);
		this.meta = meta;
		this.vh = vh;
		this.value = value;
	}

	public LogBeanKey(int varId, @NotNull BeanKeyMeta<T> meta) { // for decode
		super(null, varId);
		this.meta = meta;
		vh = null;
	}

	@Override
	public @NotNull Category category() {
		return Category.eHistory;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public @NotNull String getTypeName() {
		return meta.name;
	}

	@Override
	public void commit() {
		//noinspection DataFlowIssue
		vh.set(getBelong(), value);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		value.encode(bb);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		try {
			//noinspection unchecked
			value = (T)meta.valueFactory.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
		value.decode(bb);
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
