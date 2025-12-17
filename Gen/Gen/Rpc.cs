using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Rpc : Protocol
    {
        public string Result { get; }

        // setup in compile
        public Types.Type ResultType { get; private set;  }
        public string Base { get; private set; }

        public Rpc(ModuleSpace space, XmlElement self) : base(space, self)
        {
            Result = self.GetAttribute("result");
            Base = self.GetAttribute("base");
        }

        public override void Compile()
        {
            base.Compile();
            ResultType = Result.Length > 0 ? Types.Type.Compile(Space, Result) : null;
        }

        public override void Depends(HashSet<Types.Type> depends, string parent)
        {
            if (parent != null)
                parent += ".Rpc(" + FullName + ')';
            ArgumentType?.Depends(depends, parent);
            ResultType?.Depends(depends, parent);
        }
    }
}
