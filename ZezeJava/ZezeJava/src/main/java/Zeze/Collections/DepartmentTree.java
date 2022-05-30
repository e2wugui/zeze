package Zeze.Collections;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.DepartmentTree.*;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.OutLong;

public class DepartmentTree<TManager extends Bean, TMember extends Bean, TDepartmentMember extends Bean> {
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return beanFactory.GetSpecialTypeIdFromBean(bean);
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		return beanFactory.CreateBeanFromSpecialTypeId(typeId);
	}

	public static class Module extends AbstractDepartmentTree {
		private final ConcurrentHashMap<String, DepartmentTree<?, ?, ?>> Trees = new ConcurrentHashMap<>();
		public final Zeze.Application Zeze;
		public final LinkedMap.Module LinkedMaps;

		public Module(Zeze.Application zeze, LinkedMap.Module linkedMapModule) {
			Zeze = zeze;
			RegisterZezeTables(zeze);
			LinkedMaps = linkedMapModule;
		}

		@Override
		public void UnRegister() {
			UnRegisterZezeTables(Zeze);
		}

		@SuppressWarnings("unchecked")
		public <TManager extends Bean, TMember extends Bean, TDepartmentMember extends Bean>
			DepartmentTree<TManager, TMember, TDepartmentMember>
			open(String name, Class<TManager> managerClass, Class<TMember> memberClass, Class<TDepartmentMember> departmentMemberClass) {
			return (DepartmentTree<TManager, TMember, TDepartmentMember>)Trees.computeIfAbsent(name,
					k -> new DepartmentTree<>(this, k, managerClass, memberClass, departmentMemberClass));
		}

	}

	private final Module module;
	private final String name;
	private final MethodHandle managerConstructor;
	//private final Class<TManager> managerClass;
	private final Class<TMember> memberClass;
	private final Class<TDepartmentMember> departmentMemberClass;

	private DepartmentTree(Module module, String name, Class<TManager> managerClass, Class<TMember> memberClass, Class<TDepartmentMember> departmentMemberClass) {
		this.module = module;
		this.name = name;
		this.managerConstructor = beanFactory.register(managerClass);
		//this.managerClass = managerClass;
		this.memberClass = memberClass;
		this.departmentMemberClass = departmentMemberClass;
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

	public BDepartmentTreeNode getDepartmentTreeNode(long departmentId) {
		return module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
	}

	public LinkedMap<TDepartmentMember> getDepartmentMembers(long departmentId) {
		if (departmentId == 0)
			throw new RuntimeException("root members use getMembers.");
		return module.LinkedMaps.open("" + departmentId + "#" + name, departmentMemberClass);
	}

	public LinkedMap<TMember> getGroupMembers() {
		return module.LinkedMaps.open("0#" + name, memberClass);
	}

	public BDepartmentRoot selectRoot() {
		return module._tDepartment.selectDirty(name);
	}

	public BDepartmentTreeNode selectDepartmentTreeNode(long departmentId) {
		return module._tDepartmentTree.selectDirty(new BDepartmentKey(name, departmentId));
	}

	public BDepartmentRoot create(String root) {
		var dRoot = module._tDepartment.getOrAdd(name);
		dRoot.setRoot(root);
		return dRoot;
	}

	public long changeRoot(String oldRoot, String newRoot) {
		var dRoot = module._tDepartment.getOrAdd(name);
		if (!dRoot.getRoot().equals(oldRoot))
			return module.ErrorCode(Module.ErrorChangeRootNotOwner);
		dRoot.setRoot(newRoot);
		return 0;
	}

	@SuppressWarnings ("unchecked")
	public TManager getOrAddRootManager() {
		var dRoot = module._tDepartment.getOrAdd(name);
		return (TManager)dRoot.getManagers().computeIfAbsent(name, key -> {
			var value = new DynamicBean(0, DepartmentTree::GetSpecialTypeIdFromBean, DepartmentTree::CreateBeanFromSpecialTypeId);
			value.setBean(beanFactory.invoke(managerConstructor));
			return value;
		}).getBean();
	}

	@SuppressWarnings ("unchecked")
	public TManager getOrAddManager(long departmentId, String name) {
		if (departmentId == 0)
			return getOrAddRootManager();

		var d = getDepartmentTreeNode(departmentId);
		return (TManager)d.getManagers().computeIfAbsent(name, key -> {
			var value = new DynamicBean(0, DepartmentTree::GetSpecialTypeIdFromBean, DepartmentTree::CreateBeanFromSpecialTypeId);
			value.setBean(beanFactory.invoke(managerConstructor));
			return value;
		}).getBean();
	}

	public long createDepartment(long departmentParent, String dName, OutLong outDepartmentId) {
		var dRoot = module._tDepartment.getOrAdd(dName);
		var dId = dRoot.getNextDepartmentId() + 1;

		if (departmentParent == 0) {
			if (null != dRoot.getChilds().putIfAbsent(dName, dId))
				return module.ErrorCode(Module.ErrorDepartmentDuplicate);
		} else {
			var parent = getDepartmentTreeNode(departmentParent);
			if (null != parent.getChilds().putIfAbsent(dName, dId))
				return module.ErrorCode(Module.ErrorDepartmentDuplicate);
		}
		var child = new BDepartmentTreeNode();
		child.setName(dName);
		child.setParentDepartment(departmentParent);
		module._tDepartmentTree.insert(new BDepartmentKey(name, dId), child);
		dRoot.setNextDepartmentId(dId);
		if (null != outDepartmentId)
			outDepartmentId.Value = dId;
		return 0;
	}

	public long deleteDepartment(long departmentId, boolean recursive) {
		var department = module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
		if (null == department)
			return module.ErrorCode(Module.ErrorDepartmentNotExist);
		if (!recursive && department.getChilds().size() > 0)
			return module.ErrorCode(Module.ErrorDeleteDepartmentRemainChilds);
		for (var child : department.getChilds().values()) {
			deleteDepartment(child, true);
		}
		if (department.getParentDepartment() == 0) {
			var root = module._tDepartment.get(name);
			root.getChilds().remove(department.getName());
		} else {
			var parent = getDepartmentTreeNode(department.getParentDepartment());
			parent.getChilds().remove(department.getName());
		}
		getDepartmentMembers(departmentId).clear();
		module._tDepartmentTree.remove(new BDepartmentKey(name, departmentId));
		return 0;
	}

	public boolean isRecursiveChild(long departmentId, long child) {
		if (departmentId == child)
			return true;

		var department = module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
		if (null == department)
			return false;
		for (var c : department.getChilds().values()) {
			if (isRecursiveChild(c, child))
				return true;
		}
		return false;
	}

	public long moveDepartment(long departmentId) {
		var department = module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
		var newParent = module._tDepartment.get(name);
		if (null == department || null == newParent)
			return module.ErrorCode(Module.ErrorDepartmentNotExist);
		if (department.getParentDepartment() == 0)
			return module.ErrorCode(Module.ErrorDepartmentSameParent);
		var oldParent = getDepartmentTreeNode(department.getParentDepartment());
		oldParent.getChilds().remove(department.getName());
		if (null != newParent.getChilds().putIfAbsent(department.getName(), departmentId))
			return module.ErrorCode(Module.ErrorDepartmentDuplicate);
		department.setParentDepartment(0);
		return 0;
	}

	public long moveDepartment(long departmentId, long parent) {
		if (parent == 0) // to root
			return moveDepartment(departmentId);

		if (isRecursiveChild(departmentId, parent))
			return module.ErrorCode(Module.ErrorCanNotMoveToChilds);
		var department = module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
		if (null == department)
			return module.ErrorCode(Module.ErrorDepartmentNotExist);
		if (department.getParentDepartment() == parent)
			return module.ErrorCode(Module.ErrorDepartmentSameParent);
		var newParent = getDepartmentTreeNode(parent);
		if (null == newParent)
			return module.ErrorCode(Module.ErrorDepartmentParentNotExist);
		var oldParent = getDepartmentTreeNode(department.getParentDepartment());
		oldParent.getChilds().remove(department.getName());
		if (null != newParent.getChilds().putIfAbsent(department.getName(), departmentId))
			return module.ErrorCode(Module.ErrorDepartmentDuplicate);
		department.setParentDepartment(parent);
		return 0;
	}
}
