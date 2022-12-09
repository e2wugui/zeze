
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;
using static Google.Protobuf.WellKnownTypes.Field.Types;
using System.Xml.Linq;

namespace Zeze.Gen.Types
{
    public class External : Bean
    {
        public override string FullName => _name;

        public External(Solution sol, XmlElement self)
        {
            Space = sol;
            _name = self.GetAttribute("bean");
            Kind = self.GetAttribute("kind");
            if (string.IsNullOrEmpty(Kind))
                Kind = "bean"; // default
            Program.CheckReserveName(_name);

            if (Types.ContainsKey(_name))
                throw new Exception("duplicate type: " + _name);
            Types.Add(_name, this);
            Console.WriteLine($"external {_name}");
        }

        public override void Depends(HashSet<Type> includes)
        {
        }
    }
}
