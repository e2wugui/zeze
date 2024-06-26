// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

// 群部门树根。普通用户也可以创建部门。暂不开放这个给个人。
// ReSharper disable JoinDeclarationAndInitializer RedundantNameQualifier
namespace Zeze.Builtin.Collections.DepartmentTree
{
    public sealed class tDepartment : Table<string, Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot>, TableReadOnly<string, Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot, Zeze.Builtin.Collections.DepartmentTree.BDepartmentRootReadOnly>
    {
        public tDepartment() : base("Zeze_Builtin_Collections_DepartmentTree_tDepartment")
        {
        }

        public override int Id => -1108948075;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_Root = 1;
        public const int VAR_Managers = 2;
        public const int VAR_NextDepartmentId = 3;
        public const int VAR_Childs = 4;

        public override string DecodeKey(ByteBuffer _os_)
        {
            string _v_;
            _v_ = _os_.ReadString();
            return _v_;
        }

        public override ByteBuffer EncodeKey(string _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _os_.WriteString(_v_);
            return _os_;
        }

        async Task<Zeze.Builtin.Collections.DepartmentTree.BDepartmentRootReadOnly> TableReadOnly<string, Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot, Zeze.Builtin.Collections.DepartmentTree.BDepartmentRootReadOnly>.GetAsync(string key)
        {
            return await GetAsync(key);
        }
    }
}
