using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace UnitTest
{
    [TestClass]
    public class GlobalSetUp
    {
        [AssemblyInitialize]
        public static void initialize(TestContext testContext)
        {
            var logConfig = new NLog.Config.LoggingConfiguration();
            var layout = NLog.Layouts.Layout.FromString("${longdate}|[${threadid}]|${callsite}|${level:uppercase=true}|${message}${onexception:${newline}${exception:format=tostring}${exception:format=StackTrace}}");
            logConfig.AddTarget("console",
                new NLog.Targets.ColoredConsoleTarget()
                {
                    Layout = layout
                });
#if Debug
            logConfig.AddRule(NLog.LogLevel.Trace, NLog.LogLevel.Fatal, "console");
#else
            logConfig.AddRule(NLog.LogLevel.Info, NLog.LogLevel.Fatal, "console");
#endif // Debug
            NLog.LogManager.Configuration = logConfig;
        }
    }
}
