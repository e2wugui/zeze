using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Table
    {
        public ModuleSpace Space { get; private set; }
        public string Name { get; private set; }
        public string Key { get; }
        public string Value { get; }
        public bool IsMemory { get; }
        public bool IsAutoKey { get; }

        // setup in compile
        public Types.Type KeyType { get; private set; }
        public Types.Type ValueType { get; private set; }

        public Table(ModuleSpace space, XmlElement self)
        {
            Space = space;
            Name = self.GetAttribute("name").Trim();
            space.Add(this);

            Key = self.GetAttribute("key");
            Value = self.GetAttribute("value");
            string attr = self.GetAttribute("memory");
            IsMemory = attr.Length > 0 ? bool.Parse(attr) : false;
            attr = self.GetAttribute("autokey");
            IsAutoKey = attr.Length > 0 ? bool.Parse(attr) : false;
        }

        public void Compile()
        {
            KeyType = Types.Type.Compile(Space, Key);
            if (false == KeyType.IsKeyable)
                throw new Exception("table.key need a isKeyable type: " + Space.Path(".", Name));
            if (this.IsAutoKey && !(KeyType is Types.TypeLong))
                throw new Exception("autokey only support key type of long");

            ValueType = Types.Type.Compile(Space, Value);
            if (!ValueType.IsNormalBean) // is normal bean, exclude beankey
                throw new Exception("table.value need a normal bean : " + Space.Path(".", Name));
        }

        public void Depends(HashSet<Types.Type> depends)
        {
            KeyType.Depends(depends);
            ValueType.Depends(depends);
        }
    }
}
