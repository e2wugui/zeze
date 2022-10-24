// auto-generated
using Zeze.Serialize;

namespace Zeze.Builtin.Collections.DepartmentTree
{
    public sealed class tDepartmentTree : Zeze.Transaction.Table<Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey, Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode, Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNodeReadOnly>
    {
        public tDepartmentTree() : base("Zeze_Builtin_Collections_DepartmentTree_tDepartmentTree")
        {
        }

        public override int Id => -1578893665;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_ParentDepartment = 1;
        public const int VAR_Childs = 2;
        public const int VAR_Name = 3;
        public const int VAR_Managers = 4;

        public override Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey DecodeKey(ByteBuffer _os_)
        {
            var _v_ = new Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey();
            _v_.Decode(_os_);
            return _v_;
        }

        public override ByteBuffer EncodeKey(Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _v_.Encode(_os_);
            return _os_;
        }
    }
}
