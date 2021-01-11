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

        delegate void Delegate();

        public bool Running { get; set; } = true;

        public bool AppendLine(string line)
        {
            Delegate d = delegate { richTextBox1.AppendText(line + Environment.NewLine); };

            if (richTextBox1.InvokeRequired)
            {
                richTextBox1.BeginInvoke(d);
            }
            else
            {
                d();
            }
            return Running;
        }

        private void FormBuildProgress_Load(object sender, EventArgs e)
        {
            Build();
        }

        private ManualResetEvent WaitBuildExit = new ManualResetEvent(false);

        public void StopAndWait()
        {
            Running = false;
            WaitBuildExit.WaitOne();
        }

        private void Build()
        {
            try
            {
                FormMain.Instance.SaveAll();
                FormMain.Instance.Documents.LoadAllDocument();

                // verify
                FormMain.Instance.Documents.ForEachFile((Documents.File file) =>
                {
                    var doc = file.Document;
                    if (null == doc)
                        return true;
                    
                    if (doc.GridData.View != null)
                        return true; // 已经打开的文档，有即时验证。

                    int ErrorCount = 0;

                    FormMain.Instance.FormError.OnAddError = (GridData.Cell cell, Property.IProperty p, Property.ErrorLevel level, string desc) =>
                    {
                        if (cell.Row.GridData == doc.GridData)
                            ++ErrorCount;
                    };
                    doc.BuildGridData();
                    doc.GridData.VerifyAll();
                    if (ErrorCount > 0)
                    {
                        // 如果有错误，也显示出来。
                        TabPage tab = FormMain.Instance.NewTabPage(doc.RelateName);
                        DataGridView grid = (DataGridView)tab.Controls[0];
                        grid.SuspendLayout();
                        doc.GridData.View = grid;
                        FormMain.Instance.Tabs.Controls.Add(tab);
                        grid.ResumeLayout();
                        //tabs.SelectedTab = tab;
                        grid.Tag = doc;
                    }
                    else
                    {
                        // FormMain.Instance.FormError.RemoveErrorByGrid(doc.GridData);
                    }
                    FormMain.Instance.FormError.OnAddError = null;
                    return true;
                });

                if (FormMain.Instance.FormError.GetErrorCount() > 0)
                {
                    FormMain.Instance.FormError.Show();
                    MessageBox.Show("存在一些验证错误。停止Build。");
                    return;
                }

                // 输出服务器使用的配置数据。现在是xml格式。
                string serverDir = System.IO.Path.Combine(FormMain.Instance.ConfigProject.DataOutputDirectory, "Server");
                FormMain.Instance.Documents.ForEachFile((Documents.File file) =>
                {
                    string serverDocDir = System.IO.Path.Combine(serverDir, file.Parent.RelateName);
                    System.IO.Directory.CreateDirectory(serverDocDir);
                    string serverFileName = System.IO.Path.Combine(serverDocDir, file.Document.Name + ".xml");
                    file.Document.SaveAs(serverFileName, true, Property.DataOutputFlags.Server);
                    return true;
                });

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
                        return true;
                    });
                });
                if (hasDefaultError != null)
                {
                    MessageBox.Show(hasDefaultError.FullName() + " 默认值和类型不匹配。");
                    return;
                }

                Gen.cs.Main.Gen(FormMain.Instance, Property.DataOutputFlags.Server);

                switch (string.IsNullOrEmpty(FormMain.Instance.ConfigProject.ClientLanguage)
                    ? "cs" : FormMain.Instance.ConfigProject.ClientLanguage)
                {
                    case "cs":
                        Gen.cs.Main.Gen(FormMain.Instance, Property.DataOutputFlags.Client);
                        // 输出客户端使用的配置数据。xml格式。
                        string clientDir = System.IO.Path.Combine(FormMain.Instance.ConfigProject.DataOutputDirectory, "Client");
                        FormMain.Instance.Documents.ForEachFile((Documents.File file) =>
                        {
                            string clientDocDir = System.IO.Path.Combine(clientDir, file.Parent.RelateName);
                            System.IO.Directory.CreateDirectory(clientDocDir);
                            string clientFileName = System.IO.Path.Combine(clientDocDir, file.Document.Name + ".xml");
                            file.Document.SaveAs(clientFileName, true, Property.DataOutputFlags.Client);
                            return true;
                        });

                        break;

                    case "ts":
                        // 生成代码，数据也嵌入在代码中。
                        Gen.ts.Main.Gen(FormMain.Instance, Property.DataOutputFlags.Client);
                        break;

                    case "lua":
                        // 生成代码，数据也嵌入在代码中。
                        Gen.lua.Main.Gen(FormMain.Instance, Property.DataOutputFlags.Client);
                        break;

                    default:
                        MessageBox.Show("unkown client language: " + FormMain.Instance.ConfigProject.ClientLanguage);
                        break;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
            }
            WaitBuildExit.Set();
            buttonBreak.PerformClick();
        }
    }
}
