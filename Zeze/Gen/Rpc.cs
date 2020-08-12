using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Rpc : Protocol
    {
        public Rpc(ModuleSpace space, XmlElement self) : base(space, self)
        { 
        }
    }
}
