package Zeze.Collections;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.DAG.BDAGNodeKey;
import Zeze.Transaction.Bean;

public class DAG<V extends Bean> {

	public boolean addNode(String id, V value) {
		var nodeIdKey = new BDAGNodeKey(name, id);
		return false;
	}

	/**
	 * 检查有向图合法
	 * 1. 无环
	 */
	public boolean isValid() {
		return isNoCycle();
	}

	public Module getModule() {
		return module;
	}

	public String getName() {
		return name;
	}

	public MethodHandle getValueConstructor() {
		return valueConstructor;
	}

	public int getNodeSize() {
		return nodeSize;
	}

	public static final BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static class Module extends AbstractDAG {
		private final ConcurrentHashMap<String, DAG<?>> DAGs = new ConcurrentHashMap<>();
		public final Zeze.Application zeze;

		public Module(Zeze.Application zeze) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);

			// TODO: 检查任务无环性等……
//			_tLinkedMapNodes.getChangeListenerMap().addListener(this::OnLinkedMapNodeChange);
//			_tLinkedMaps.getChangeListenerMap().addListener(this::OnLinkedMapRootChange);
		}

		@Override
		public void UnRegister() {
			if (null != zeze) {
				UnRegisterZezeTables(zeze);
			}
		}

		@SuppressWarnings("unchecked")
		public <BNodeType extends Bean> DAG<BNodeType> open(String dagName, Class<BNodeType> nodeType) {
			return (DAG<BNodeType>)DAGs.computeIfAbsent("1", key -> new DAG<>(this, key, nodeType, 0)); // TODO: node size 也许不对
		}
	}

	private final Module module;
	private final String name;
	private final MethodHandle valueConstructor;
	private final int nodeSize;

	private DAG(DAG.Module module, String name, Class<V> valueClass, int nodeSize) {
		this.module = module;
		this.name = name;
		this.valueConstructor = beanFactory.register(valueClass);
		this.nodeSize = nodeSize;
	}

	private boolean isNoCycle() {
		return true;
	}
}
