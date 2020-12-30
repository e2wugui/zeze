using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor.Property
{
    public class Manager
    {
        public SortedDictionary<string, IProperty> Properties { get; } = new SortedDictionary<string, IProperty>();
        public SortedDictionary<string, IProperty> BuildIns { get; } = new SortedDictionary<string, IProperty>();

        public SortedDictionary<Group, List<IProperty>> SortedGroup()
        {
            SortedDictionary<Group, List<IProperty>> group = new SortedDictionary<Group, List<IProperty>>();
            foreach (var e in Properties)
            {
                if (e.Value.BuildIn)
                    continue; // skip BuildIn type.

                if (false == group.TryGetValue(e.Value.Group, out var exist))
                    group.Add(e.Value.Group, exist = new List<IProperty>());
                exist.Add(e.Value);
            }
            return group;
        }

        public void AddProperty(IProperty p)
        {
            Properties.Add(p.Name, p);
        }


        public void AddBuildIn(IProperty p)
        {
            if (false == p.BuildIn)
                throw new Exception("Not A Build Property.");
            BuildIns.Add(p.Name, p);
        }

        public List<IProperty> Parse(string properties)
        {
            List<IProperty> result = new List<IProperty>();

            if (null == properties)
                return result;

            foreach (var p in properties.Split(';'))
            {
                if (Properties.TryGetValue(p, out var ip))
                    result.Add(ip);
            }
            return result;
        }

        public string BuildToolTipText(string properties)
        {
            StringBuilder sb = new StringBuilder();
            foreach (var p in Parse(properties))
            {
                sb.Append(p.Name).Append(": ").Append(p.Comment).Append(Environment.NewLine);
            }
            return sb.ToString();
        }

        public string BuildString(List<IProperty> ps)
        {
            StringBuilder sb = new StringBuilder();
            foreach (var p in ps)
            {
                sb.Append(p.Name).Append(";");
            }
            return sb.ToString();
        }

        public Manager()
        {
            // Normal
            AddProperty(new Id());
            AddProperty(new Unique());
            AddProperty(new UniqueList());
            AddProperty(new IdList());

            // GenTarget
            AddProperty(new Client());
            AddProperty(new Server());

            // DataType
            AddProperty(new Dns());
            AddProperty(new File());
            AddProperty(new Url());
            AddProperty(new Directory());

            // BuildIn type. 不做校验的几个先不注册进去。这些不会在FormProperties中编辑。
            //AddBuildIn(new UndecidedVerify());
            AddBuildIn(new IntVerify());
            AddBuildIn(new LongVerify());
            AddBuildIn(new DoubleVerify());
            //AddBuildIn(new StringVerify());
            //AddBuildIn(new ListVerify());
            AddBuildIn(new FloatVerify());
            AddBuildIn(new EnumVerify());
        }
    }
}
