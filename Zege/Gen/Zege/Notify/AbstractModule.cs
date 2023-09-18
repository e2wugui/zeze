// auto-generated

/*
	通用通知模块。也可以叫任务模块，或者TODO模块。一开始叫Notify，就一直这么叫了吧。
	a) 特性
	   1. 数量不限（客户端翻页）。
	   2. 会过期（可选？）。
	   3. 有名字的，相同名字刷新排序位置，但还是同一个通知。
	b) 用途
	   1. 好友添加待定请求。
*/
// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zege.Notify
{
    public abstract class AbstractModule : Zeze.IModule
    {
        public override string FullName => "Zege.Notify";
        public override string Name => "Notify";
        public override int Id => 4;

        public const int eNotifyNodeNotFound = 1;

        protected abstract System.Threading.Tasks.Task<long>  ProcessNotifyNodeLogBeanNotify(Zeze.Net.Protocol p);
    }
}
