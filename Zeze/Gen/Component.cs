using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;

namespace Zeze.Gen
{
    public class Component : Project
    {
        public Component(Solution sol, XmlElement self)
            : base(sol, self)
        {
        }

        protected override void MakePlatform()
        {
            switch (Platform)
            {
                case "zeze+cs":
                    new Zeze.Gen.cs.MakerZeze(this).Make();
                    break;

                case "zeze+java":
                    break;

                default:
                    throw new Exception("Component: unsupport platform: " + Platform);
            }
        }
    }
}
