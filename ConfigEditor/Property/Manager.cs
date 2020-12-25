using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor.Property
{
    public class Manager
    {
        public Dictionary<string, IProperty> Properties { get; } = new Dictionary<string, IProperty>();

        public void AddProperty(IProperty p)
        {
            Properties.Add(p.Name, p);
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

        public Manager()
        {
            AddProperty(new Id());
            AddProperty(new Unique());

            AddProperty(new Client());
            AddProperty(new Server());

            AddProperty(new Dns());
            AddProperty(new File());
            AddProperty(new Url());
        }
    }
}
