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
        public virtual bool IsBuiltin => false;

        public virtual void Register()
        {
        }

        public virtual void UnRegister()
        {
        }

        public long ErrorCode(int code)
        {
            if (code < 0)
                throw new Exception("code < 0");
            return Zeze.Net.Protocol.MakeTypeId(Id, code);
        }

        public static int GetModuleId(long result)
        {
            return Zeze.Net.Protocol.GetModuleId(result);
        }

        public static int GetErrorCode(long result)
        {
            return Zeze.Net.Protocol.GetProtocolId(result);
        }

        public virtual void Initialize()
        {

        }
    }
}
