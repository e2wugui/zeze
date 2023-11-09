package Zeze.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Collections.DepartmentTree.*;
import Zeze.Hot.HotBeanFactory;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.OutLong;

public class DepartmentTree<
		TManager extends Bean,
		TMember extends Bean,
		TDepartmentMember extends Bean,
		TGroupData extends Bean,
		TDepartmentData extends Bean> implements HotBeanFactory {
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Serializable bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	private final ConcurrentHashSet<HotModule> hotModulesHaveDynamic = new ConcurrentHashSet<>();
	private boolean freshStopModuleDynamic = false;

	private void onHotModuleStop(HotModule hot) {
		freshStopModuleDynamic |= hotModulesHaveDynamic.remove(hot) != null;
	}

	private void tryRecordHotModule(Class<?> customClass) {
		var cl = customClass.getClassLoader();
		if (HotManager.isHotModule(cl)) {
			var hotModule = (HotModule)cl;
			hotModule.stopEvents.add(this::onHotModuleStop);
			hotModulesHaveDynamic.add(hotModule);
		}
	}

	@Override
	public void processWithNewClasses(java.util.List<Class<?>> newClasses) {
		for (var cls : newClasses) {
			tryRecordHotModule(cls);
		}
	}

	@Override
	public boolean hasFreshStopModuleDynamicOnce() {
		var tmp = freshStopModuleDynamic;
		freshStopModuleDynamic = false;
		return tmp;
	}

	@Override
	public void clearTableCache() {
		module._tDepartment.__ClearTableCacheUnsafe__();
		module._tDepartmentTree.__ClearTableCacheUnsafe__();
	}

	@Override
	public BeanFactory beanFactory() {
		return beanFactory;
	}

	public static class Module extends AbstractDepartmentTree {
		private final ConcurrentHashMap<String, DepartmentTree<?, ?, ?, ?, ?>> trees = new ConcurrentHashMap<>();
		public final Zeze.Application zeze;
		public final LinkedMap.Module linkedMaps;

		public Module(Zeze.Application zeze, LinkedMap.Module linkedMapModule) {
			this.zeze = zeze;
			RegisterZezeTables(zeze);
			linkedMaps = linkedMapModule;
		}

		@Override
		public void UnRegister() {
			UnRegisterZezeTables(zeze);
		}

		@SuppressWarnings("unchecked")
		public <TManager extends Bean,
				TMember extends Bean,
				TDepartmentMember extends Bean,
				TGroupData extends Bean,
				TDepartmentData extends Bean>
		DepartmentTree<TManager, TMember, TDepartmentMember, TGroupData, TDepartmentData>
		open(String name,
			 Class<TManager> managerClass,
			 Class<TMember> memberClass,
			 Class<TDepartmentMember> departmentMemberClass,
			 Class<TGroupData> groupDataClass,
			 Class<TDepartmentData> departmentDataClass) {
			if (name.isEmpty())
				throw new IllegalArgumentException("name is empty.");
			if (name.contains("@"))
				throw new IllegalArgumentException("name contains '@', that is reserved.");

			return (DepartmentTree<
					TManager,
					TMember,
					TDepartmentMember,
					TGroupData,
					TDepartmentData>)
					trees.computeIfAbsent(name,
							k -> new DepartmentTree<>(this, k,
									managerClass,
									memberClass,
									departmentMemberClass,
									groupDataClass,
									departmentDataClass));
		}

	}

	private final Module module;
	private final String name;
	private final long managerTypeId;
	private final long groupDataTypeId;
	private final long departmentDataTypeId;

	private final long memberBeanTypeId;
	private final long departmentMemberBeanTypeId;

	private DepartmentTree(Module module, String name,
						   Class<TManager> managerClass,
						   Class<TMember> memberClass,
						   Class<TDepartmentMember> departmentMemberClass,
						   Class<TGroupData> groupDataClass,
						   Class<TDepartmentData> departmentDataClass) {

		var hotManager = module.zeze.getHotManager();
		if (null != hotManager) {
			hotManager.addHotBeanFactory(this);
			tryRecordHotModule(managerClass);
			tryRecordHotModule(memberClass);
			tryRecordHotModule(departmentMemberClass);
			tryRecordHotModule(groupDataClass);
			tryRecordHotModule(departmentDataClass);
		}

		this.module = module;
		this.name = name;

		beanFactory.register(managerClass);
		beanFactory.register(groupDataClass);
		beanFactory.register(departmentDataClass);

		this.managerTypeId = BeanFactory.typeId(managerClass);
		this.groupDataTypeId = BeanFactory.typeId(groupDataClass);
		this.departmentDataTypeId = BeanFactory.typeId(departmentDataClass);

		//this.managerClass = managerClass;
		this.memberBeanTypeId = BeanFactory.typeId(memberClass);
		this.departmentMemberBeanTypeId = BeanFactory.typeId(departmentMemberClass);
	}

	public String getName() {
		return name;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Api 设计原则：
	// 1. 可以直接访问原始Bean，不进行包装。
	// 2. 不考虑使用安全性。
	// 3. 提供必要的辅助函数完成一些操作。
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	public TManager getManagerData(BDepartmentRoot root, String account) {
		var dynamicManager = root.getManagers().get(account);
		if (null == dynamicManager)
			return null;
		return (TManager)dynamicManager.getBean();
	}

	@SuppressWarnings("unchecked")
	public TManager getManagerData(BDepartmentTreeNode department, String account) {
		var dynamicManager = department.getManagers().get(account);
		if (null == dynamicManager)
			return null;
		return (TManager)dynamicManager.getBean();
	}

	@SuppressWarnings("unchecked")
	public TGroupData getGroupData(BDepartmentRoot root) {
		return (TGroupData)root.getData().getBean();
	}

	@SuppressWarnings("unchecked")
	public TDepartmentData getDepartmentData(BDepartmentTreeNode department) {
		return (TDepartmentData)department.getData().getBean();
	}

	// see checkManagePermission
	public long checkParentManagePermission(String account, long departmentId) {
		if (departmentId == 0) {
			// root
			var root = getRoot();
			if (root.getRoot().equals(account))
				return 0; // parent, 对于根节点定义成root，grant
			return module.errorCode(Module.ErrorManagePermission);
		}
		var department = getDepartmentTreeNode(departmentId);
		if (department == null)
			return module.errorCode(Module.ErrorDepartmentNotExist);

		// 开始检查parent
		return checkManagePermission(account, department.getParentDepartment());
	}

	// 检查account是否拥有部门的管理权限。
	// 规则：
	// 1. 如果部门拥有管理员，仅判断account是否管理员。
	// 2. 如果部门没有管理员，则递归检查父部门的管理员设置。
	// 3. 递归时优先规则1，直到根为止。Root总是拥有权限。
	// 其他：
	// 当管理管理员设置时，这个方法允许本级部门管理添加新的管理员和删除管理员。
	// 对于管理员的修改是否限定只能由上级操作？
	// 对于这个限定，可以在调用 checkParentManagePermission
	public long checkManagePermission(String account, long departmentId) {
		if (departmentId == 0) {
			// root
			var root = getRoot();
			if (root.getManagers().containsKey(account) || root.getRoot().equals(account))
				return 0; // grant
			return module.errorCode(Module.ErrorManagePermission);
		}

		var department = getDepartmentTreeNode(departmentId);
		if (department == null)
			return module.errorCode(Module.ErrorDepartmentNotExist);

		if (department.getManagers().isEmpty()) // 当前部门没有管理员，使用父部门的设置(递归)。
			return checkManagePermission(account, department.getParentDepartment());

		if (department.getManagers().containsKey(account))
			return 0; // grant

		// 当设置了管理员，不再递归。遵守权限不越级规则。
		return module.errorCode(Module.ErrorManagePermission);
	}

	public BDepartmentRoot getRoot() {
		return module._tDepartment.get(name);
	}

	public BDepartmentTreeNode getDepartmentTreeNode(long departmentId) {
		if (departmentId == 0)
			throw new IllegalArgumentException("root can not access use this method.");
		return module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
	}

	@SuppressWarnings("unchecked")
	public LinkedMap<TDepartmentMember> getDepartmentMembers(long departmentId) {
		if (departmentId == 0)
			throw new IllegalArgumentException("root can not access use this method.");
		var departmentMemberClass = (Class<TDepartmentMember>)BeanFactory.findClass(departmentMemberBeanTypeId);
		if (null == departmentMemberClass)
			throw new IllegalArgumentException("departmentMemberClass not found");
		return module.linkedMaps.open(departmentId + "@" + name, departmentMemberClass);
	}

	@SuppressWarnings("unchecked")
	public LinkedMap<TMember> getGroupMembers() {
		var memberClass = (Class<TMember>)BeanFactory.findClass(memberBeanTypeId);
		if (null == memberClass)
			throw new IllegalArgumentException("memberClass not found");
		return module.linkedMaps.open("0@" + name, memberClass);
	}

	public BDepartmentRoot selectRoot() {
		return module._tDepartment.selectDirty(name);
	}

	public BDepartmentTreeNode selectDepartmentTreeNode(long departmentId) {
		return module._tDepartmentTree.selectDirty(new BDepartmentKey(name, departmentId));
	}

	public void destroy() {
		module._tDepartment.remove(name);
	}

	public BDepartmentRoot create() {
		var root = module._tDepartment.getOrAdd(name);
		root.getData().setBean(beanFactory.createBeanFromSpecialTypeId(groupDataTypeId));
		return root;
	}

	public long changeRoot(String oldRoot, String newRoot) {
		var dRoot = module._tDepartment.getOrAdd(name);
		if (!dRoot.getRoot().equals(oldRoot))
			return module.errorCode(Module.ErrorChangeRootNotOwner);
		dRoot.setRoot(newRoot);
		return 0;
	}

	@SuppressWarnings("unchecked")
	private TManager getOrAddRootManager() {
		var dRoot = module._tDepartment.getOrAdd(name);
		return (TManager)dRoot.getManagers().computeIfAbsent(name, key -> {
			var value = new DynamicBean(0, DepartmentTree::getSpecialTypeIdFromBean, DepartmentTree::createBeanFromSpecialTypeId);
			value.setBean(beanFactory.createBeanFromSpecialTypeId(managerTypeId));
			return value;
		}).getBean();
	}

	@SuppressWarnings("unchecked")
	public TManager getOrAddManager(long departmentId, String name) {
		if (departmentId == 0)
			return getOrAddRootManager();

		var d = getDepartmentTreeNode(departmentId);
		return (TManager)d.getManagers().computeIfAbsent(name, key -> {
			var value = new DynamicBean(0, DepartmentTree::getSpecialTypeIdFromBean, DepartmentTree::createBeanFromSpecialTypeId);
			value.setBean(beanFactory.createBeanFromSpecialTypeId(managerTypeId));
			return value;
		}).getBean();
	}

	@SuppressWarnings("unchecked")
	private TManager deleteRootManager(String name) {
		var dRoot = module._tDepartment.getOrAdd(name);
		var m = dRoot.getManagers().remove(name);
		if (null == m)
			return null;
		return (TManager)m.getBean();
	}

	@SuppressWarnings("unchecked")
	public TManager deleteManager(long departmentId, String name) {
		if (departmentId == 0)
			return deleteRootManager(name);

		var d = getDepartmentTreeNode(departmentId);
		var m = d.getManagers().remove(name);
		return m != null ? (TManager)m.getBean() : null;
	}

	public long createDepartment(long departmentParent, String dName, int childrenLimit, OutLong outDepartmentId) {
		var dRoot = module._tDepartment.getOrAdd(name);
		var dId = dRoot.getNextDepartmentId() + 1;

		if (departmentParent == 0) {
			if (dRoot.getChilds().size() > childrenLimit)
				return module.errorCode(Module.ErrorTooManyChildren);
			if (null != dRoot.getChilds().putIfAbsent(dName, dId))
				return module.errorCode(Module.ErrorDepartmentDuplicate);
		} else {
			var parent = getDepartmentTreeNode(departmentParent);
			if (parent.getChilds().size() > childrenLimit)
				return module.errorCode(Module.ErrorTooManyChildren);
			if (null != parent.getChilds().putIfAbsent(dName, dId))
				return module.errorCode(Module.ErrorDepartmentDuplicate);
		}
		var child = new BDepartmentTreeNode();
		child.getData().setBean(beanFactory.createBeanFromSpecialTypeId(departmentDataTypeId));
		child.setName(dName);
		child.setParentDepartment(departmentParent);
		module._tDepartmentTree.insert(new BDepartmentKey(name, dId), child);
		dRoot.setNextDepartmentId(dId);
		if (null != outDepartmentId)
			outDepartmentId.value = dId;
		return 0;
	}

	public long deleteDepartment(long departmentId, boolean recursive) {
		var department = module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
		if (null == department)
			return module.errorCode(Module.ErrorDepartmentNotExist);
		if (!recursive && !department.getChilds().isEmpty())
			return module.errorCode(Module.ErrorDeleteDepartmentRemainChilds);
		for (var child : department.getChilds().values()) {
			deleteDepartment(child, true);
		}
		if (department.getParentDepartment() == 0) {
			var root = module._tDepartment.get(name);
			if (root != null)
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
			return module.errorCode(Module.ErrorDepartmentNotExist);
		if (department.getParentDepartment() == 0)
			return module.errorCode(Module.ErrorDepartmentSameParent);
		var oldParent = getDepartmentTreeNode(department.getParentDepartment());
		oldParent.getChilds().remove(department.getName());
		if (null != newParent.getChilds().putIfAbsent(department.getName(), departmentId))
			return module.errorCode(Module.ErrorDepartmentDuplicate);
		department.setParentDepartment(0);
		return 0;
	}

	public long moveDepartment(long departmentId, long parent) {
		if (parent == 0) // to root
			return moveDepartment(departmentId);

		if (isRecursiveChild(departmentId, parent))
			return module.errorCode(Module.ErrorCanNotMoveToChilds);
		var department = module._tDepartmentTree.get(new BDepartmentKey(name, departmentId));
		if (null == department)
			return module.errorCode(Module.ErrorDepartmentNotExist);
		if (department.getParentDepartment() == parent)
			return module.errorCode(Module.ErrorDepartmentSameParent);
		var newParent = getDepartmentTreeNode(parent);
		if (null == newParent)
			return module.errorCode(Module.ErrorDepartmentParentNotExist);
		var oldParent = getDepartmentTreeNode(department.getParentDepartment());
		oldParent.getChilds().remove(department.getName());
		if (null != newParent.getChilds().putIfAbsent(department.getName(), departmentId))
			return module.errorCode(Module.ErrorDepartmentDuplicate);
		department.setParentDepartment(parent);
		return 0;
	}
}
