package Zeze.Transaction;


/**
 * 表数据变更监听器：记录提交后按 key 与变更记录触发回调。
 * 所有监听器必须在任何业务事务开始前完成注册，业务运行期间不支持新增监听器。
 * 集合在事务首次写入时按消费者选择是否采集增量；注册时机由应用启动流程保证。
 */
@FunctionalInterface
public interface ChangeListener {
	void OnChanged(Object key, Changes.Record r);
}
