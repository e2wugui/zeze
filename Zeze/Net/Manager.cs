using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Net
{
    public class Manager
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private Dictionary<long, ASocket> _asocketMap = new Dictionary<long, ASocket>();

        /// <summary>
        /// 只包含成功建立的连接：服务器Accept和客户端Connected的连接。
        /// </summary>
        /// <param name="serialNo"></param>
        /// <returns></returns>
        public virtual ASocket GetASocket(long serialNo)
        {
            lock (_asocketMap)
            {
                ASocket value = null;
                if (_asocketMap.TryGetValue(serialNo, out value))
                    return value;
                return null;
            }
        }
        /// <summary>
        /// ASocket 关闭的时候总是回调。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="e"></param>
        public virtual void OnSocketClose(ASocket so, Exception e)
        {
            lock (_asocketMap)
            {
                _asocketMap.Remove(so.SerialNo);
            }
            logger.Debug(e, "OnSocketClose");
        }

        /// <summary>
        /// 服务器接受到新连接回调。
        /// </summary>
        /// <param name="so"></param>
        public virtual void OnSocketAccept(ASocket so)
        {
            lock(_asocketMap)
            {
                _asocketMap.Add(so.SerialNo, so);
            }
        }

        /// <summary>
        /// 连接失败回调。同时也会回调OnSocketClose。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="e"></param>
        public virtual void OnSocketConnectError(ASocket so, Exception e)
        {
            logger.Debug(e, "OnSocketConnectError");
        }

        /// <summary>
        /// 连接成功回调。
        /// </summary>
        /// <param name="so"></param>
        public virtual void OnSocketConnected(ASocket so)
        {
            lock (_asocketMap)
            {
                _asocketMap.Add(so.SerialNo, so);
            }
            Console.WriteLine("OnSocketConnected: " + so.SerialNo);
            string head = "HEAD http://www.163.com/\r\nHost: www.163.com\r\nAccept:*/*\r\n\r\n";
            so.Send(head);
        }

        /// <summary>
        /// 处理数据。
        /// 在异步线程中回调，要注意线程安全。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="input"></param>
        public virtual void OnSocketProcessInputBuffer(ASocket so, Zeze.Serialize.ByteBuffer input)
        {
            Console.WriteLine("OnSocketProcessInputBuffer: " + so.SerialNo);
            Console.WriteLine(Encoding.UTF8.GetString(input.Bytes, input.ReadIndex, input.Size));
            input.Reset(); // skip all data
        }
    }
}
