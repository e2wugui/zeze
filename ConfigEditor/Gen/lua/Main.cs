using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor.Gen.lua
{
    public class Main
    {
        public static void Gen(FormMain main, Property.DataOutputFlags flags)
        {
            switch (flags)
            {
                case Property.DataOutputFlags.Client:
                    foreach (var doc in main.Documents.Values)
                    {
                        BeanFormatter.Gen(main.ConfigProject.ClientSrcDirectory, doc, Property.DataOutputFlags.Client);
                    }
                    break;
                default:
                    throw new Exception("lua. Client Only");
            }

        }
    }
}
