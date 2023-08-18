using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using Zeze.Transaction;

namespace Zeze.Gen
{
    public class Table
    {
        public ModuleSpace Space { get; private set; }
        public string Name { get; private set; }
        public string Key { get; }
        public string Value { get; }
        public string Gen { get; }
        public bool IsMemory { get; }
        public string AutoKey { get; }
        public bool IsAutoKey => "true".Equals(AutoKey);
        public bool IsAutoKeyRandom => "random".Equals(AutoKey);

        // setup in compile
        public Types.Type KeyType { get; private set; }
        public Types.Type ValueType { get; private set; }
        public string FullName => Space.Path(".", Name);
        public string Kind { get; private set; } = "";
        public bool IsRocks => Kind.Equals("rocks");
        public int Id { get; private set; }
        public string Comment { get; private set; }
        public string RelationalMapping { get; private set; } = string.Empty;
        public bool IsRelationalMapping => RelationalMapping switch
        {
            "project" => Project.MakingInstance.RelationalMapping,
            "" => Project.MakingInstance.RelationalMapping,
            "true" => true,
            "false" => false,
            _ => throw new System.Exception("RelationalMapping Options: true|false|project")
        };

        public Table(ModuleSpace space, XmlElement self)
        {
            Space = space;
            Name = self.GetAttribute("name").Trim();
            Program.CheckReserveName(Name, space.Path());
            space.Add(this);

            Key = self.GetAttribute("key");
            Value = self.GetAttribute("value");
            Gen = self.GetAttribute("gen");

            string attr = self.GetAttribute("memory");
            IsMemory = attr.Length > 0 && bool.Parse(attr);
            AutoKey = self.GetAttribute("autokey");

            Kind = self.GetAttribute("kind");

            attr = self.GetAttribute("id");
            Id = attr.Length > 0 ? int.Parse(attr) : Util.FixedHash.Hash32(FullName);
            Comment = Types.Bean.GetComment(self);

            RelationalMapping = self.GetAttribute("RelationalMapping");
        }

        public void Compile()
        {
            KeyType = Types.Type.Compile(Space, Key);
            if (false == KeyType.IsKeyable)
                throw new Exception("table.key need a isKeyable type: " + Space.Path(".", Name));
            if (this.IsAutoKey && KeyType is not Types.TypeLong)
                throw new Exception("autokey only support key type of long");

            ValueType = Types.Type.Compile(Space, Value);
            if (IsRocks)
            {
                if (!ValueType.IsRocks)
                    throw new Exception("rocks table need a rocks bean. table=" + Space.Path(".", Name));
            }
            else
            {
                if (!ValueType.IsNormalBean) // is normal bean, exclude beankey
                    throw new Exception("zeze table need a normal bean. table=" + Space.Path(".", Name));
            }
            if (IsAutoKeyRandom)
            {
                if (KeyType is not Types.TypeBinary)
                    throw new Exception("autokey random need a binary key type.");
            }
        }

        public void Depends(HashSet<Types.Type> depends, string parent)
        {
            if (parent != null)
                parent += ".Table(" + FullName + ')';
            KeyType.Depends(depends, parent);
            ValueType.Depends(depends, parent);
        }
    }
}
