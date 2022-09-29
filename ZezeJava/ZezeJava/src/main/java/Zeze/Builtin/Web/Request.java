// auto-generated @formatter:off
package Zeze.Builtin.Web;

/*
			多数情况下，Http请求处理一个rpc完成。当需要处理上传大文件或下载大文件时，使用流接口处理。
			1. 普通请求
               linkd -> Rpc Request -> server
			2. 上传文件
			   linkd -> Rpc Request -> server
			   linkd -> Rpc RequestInputStream -> server
			   ... until Finish
			3. 下载文件
			   linkd -> Rpc Request -> server
			   server -> Rpc ResponseOutputStream -> linkd
			   ... until Finish
			4. 上传下载可以同时进行，但一般不会有这种应用。
*/
public class Request extends Zeze.Net.Rpc<Zeze.Builtin.Web.BRequest, Zeze.Builtin.Web.BResponse> {
    public static final int ModuleId_ = 11102;
    public static final int ProtocolId_ = -117897707; // 4177069589
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47686903989781

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Request() {
        Argument = new Zeze.Builtin.Web.BRequest();
        Result = new Zeze.Builtin.Web.BResponse();
    }

    public Request(Zeze.Builtin.Web.BRequest arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Web.BResponse();
    }
}
