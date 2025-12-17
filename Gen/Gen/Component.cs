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
                    new Zeze.Gen.cs.MakerComponent(this).Make();
                    break;

                case "zeze+java":
                    new Zeze.Gen.java.MakerComponent(this).Make();
                    break;

                case "conf+cs+net":
                    new Zeze.Gen.confcs.MakerComponent(this).Make();
                    break;

                default:
                    throw new Exception("Component: unsupport platform: " + Platform);
            }
            Program.FlushOutputs();
        }
    }
}
