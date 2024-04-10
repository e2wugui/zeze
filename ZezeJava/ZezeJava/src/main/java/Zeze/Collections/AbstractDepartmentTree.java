// auto-generated @formatter:off
package Zeze.Collections;

public abstract class AbstractDepartmentTree implements Zeze.IModule {
    public static final int ModuleId = 11101;
    public static final String ModuleName = "DepartmentTree";
    public static final String ModuleFullName = "Zeze.Collections.DepartmentTree";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int ErrorChangeRootNotOwner = 1;
    public static final int ErrorDepartmentDuplicate = 2;
    public static final int ErrorDepartmentNotExist = 3;
    public static final int ErrorDeleteDepartmentRemainChilds = 4;
    public static final int ErrorDepartmentSameParent = 5;
    public static final int ErrorCanNotMoveToChilds = 6;
    public static final int ErrorDepartmentParentNotExist = 7;
    public static final int ErrorManagePermission = 8;
    public static final int ErrorTooManyChildren = 9;

    protected final Zeze.Builtin.Collections.DepartmentTree.tDepartment _tDepartment = new Zeze.Builtin.Collections.DepartmentTree.tDepartment();
    protected final Zeze.Builtin.Collections.DepartmentTree.tDepartmentTree _tDepartmentTree = new Zeze.Builtin.Collections.DepartmentTree.tDepartmentTree();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tDepartment.getName()).getDatabaseName(), _tDepartment);
        zeze.addTable(zeze.getConfig().getTableConf(_tDepartmentTree.getName()).getDatabaseName(), _tDepartmentTree);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tDepartment.getName()).getDatabaseName(), _tDepartment);
        zeze.removeTable(zeze.getConfig().getTableConf(_tDepartmentTree.getName()).getDatabaseName(), _tDepartmentTree);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
