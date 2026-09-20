package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Meta1;
import Zeze.Transaction.Log;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public class LogBeanKey<T extends Serializable> extends Log {
	private final @NotNull Meta1<T> meta;
	private final VarHandle vh;
	public T value;

	// 事务修改过程中不需要Factory。
	// 不变量（TL1-F1 裁决固化）：BeanKey 恒为 final 类（生成器 BeanKeyFormatter 无条件产出
	// public final class），故 value.getClass() 恒等于其声明类，按运行时类建 meta/typeId 与
	// 读端按声明类注册的解码工厂（History/Helper.registerLogBeanKey）天然对称，勿改用子类实例。
	@SuppressWarnings("unchecked")
	public LogBeanKey(Bean belong, int varId, VarHandle vh, @NotNull T value) {
		super(belong, varId);
		meta = Meta1.getBeanMeta((Class<T>)value.getClass());
		this.vh = vh;
		this.value = value;
	}

	public LogBeanKey(int varId, @NotNull Class<T> beanClass) {
		super(null, varId);
		meta = Meta1.getBeanMeta(beanClass);
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
		vh.set(getBelong(), value);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		value.encode(bb);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(@NotNull IByteBuffer bb) {
		try {
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
