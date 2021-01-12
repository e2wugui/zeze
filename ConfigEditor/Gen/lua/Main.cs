using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor.Gen.lua
{
    public class Main
    {
        public static void Gen(FormMain main, Property.DataOutputFlags flags, FormBuildProgress progress)
        {
            switch (flags)
            {
                case Property.DataOutputFlags.Client:
                    main.Documents.ForEachFile((Documents.File file) =>
                    {
                        progress.AppendLine($"生成lua客户端代码. {file.Document.RelateName}", Color.Black);
                        BeanFormatter.Gen(main.ConfigProject.ClientSrcDirectory, file.Document, Property.DataOutputFlags.Client);
                        return progress.Running;
                    });
                    break;
                default:
                    throw new Exception("lua. Client Only");
            }

        }
    }
}
