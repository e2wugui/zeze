using System;
using System.Collections.Generic;
using System.Xml;

namespace Zeze.Gen.Types
{
    public class External : Bean
    {
        public override string FullName => _name;

        public External(ModuleSpace sol, XmlElement self)
        {
            Space = sol;
            _name = self.GetAttribute("bean");
            Kind = "bean";
            Program.CheckReserveFullName(_name, sol.Path());

            if (Types.ContainsKey(_name))
                throw new Exception("duplicate type: " + _name);
            Types.Add(_name, this);
            //Console.WriteLine($"external {_name}");
        }

        public override void Depends(HashSet<Type> includes, string parent)
        {
        }
    }
}
