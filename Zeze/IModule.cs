using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze
{
    public interface IModule
    {
        public string FullName { get; }
        public string Name { get; }
        public int Id { get; }

        public void UnRegister(); // 为了重新装载 Module 的补丁。注册在构造函数里面进行。
    }
}
