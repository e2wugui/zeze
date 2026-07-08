using System;
using System.Xml;
using Zeze.Transaction;

namespace Zeze.Gen
{
    public class ServletStream
    {
        public ModuleSpace Space { get; private set; }
        public string Name { get; }
        public TransactionLevel TransactionLevel { get; } = TransactionLevel.Serializable;

        public ServletStream(ModuleSpace space, XmlElement self)
        {
            Space = space;
            Name = self.GetAttribute("name").Trim();
            Program.CheckReserveName(Name, space.Path());

            var tLevel = self.GetAttribute("TransactionLevel");
            if (tLevel.Length > 0)
                TransactionLevel = Enum.Parse<TransactionLevel>(tLevel);

            space.Add(this);
        }
    }
}
