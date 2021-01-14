using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor
{
    public partial class FormBuildProgress : Form
    {
        public FormBuildProgress()
        {
            InitializeComponent();
        }

        private delegate void Delegate();

        public void AppendLine(string line, Color color)
        {
            Delegate d = delegate
            {
                richTextBox1.SelectionColor = color;
                richTextBox1.AppendText(line + Environment.NewLine);
            };

            if (richTextBox1.InvokeRequired)
            {
                richTextBox1.BeginInvoke(d);
            }
            else
            {
                d();
            }
        }

        private void FormBuildProgress_Load(object sender, EventArgs e)
        {
            AsyncBuild();
        }

        private ManualResetEvent WaitBuildExit = new ManualResetEvent(false);
        public bool Running { get; private set; } = true;


        public void StopAndWait()
        {
            Running = false;
            WaitBuildExit.WaitOne();
        }

        private async void AsyncBuild()
        {
            await Task.Run(() => Build());
            Documents.CloseNotDependsByView();
            this.AppendLine($"Build 结束.", Color.Blue);
            buttonBreak.Text = "关闭";
        }

        private void Build()
        {
            try
            {
                FormMain.Instance.SaveAll();
                FormMain.Instance.Documents.LoadAllDocument(this);

                if (FormMain.Instance.FormError.GetErrorCount() > 0)
                {
                    FormMain.Instance.InvokeShowFormError();
                    this.AppendLine("当前打开的文档存在一些验证错误。停止Build。", Color.Red);
                    return;
                }

                // verify
                FormMain.Instance.Documents.ForEachFile((Documents.File file) =>
                {
                    var doc = file.Document;                
                    if (doc.GridData != null)
                        return Running; // 已经打开的文档，有即时验证。

                    this.AppendLine($"Verify {doc.RelateName}", Color.Black);
                    int ErrorCount = 0;
                    FormMain.Instance.FormError.OnAddError = (GridData.Cell cell, Property.IProperty p, Property.ErrorLevel level, string desc) =>
                    {
                        if (cell.Row.GridData == doc.GridData)
                            ++ErrorCount;
                    };
                    doc.BuildGridData();
                    doc.GridData.VerifyAll(false);
                    if (ErrorCount > 0)
                    {
                        FormMain.Instance.InvokeOpenGrid(doc, false);
                    }
                    else
                    {
                        // 等 FormError 处理了有 GridData 但是没有 View 的情况，可以不清除错误。
                        // FormMain.Instance.FormError.RemoveErrorByGrid(doc.GridData);
                    }
                    return Running;
                });
                FormMain.Instance.FormError.OnAddError = null;

                if (false == Running)
                    return;

                // 输出服务器使用的配置数据。现在是xml格式。
                string serverDir = System.IO.Path.Combine(FormMain.Instance.ConfigProject.DataOutputDirectory, "Server");
                FormMain.Instance.Documents.ForEachFile((Documents.File file) =>
                {
                    this.AppendLine($"导出服务器配置. {file.Document.RelateName}", Color.Black);
                    string serverDocDir = System.IO.Path.Combine(serverDir, file.Parent.RelateName);
                    System.IO.Directory.CreateDirectory(serverDocDir);
                    string serverFileName = System.IO.Path.Combine(serverDocDir, file.Document.Name + ".xml");
                    file.Document.SaveAs(serverFileName, true, Property.DataOutputFlags.Server);
                    return Running;
                });

                if (false == Running)
                    return;
                // check VarDefine.Default
                VarDefine hasDefaultError = null;
                FormMain.Instance.Documents.ForEachFile((Documents.File file) =>
                {
                    return file.Document.BeanDefine.ForEach((BeanDefine beanDefine) =>
                    {
                        foreach (var varDefine in beanDefine.Variables)
                        {
                            if (false == varDefine.CheckType(varDefine.TypeNow, varDefine.Default))
                            {
                                hasDefaultError = varDefine;
                                return false;
                            }
                        }
                        return Running;
                    });
                });
                if (hasDefaultError != null)
                {
                    this.AppendLine(hasDefaultError.FullName() + " 默认值和类型不匹配。", Color.Red);
                    return;
                }
                if (false == Running)
                    return;

                Gen.cs.Main.Gen(FormMain.Instance, Property.DataOutputFlags.Server, this);

                if (false == Running)
                    return;

                switch (string.IsNullOrEmpty(FormMain.Instance.ConfigProject.ClientLanguage)
                    ? "cs" : FormMain.Instance.ConfigProject.ClientLanguage)
                {
                    case "cs":
                        Gen.cs.Main.Gen(FormMain.Instance, Property.DataOutputFlags.Client, this);
                        // 输出客户端使用的配置数据。xml格式。
                        string clientDir = System.IO.Path.Combine(FormMain.Instance.ConfigProject.DataOutputDirectory, "Client");
                        FormMain.Instance.Documents.ForEachFile((Documents.File file) =>
                        {
                            this.AppendLine($"导出cs客户端数据. {file.Document.RelateName}", Color.Black);
                            string clientDocDir = System.IO.Path.Combine(clientDir, file.Parent.RelateName);
                            System.IO.Directory.CreateDirectory(clientDocDir);
                            string clientFileName = System.IO.Path.Combine(clientDocDir, file.Document.Name + ".xml");
                            file.Document.SaveAs(clientFileName, true, Property.DataOutputFlags.Client);
                            return Running;
                        });

                        break;

                    case "ts":
                        // 生成代码，数据也嵌入在代码中。
                        Gen.ts.Main.Gen(FormMain.Instance, Property.DataOutputFlags.Client, this);
                        break;

                    case "lua":
                        // 生成代码，数据也嵌入在代码中。
                        Gen.lua.Main.Gen(FormMain.Instance, Property.DataOutputFlags.Client, this);
                        break;

                    default:
                        this.AppendLine("unkown client language: " + FormMain.Instance.ConfigProject.ClientLanguage, Color.Red);
                        break;
                }
            }
            catch (Exception ex)
            {
                this.AppendLine(ex.ToString(), Color.Red);
            }
            finally
            {
                WaitBuildExit.Set();
            }
        }
    }
}
