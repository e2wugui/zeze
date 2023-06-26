namespace Zeze.Net
{
    public sealed class SocketOptions
    {
        // 系统选项
        public bool? NoDelay;
        public int? SendBuffer;
        public int? ReceiveBuffer;

        // 应用选项

        // 网络层接收数据 buffer 大小，大流量网络应用需要加大。
        public int InputBufferSize;

        // 最大协议包的大小。协议需要完整收到才解析和处理，所以需要缓存。这是个安全选项。防止出现攻击占用大量内存。
        public int InputBufferMaxProtocolSize;
        public int OutputBufferMaxSize;

        // 系统选项，但没有默认，只有 ServerSocket 使用。
        public int Backlog;

        // 其他杂项
        public Config.LogLevel SocketLogLevel;
        public string TimeThrottle;
        public int? TimeThrottleSeconds;
        public int? TimeThrottleLimit;
        public int? TimeThrottleBandwidth;
        public long? OverBandwidth;
        public double OverBandwidthFusingRate = 1.0;
        public double OverBandwidthNormalRate = 0.7;

        public SocketOptions()
        {
            // 这几个是应用层的选项，提供默认值。
            // 其他系统的选项不指定的话由系统提供默认值。
            InputBufferSize = 8192;
            InputBufferMaxProtocolSize = 2 * 1024 * 1024; // 2M
            OutputBufferMaxSize = int.MaxValue;
            Backlog = 128;
            NoDelay = true;

            SocketLogLevel = Config.LogLevel.Trace; // 可以使用 NLog.LogLevel.FromString 从配置中读取
        }
    }
}
