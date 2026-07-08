using System;
using System.Xml;
using Zeze.Transaction;

namespace Zeze.Gen
{
    public class Servlet
    {
        public ModuleSpace Space { get; private set; }
        public string Name { get; }
        public readonly int MaxContentLength = 8192;
        public TransactionLevel TransactionLevel { get; } = TransactionLevel.Serializable;

        public Servlet(ModuleSpace space, XmlElement self)
        {
            Space = space;
            Name= self.GetAttribute("name").Trim();
            Program.CheckReserveName(Name, space.Path());

            var tLength = self.GetAttribute("MaxContentLength");
            if (tLength.Length > 0)
                MaxContentLength = int.Parse(tLength);

            var tLevel = self.GetAttribute("TransactionLevel");
            if (tLevel.Length > 0)
                TransactionLevel = Enum.Parse<TransactionLevel>(tLevel);

            space.Add(this);
        }
    }
}
