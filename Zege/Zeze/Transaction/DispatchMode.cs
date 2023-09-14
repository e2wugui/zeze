namespace Zeze.Transaction
{
    public enum DispatchMode
    {
        // 由于c#只有一个线程池，下面的模式全部都没有实现。
        Normal, // 在普通线程池中执行。
        Critical, // 在重要线程池中执行。
        Direct, // 在调用者线程执行。

        // 这个定义是给maui程序准备的，需要自己重载Service实现到UIThread的派发。
        UIThread
    }
}
