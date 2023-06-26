using System;
using Zeze.Transaction;

namespace Zeze.Util
{
    [AttributeUsage(AttributeTargets.Method)]
    public class DispatchModeAttribute : Attribute
    {
        public DispatchMode Mode { get; set; } = DispatchMode.Normal;
    }
}
