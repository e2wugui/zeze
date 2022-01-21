
namespace Zeze.Net
{
    public sealed class SocketOptions
    {
        // 系统选项
        public bool? NoDelay { get; set; }
        public int? SendBuffer { get; set; }
        public int? ReceiveBuffer { get; set; }

        // 应用选项

        // 网络层接收数据 buffer 大小，大流量网络应用需要加大。
        public int InputBufferSize { get; set; }
        // 最大协议包的大小。协议需要完整收到才解析和处理，所以需要缓存。这是个安全选项。防止出现攻击占用大量内存。
        public int InputBufferMaxProtocolSize { get; set; }
        public int OutputBufferMaxSize { get; set; }

        // 系统选项，但没有默认，只有 ServerSocket 使用。
        public int Backlog { get; set; }

        // 其他杂项
        public NLog.LogLevel SocketLogLevel { get; set; }

        public SocketOptions()
        {
            // 这几个是应用层的选项，提供默认值。
            // 其他系统的选项不指定的话由系统提供默认值。
            InputBufferSize = 8192;
            InputBufferMaxProtocolSize = 2 * 1024 * 1024; // 2M
            OutputBufferMaxSize = int.MaxValue;
            Backlog = 128;
            NoDelay = true;

            SocketLogLevel = NLog.LogLevel.Trace; // 可以使用 NLog.LogLevel.FromString 从配置中读取
        }
    }
}
