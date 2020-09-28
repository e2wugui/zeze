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
        public int TypeId => ((int)Space.Id << 16) | (int)Id;
        public int TypeRpcRequestId => TypeId; // 用来注册 handle
        public int TypeRpcResponseId => (int)((uint)TypeId | 0x80000000); // 用来注册 handle
        public int TypeRpcTimeoutId => (TypeId | 0x8000); // 用来注册 handle

        public string Argument { get; private set; }
        public string Handle { get; private set; }
        public int HandleFlags { get; }

        public List<Types.Enum> Enums { get; private set; } = new List<Types.Enum>();

        // setup in compile
        public Types.Type ArgumentType { get; private set; }

        public Protocol(ModuleSpace space, XmlElement self)
        {
            Space = space;
            Name = self.GetAttribute("name").Trim();
            space.Add(this);

            Id = short.Parse(self.GetAttribute("id"));
            if (Id <= 0)
                throw new Exception("protocol id <= 0 is reserved. @" + space.Path(".", Name));
            space.ProtocolIdRanges.CheckAdd(Id);

            Argument = self.GetAttribute("argument");
            Handle = self.GetAttribute("handle");
            HandleFlags = Program.ToHandleFlags(Handle);

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;

                String nodename = e.Name;
                switch (e.Name)
                {
                    case "enum":
                        Add(new Types.Enum(e));
                        break;
                    default:
                        throw new Exception("node=" + nodename);
                }
            }
        }

        public void Add(Types.Enum e)
        {
            Enums.Add(e); // check duplicate
        }

        public virtual void Compile()
        {
            ArgumentType = Argument.Length > 0 ? Types.Type.Compile(Space, Argument) : null;
        }

        public virtual void Depends(HashSet<Types.Type> depends)
        {
            ArgumentType?.Depends(depends);
        }
    }
}
