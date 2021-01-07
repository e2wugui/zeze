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
                    main.Documents.ForEachFile((Documents.File file) =>
                    {
                        BeanFormatter.Gen(main.ConfigProject.ClientSrcDirectory, file.Document, Property.DataOutputFlags.Client);
                        return true;
                    });
                    break;
                default:
                    throw new Exception("lua. Client Only");
            }

        }
    }
}
