using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;
using Zeze.Transaction;

namespace Zeze.Gen
{
    public class ServletStream
    {
        public ModuleSpace Space { get; private set; }
        public string Name { get; set; }
        public TransactionLevel TransactionLevel { get; } = TransactionLevel.Serializable;

        public ServletStream(ModuleSpace space, XmlElement self)
        {
            Space = space;
            Name = self.GetAttribute("name").Trim();
            Program.CheckReserveName(Name, space.Path());

            var tlevel = self.GetAttribute("TransactionLevel");
            if (tlevel.Length > 0)
                TransactionLevel = (TransactionLevel)TransactionLevel.Parse(typeof(TransactionLevel), tlevel);

            space.Add(this);
        }
    }
}
