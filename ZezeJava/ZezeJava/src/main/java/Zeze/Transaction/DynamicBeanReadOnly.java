package Zeze.Transaction;


/** DynamicBean 的只读视图接口：仅暴露 typeId 与内部 bean，供外部无副作用访问。 */
public interface DynamicBeanReadOnly {
	long getTypeId();

	Bean getBean();
}
