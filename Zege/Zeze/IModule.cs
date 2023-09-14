using System;
using Zeze.Net;

namespace Zeze
{
    public abstract class IModule
    {
        public static int GetModuleId(long result)
        {
            return Protocol.GetModuleId(result);
        }

        public static int GetErrorCode(long result)
        {
            return Protocol.GetProtocolId(result);
        }

        public abstract string Name { get; }
        public abstract string FullName { get; }
        public abstract int Id { get; }
        public virtual bool IsBuiltin => false;

        public virtual void Register()
        {
        }

        public virtual void UnRegister()
        {
        }

        public virtual void Initialize()
        {
        }

        public long ErrorCode(int code)
        {
            if (code < 0)
                throw new Exception("code < 0");
            return Protocol.MakeTypeId(Id, code);
        }
    }
}
