﻿using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor
{
    /// <summary>
    /// BeanDefine 名字空间管理。
    /// 根据 Config-Home 目录建立。
    /// 主要用于查找 BeanDefine。
    /// 
    /// 由于名字空间里面的名字不包含文件名后缀，所以可能和路径名冲突，导致名字路径就是唯一的，有歧义。
    /// *** 为了避免这种情况，禁止文件名和路径名一样。
    /// *** 初始化和新建文件的时候需要检查。
    /// </summary>
    public class Documents
    {
        public class File
        {
            public string Name { get; private set; }
            public string AbsoluteName { get; private set; }
            public string RelateName { get; private set; }
            public Directory Parent { get; }

            public Document Document { get; private set; }

            public File(string path, Directory parent)
            {
                this.Parent = parent;
                this.Name = System.IO.Path.GetFileNameWithoutExtension(path);
                this.AbsoluteName = System.IO.Path.GetFullPath(path);
                var home = FormMain.Instance.ConfigEditor.GetHome();
                this.RelateName = this.AbsoluteName.Substring(home.Length);
                if (this.RelateName.StartsWith(System.IO.Path.DirectorySeparatorChar.ToString()))
                    this.RelateName = this.RelateName.Substring(1);
            }

            // 这个是 SaveAs 调用，需要更新Document（修改引用等操作）
            public void Rename(string path)
            {
                // 移动文件到目录需要根据路径建立目录树，而且需要提前 VerifyName。
                File newFile = FormMain.Instance.Documents.OpenFile(path, true);
                if (null == newFile || newFile == this)
                    return;

                OpenDocument(false);

                // 装载完就可以移动文件了。虽然马上不会用到。这样可以把文件系统错误提前暴露出来，避免修改到最后才报错。
                System.IO.File.Move(this.AbsoluteName, newFile.AbsoluteName);

                // use old full-name
                Document.BeanDefine.RemoveReferenceFromMe();

                // 把打开的 Document 移到 newFile
                newFile.Document = Document;
                Document.File = newFile;

                // update to new full-name
                Document.UpdateRelateName();
                Document.BeanDefine.SetNameOnly(Document.Name);
                Document.BeanDefine.AddReferenceFromMe();
                Document.BeanDefine.UpdateReferenceFroms();

                Parent.RemoveFile(this.Name);
                Document.IsChanged = true;
                Document.SaveIfChanged();
            }

            // 这个方法仅在 Document.BeanDefine 改名的时候调用。所以不能再去更新: 修改BeanDefine.Name。 
            internal void RenameWhenRootBeanDefineNameChange(string nameOnlyWithoutExtension)
            {
                if (this.Name.Equals(nameOnlyWithoutExtension))
                    return;

                var oldAbsoluteName = this.AbsoluteName;
                Parent.RemoveFile(this.Name);
                this.Name = nameOnlyWithoutExtension;
                Parent.AddFile(this);
                this.AbsoluteName = System.IO.Path.Combine(Parent.AbsoluteName, this.Name + ".xml");
                var home = FormMain.Instance.ConfigEditor.GetHome();
                this.RelateName = this.AbsoluteName.Substring(home.Length);
                if (this.RelateName.StartsWith(System.IO.Path.DirectorySeparatorChar.ToString()))
                    this.RelateName = this.RelateName.Substring(1);

                Document?.UpdateRelateName();
                System.IO.File.Move(oldAbsoluteName, this.AbsoluteName);
            }

            public Document OpenDocument(bool isOpenOrNew = false)
            {
                if (null != Document)
                    return Document;

                if (System.IO.File.Exists(AbsoluteName))
                {
                    Document = Load(isOpenOrNew);
                    // 必须在 Document 设置之后初始化引用。
                    // XXX 异步装载，在 FormMain.OpenGrid 里面调用这个。
                    Document?.BeanDefine.InitializeReference();
                }
                else
                {
                    Document = new Document(this);
                    Document.Save(); // 马上创建文件。免得没有修改的话，不会保存。
                }
                return Document;
            }

            private Task Loading;
            private Document Load(bool isOpenOrNew)
            {
                if (null != Loading)
                {
                    if (isOpenOrNew)
                        return null;

                    Loading?.Wait();
                    return Document;
                }
                var doc = new Document(this);
                Loading = new Task(() =>
                {
                    doc.LoadXmlFile();
                    Document = doc; // 后续所有的 Open 调用都能看到 Document 了。
                    Loading = null; // 仅仅清除，设置了 Document 变量以后，这个不会被访问了。
                    // 把执行权限交给 FormMain 继续 OpenOrNew
                    if (isOpenOrNew)
                        FormMain.Instance.InvokeOpenGrid(doc);
                });
                Loading.Start();
                if (isOpenOrNew)
                    return null;

                Loading?.Wait();
                return Document;
            }

            public void Close(Document doc)
            {
                Loading = null; // 在 Load 完成以后就可以清除了。这里保险起见。
                Document = null;
            }
        }

        public class Directory
        {
            public string Name { get; }
            public string AbsoluteName { get; }
            public string RelateName { get; }
            public Directory Parent { get; }

            private Dictionary<string, File> Files = new Dictionary<string, File>();
            private Dictionary<string, Directory> Directorys = new Dictionary<string, Directory>();

            private File Create(string[] paths, ref int offset)
            {
                var name = paths[offset];
                var err = Tools.VerifyName(name, CheckNameType.CheckOnly);
                if (null != err)
                {
                    MessageBox.Show($"不正确的名字'{name}':{err}");
                    return null;
                }
                if (offset == paths.Length - 1)
                {
                    var file = new File(System.IO.Path.Combine(AbsoluteName, name + ".xml"), this);
                    AddFile(file);
                    return file;
                }
                var directory = new Directory(System.IO.Path.Combine(AbsoluteName, name), this);
                if (AddDirectory(directory))
                {
                    // 虽然现在使用上不会存在目录不存在。但是这样更好，允许外面提供包含多个不存在子目录的路径。
                    System.IO.Directory.CreateDirectory(directory.AbsoluteName);
                    ++offset;
                    return directory.Create(paths, ref offset);
                }
                throw new Exception("AddDirectory fail!!!");
            }

            public File SearchFile(string[] paths, ref int offset, bool create)
            {
                if (offset >= paths.Length)
                {
                    return null;
                }
                var name = paths[offset];
                if (Files.TryGetValue(name, out var file))
                {
                    return file;
                }
                if (Directorys.TryGetValue(name, out var directory))
                {
                    ++offset;
                    return directory.SearchFile(paths, ref offset, create);
                }
                if (create)
                {
                    return Create(paths, ref offset);
                }
                return null;
            }

            internal void RemoveFile(string name)
            {
                Files.Remove(name);
            }

            internal void AddFile(File file)
            {
                var err = Tools.VerifyName(file.Name, CheckNameType.CheckOnly);
                if (null != err)
                {
                    MessageBox.Show($"名字不正确: {err} 忽略掉此文件。\r\n{file.AbsoluteName}");
                    return;
                }
                Files.Add(file.Name, file);
            }

            private bool AddDirectory(Directory dir)
            {
                if (Files.ContainsKey(dir.Name))
                {
                    MessageBox.Show($"目录名和文件名（不包括后缀）重复，这个目录及以下的文件将不能访问。\r\n{dir.AbsoluteName}");
                    return false;
                }
                Directorys.Add(dir.Name, dir);
                return true;
            }

            public Directory(string path, Directory parent)
            {
                // 系统返回的路径名是不包括最后的分隔符的。
                // 保险起见：如果存在就去掉。
                // 后面使用 GetFileName 得到最后一个路径的名字，有这个分割符时不能工作。
                if (path.EndsWith(System.IO.Path.DirectorySeparatorChar.ToString()))
                    path = path.Substring(0, path.Length - 1);
                this.AbsoluteName = System.IO.Path.GetFullPath(path);
                var home = FormMain.Instance.ConfigEditor.GetHome();
                this.RelateName = this.AbsoluteName.Substring(home.Length);
                if (this.RelateName.StartsWith(System.IO.Path.DirectorySeparatorChar.ToString()))
                    this.RelateName = this.RelateName.Substring(1);
                if (this.RelateName.EndsWith(System.IO.Path.DirectorySeparatorChar.ToString()))
                    this.RelateName = this.RelateName.Substring(0, this.RelateName.Length - 1);
                this.Name = System.IO.Path.GetFileName(path);
                this.Parent = parent;
            }

            public void Build(string dir)
            {
                foreach (var path in System.IO.Directory.GetFiles(dir, "*.xml"))
                {
                    AddFile(new File(path, this));
                }
                foreach (var path in System.IO.Directory.GetDirectories(dir))
                {
                    var directory = new Directory(path, this);
                    var err = Tools.VerifyName(directory.Name, CheckNameType.CheckOnly);
                    if (null != err)
                    {
                        MessageBox.Show($"不正确的名字：{err}\r\n忽略掉这个子目录及一下的所有文件{path}");
                        continue;
                    }
                    if (AddDirectory(directory))
                        directory.Build(path);
                }
            }

            public bool ForEachFile(Func<File, bool> action)
            {
                foreach (var file in Files.Values)
                {
                    if (false == action(file))
                        return false;
                }
                foreach (var dir in Directorys.Values)
                {
                    if (false == dir.ForEachFile(action))
                        return false;
                }
                return true;
            }
        }

        private Directory Root;

        public Documents()
        {
        }

        public void Build()
        {
            var home = FormMain.Instance.ConfigEditor.GetHome();
            Root = new Directory(home, null);
            Root.Build(home);
        }

        public BeanDefine SearchReference(string [] paths)
        {
            int offset = 0;
            File file = Root.SearchFile(paths, ref offset, false);
            return file?.OpenDocument().BeanDefine.Search(paths, offset + 1);
        }

        public BeanDefine SearchReference(string referenceName)
        {
            return SearchReference(referenceName.Split('.'));
        }

        public File OpenFile(string fileName, bool create)
        {
            var home = FormMain.Instance.ConfigEditor.GetHome();
            var full = System.IO.Path.GetFullPath(fileName);
            if (false == full.StartsWith(home))
            {
                MessageBox.Show($"不能打开不在Home目录下的文件。\r\nHome={home}");
                return null;
            }
            if (false == full.EndsWith(".xml"))
            {
                MessageBox.Show($"不是xml文件。{fileName}");
                return null;
            }
            var relate = full.Substring(home.Length);
            if (relate.StartsWith(System.IO.Path.DirectorySeparatorChar.ToString()))
                relate = relate.Substring(1);
            if (relate.EndsWith(".xml"))
                relate = relate.Substring(0, relate.Length - 4);

            int offset = 0;
            string[] paths = relate.Split(new char[] { '\\', '/' });
            File file = Root.SearchFile(paths, ref offset, create);
            if (null == file)
                return null;

            if (offset != paths.Length - 1)
            {
                MessageBox.Show($"路径还没有搜索完就找到了文件，肯定某个目录下的路径名和文件名重复了。\r\n当前文件：{file.AbsoluteName}");
                return null;
            }
            return file;
        }

        public bool ForEachFile(Func<File, bool> action)
        {
            return Root.ForEachFile(action);
        }

        public void LoadAllDocument(FormBuildProgress progress = null)
        {
            ForEachFile((File file) =>
            {
                if (file.Document == null)
                {
                    file.OpenDocument();
                    progress?.AppendLine($"Load  {file.Document.RelateName}", Color.Black);
                }
                return true;
            });
        }

        public static void CloseNotDependsByView()
        {
            HashSet<BeanDefine> deps = new HashSet<BeanDefine>();
            foreach (var tab in FormMain.Instance.Tabs.Controls)
            {
                var doc = (tab as TabPage).Controls[0].Tag as Document;
                doc.BeanDefine.Depends(deps);
            }
            HashSet<Document> docs = new HashSet<Document>();
            foreach (var bean in deps)
            {
                docs.Add(bean.Document);
            }
            FormMain.Instance.Documents.ForEachFile((Documents.File file) =>
            {
                if (null == file.Document)
                    return true;
                if (docs.Contains(file.Document))
                    return true;
                file.Document.Close();
                return true;
            });
        }
    }
}
