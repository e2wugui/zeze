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
        }

        public void Compile()
        {
            KeyType = Types.Type.Compile(Space, Key);
            if (false == KeyType.IsImmutable)
                throw new Exception("table.key need a immutable type: " + Space.Path(".", Name));

            ValueType = Types.Type.Compile(Space, Value);
            if (!ValueType.IsBean && !ValueType.IsImmutable)
                throw new Exception("table.value need a bean or immutable type : " + Space.Path(".", Name));
        }

        public void Depends(HashSet<Types.Type> depends)
        {
            KeyType.Depends(depends);
            ValueType.Depends(depends);
        }
    }
}
