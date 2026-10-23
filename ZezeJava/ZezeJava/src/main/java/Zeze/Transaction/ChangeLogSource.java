package Zeze.Transaction;

import Zeze.Transaction.Collections.LogBean;

/** 事务日志中需要导出独立消费载荷的角色；节点缓存和向上挂接统一由 Changes 管理。 */
public interface ChangeLogSource {
	Bean getSourceBean();

	/** 仅创建载荷，不查询当前事务、不缓存，也不向上收集。 */
	LogBean createChangeLog();
}
