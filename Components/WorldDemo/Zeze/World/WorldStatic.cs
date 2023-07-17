
using Zeze.Net;

namespace Zeze.World
{
    /// <summary>
    /// 这个模块用来定义地图组件需要的静态绑定的协议。
    /// 现在只有服务器处理的SwitchWorld协议，由于发送代码写在哪里跟定义在那个模块无关，
    /// 这个协议的发送代码写到World模块中了。
    /// 以后，当需要增加客户端处理的协议时，处理入口会在这个模块里面。
    /// </summary>
    public class WorldStatic : AbstractWorldStatic
    {
        public WorldStatic(Service service)
        {
            RegisterProtocols(service);
        }
    }
}
