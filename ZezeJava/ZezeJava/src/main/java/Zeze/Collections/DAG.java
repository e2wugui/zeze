package Zeze.Collections;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.DAG.BDAGNode;
import Zeze.Builtin.Collections.DAG.BDAGNodeKey;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

public class DAG<V extends Bean> {
	public static final BeanFactory beanFactory = new BeanFactory();
	public static long getSpecialTypeIdFromBean(Serializable bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}
	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}
	public MethodHandle getValueConstructor() {
		return valueConstructor;
	}
	public Module getModule() {
		return module;
	}

	public String getName() {
		return name;
	}
	final DirectedAcyclicGraph<BDAGNodeKey, DefaultEdge> graph = new DirectedAcyclicGraph<>(DefaultEdge.class);

	public boolean addNode(long id, V value) throws Exception {
//		var nodeIdKey = new BDAGNodeKey(name, Long.toString(id));
		var nodeNode = new BDAGNode();
		nodeNode.getValue().setBean(value);
		checkValid();
		return true;
	}
	public boolean addEdge(long from, long to) throws Exception {
		checkValid();
		return true;
	}
	public void checkValid() throws Exception {
		if (!isValid())
			throw new Exception("DAG is invalid."); // 让异常提示更加智能
	}
	public boolean isEmpty() {
		return graph.vertexSet().isEmpty();
	}

	public static class Module extends AbstractDAG {
		private final ConcurrentHashMap<String, DAG<?>> DAGs = new ConcurrentHashMap<>();
		public final Zeze.Application zeze;

		public Module(Zeze.Application zeze) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);
		}

		@Override
		public void UnRegister() {
			if (null != zeze) {
				UnRegisterZezeTables(zeze);
			}
		}

		@SuppressWarnings("unchecked")
		public <BNodeType extends Bean> DAG<BNodeType> open(String dagName, Class<BNodeType> nodeType) {
			return (DAG<BNodeType>)DAGs.computeIfAbsent("1", key -> new DAG<>(this, key, nodeType));
		}
	}

	private DAG(DAG.Module module, String name, Class<V> valueClass) {
		this.module = module;
		this.name = name;
		this.valueConstructor = beanFactory.register(valueClass);
	}
	private final Module module;
	private final String name;
	private final MethodHandle valueConstructor;
	/**
	 * 检查有向图合法
	 * 1. 无环
	 */
	private boolean isValid() {
		return isNoCycle();
	}
	@SuppressWarnings("MethodMayBeStatic")
	private boolean isNoCycle() {
		return true;
	}
}
