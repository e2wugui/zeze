namespace Zeze.Util
{
    // Gen 工具只需要 confcs 运行时中 ConfBean 的常量定义，这里保留同名常量，避免引入整个 confcs 依赖。
    // 值必须和 confcs/Zeze/Util/ConfBean.cs 保持一致。
    public static class ConfBean
    {
        public const int ObjectIdStep = 4096;
        public const int MaxVariableId = ObjectIdStep - 1;
    }

    public static class ConfEmptyBean
    {
        public const long TYPEID = 0;
    }
}
