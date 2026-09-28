package Zeze.Transaction;

import java.util.ArrayList;
import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;

/** 可作 key 使用的 Bean 接口：提供变量元信息供 HotDistribute 反射查询。 */
public interface BeanKey extends Serializable {
	default @NotNull ArrayList<BVariable.Data> variables() {
		return new ArrayList<>();
	}
}
