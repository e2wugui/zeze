using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Gen.cs
{
    public class Main
    {
        public static void Gen(FormMain main, Property.DataOutputFlags flags)
        {
            switch (flags)
            {
                case Property.DataOutputFlags.Server:
                    foreach (var doc in main.Documents.Values)
                    {
                        BeanFormatter.Gen(main.ConfigProject.ServerSrcDirectory, doc, Property.DataOutputFlags.Server);
                    }
                    break;

                case Property.DataOutputFlags.Client:
                    foreach (var doc in main.Documents.Values)
                    {
                        BeanFormatter.Gen(main.ConfigProject.ClientSrcDirectory, doc, Property.DataOutputFlags.Client);
                    }
                    break;

            }
        }
    }
}
