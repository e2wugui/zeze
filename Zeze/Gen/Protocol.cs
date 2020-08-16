using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Protocol
    {
        public ModuleSpace Space { get; private set; }
        public string Name { get; private set; }
        public short Id { get; private set; }
        public string Argument { get; private set; }
        public string Handle { get; private set; }

        // setup in compile
        public Types.Type ArgumentType { get; private set; }

        public Protocol(ModuleSpace space, XmlElement self)
        {
            Space = space;
            Name = self.GetAttribute("name").Trim();
            space.Add(this);

            Id = short.Parse(self.GetAttribute("id"));
            if (Id < 0)
                throw new Exception("protocol id < 0 is reserved. @" + space.Path(".", Name));
            space.ProtocolIdRanges.CheckAdd(Id);

            Argument = self.GetAttribute("argument");
            Handle = self.GetAttribute("handle");
        }

        public virtual void Compile()
        {
            ArgumentType = Types.Type.Compile(Space, Argument);
        }

        public virtual void Depends(HashSet<Types.Type> depends)
        {
            ArgumentType.Depends(depends);
        }
    }
}
