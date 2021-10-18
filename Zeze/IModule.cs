using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze
{
    public abstract class IModule
    {
        public virtual string FullName { get; }
        public virtual string Name { get; }
        public virtual int Id { get; }

        public virtual void UnRegister() // 为了重新装载 Module 的补丁。注册在构造函数里面进行。
        {
        }

        public int ReturnCode(uint code)
        {
            if (code > ushort.MaxValue)
                throw new Exception("return code too big");
            return ReturnCode((ushort)code);
        }

        public int ReturnCode(ushort code)
        {
            return Id << 16 | code;
        }
    }
}
