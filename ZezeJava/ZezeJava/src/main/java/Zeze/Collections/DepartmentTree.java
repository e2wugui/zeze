package Zeze.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.DepartmentTree.*;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;

public class DepartmentTree<TManager extends Bean, TMember extends Bean> {
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return beanFactory.GetSpecialTypeIdFromBean(bean);
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		return beanFactory.CreateBeanFromSpecialTypeId(typeId);
	}

	public static class Module extends AbstractDepartmentTree {
		private final ConcurrentHashMap<String, DepartmentTree<?, ?>> Trees = new ConcurrentHashMap<>();
		public final Zeze.Application Zeze;
		public final LinkedMap.Module Members;

		public Module(Zeze.Application zeze) {
			Zeze = zeze;
			RegisterZezeTables(zeze);
			Members = new LinkedMap.Module(zeze);
		}

		@Override
		public void UnRegister() {
			Members.UnRegister();
			UnRegisterZezeTables(Zeze);
		}

		@SuppressWarnings("unchecked")
		public <TManager extends Bean, TMember extends Bean> DepartmentTree<TManager, TMember>
			open(String name, Class<TManager> managerClass, Class<TMember> memberClass) {
			return (DepartmentTree<TManager, TMember>)Trees.computeIfAbsent(name, k -> new DepartmentTree<>(this, k, managerClass, memberClass));
		}

	}

	private final Module module;
	private final String name;
	private final Class<TMember> memberClass;

	private DepartmentTree(Module module, String name, Class<TManager> managerClass, Class<TMember> memberClass) {
		this.module = module;
		this.name = name;
		beanFactory.register(managerClass);
		this.memberClass = memberClass;
	}

	public String getName() {
		return name;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Api 设计原则：
	// 1. 可以直接访问原始Bean，不进行包装。
	// 2. 提供必要的辅助函数完成一些操作。
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public BDepartmentRoot getRoot() {
		return module._tDepartment.get(name);
	}

	public BDepartmentTreeNode getDepartment(long departmentId) {
		return module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
	}

	public LinkedMap<TMember> getDepartmentMembers(long departmentId) {
		return module.Members.<TMember>open("" + departmentId + "#" + name, memberClass);
	}

	public LinkedMap<TMember> getMembers() {
		return module.Members.<TMember>open("0#" + name, memberClass);
	}

	public BDepartmentRoot selectDirtyRoot() {
		return module._tDepartment.selectDirty(name);
	}

	public BDepartmentTreeNode selectDirtyNode(long departmentId) {
		return module._tDepartmentTree.selectDirty(new BDepartmentKey(name, departmentId));
	}

	public BDepartmentRoot create(String root) {
		var dRoot = module._tDepartment.getOrAdd(name);
		dRoot.setRoot(root);
		return dRoot;
	}

	public boolean changeRoot(String oldRoot, String newRoot)
	{
		var dRoot = module._tDepartment.getOrAdd(name);
		if (!dRoot.getRoot().equals(oldRoot))
			return false;
		dRoot.setRoot(newRoot);
		return true;
	}

	public void addRootManager(String name, TManager manager) {
		var dRoot = module._tDepartment.getOrAdd(name);
		var dynamic = new DynamicBean(0, DepartmentTree::GetSpecialTypeIdFromBean, DepartmentTree::CreateBeanFromSpecialTypeId);
		dynamic.setBean(manager);
		dRoot.getManagers().put(name, dynamic);
	}

	public void addDepartmentManager(long departmentId, String name, TManager manager) {
		var d = getDepartment(departmentId);
		var dynamic = new DynamicBean(0, DepartmentTree::GetSpecialTypeIdFromBean, DepartmentTree::CreateBeanFromSpecialTypeId);
		dynamic.setBean(manager);
		d.getManagers().put(name, dynamic);
	}

	public long createDepartment(String name) {
		return createDepartment(0, name);
	}

	public long createDepartment(long departmentParent, String name) {
		var dRoot = module._tDepartment.getOrAdd(name);
		var dId = dRoot.getNextDepartmentId() + 1;

		if (departmentParent == 0) {
			if (null != dRoot.getChildDepartments().put(name, dId))
				return -1; // put first and check duplicate
		} else {
			var parent = getDepartment(departmentParent);
			if (null != parent.getChilds().put(name, dId))
				return -1; // put first and check duplicate
		}
		var child = new BDepartmentTreeNode();
		child.setName(name);
		child.setParentDepartment(departmentParent);
		module._tDepartmentTree.insert(new BDepartmentKey(getName(), dId), child);
		dRoot.setNextDepartmentId(dId);
		return dId;
	}

	public boolean deleteDepartment(long departmentId, boolean recursive) {
		var department = module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
		if (null == department)
			return false;
		if (!recursive && department.getChilds().size() > 0)
			return false;
		for (var child : department.getChilds().values()) {
			deleteDepartment(child, true);
		}
		if (department.getParentDepartment() == 0) {
			var root = module._tDepartment.get(name);
			root.getChildDepartments().remove(department.getName());
		} else {
			var parent = getDepartment(department.getParentDepartment());
			parent.getChilds().remove(department.getName());
		}
		getDepartmentMembers(departmentId).clear();
		module._tDepartmentTree.remove(new BDepartmentKey(name, departmentId));
		return true;
	}
}
