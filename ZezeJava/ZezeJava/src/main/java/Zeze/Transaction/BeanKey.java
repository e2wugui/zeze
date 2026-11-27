package Zeze.Transaction;

import java.util.ArrayList;
import Zeze.Serialize.Encodable;

/** 可作 key 使用的 Bean 接口：提供变量元信息供运行时反射查询。 */
public interface BeanKey extends Encodable {
	default ArrayList<VariableMeta> variables() {
		return new ArrayList<>();
	}
}
