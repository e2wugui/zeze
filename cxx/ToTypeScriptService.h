
#pragma once

#include "Net.h"
#include <unordered_map>
#include <unordered_set>
#include "Protocol.h"
#include <mutex>

namespace Zeze
{
	namespace Net
	{
		class ToTypeScriptService : public Service
		{
			virtual void OnSocketClose(const std::shared_ptr<Socket>& sender, const std::exception* e) override
			{
				if (sender.get() == socket.get())
				{
					SetSocketClose(sender->SessionId);
					socket.reset();
				}
				Service::OnSocketClose(sender, e);
			}

			virtual void OnHandshakeDone(const std::shared_ptr<Socket>& sender) override
			{
				Service::OnHandshakeDone(sender);
				SetHandshakeDone(sender->SessionId);
			}

			virtual void OnSocketProcessInputBuffer(const std::shared_ptr<Socket>& sender, Zeze::Serialize::ByteBuffer& input) override
			{
				if (sender->IsHandshakeDone)
				{
					AppendInputBuffer(sender->SessionId, input);
					input.ReadIndex = input.WriteIndex;
				}
				else
				{
					Protocol::DecodeProtocol(this, sender, input);
				}
			}
        private:
            std::unordered_map<long long, Zeze::Serialize::ByteBuffer> ToBuffer;
            std::unordered_set<long long> ToHandshakeDone;
            std::unordered_set<long long> ToSocketClose;
            std::mutex mutex;

            void SetHandshakeDone(long long socketSessionId)
            {
                std::lock_guard<std::mutex> lock(mutex);
                {
                    ToHandshakeDone.insert(socketSessionId);
                }
            }

            void SetSocketClose(long long socketSessionId)
            {
                std::lock_guard<std::mutex> lock(mutex);
                {
                    ToSocketClose.insert(socketSessionId);
                }
            }

            void AppendInputBuffer(long long socketSessionId, Zeze::Serialize::ByteBuffer & buffer)
            {
                std::lock_guard<std::mutex> lock(mutex);
                {
                    ToBuffer[socketSessionId].Append((const char*)buffer.Bytes, buffer.ReadIndex, buffer.Size());
                }
            }

        public:
			ToTypeScriptService(const std::string & name) : Service(name)
			{

			}

            void Send(long long sessionId, Puerts.ArrayBuffer buffer, int offset, int len)
            {
                if (Socket* so = socket.get())
                {
                    if (so->SessionId == sessionId)
                    {
                        so->Send(buffer.Bytes, offset, len);
                    }
                }
            }

            void Close(long long sessionId)
            {
                if (Socket* so = socket.get())
                {
                    if (so->SessionId == sessionId)
                    {
                        so->Close(NULL);
                    }
                }
            }

            void TickUpdate()
            {
                std::unordered_set<long long> handshakeTmp;
                std::unordered_set<long long> socketCloseTmp;
                std::unordered_map<long long, Zeze::Serialize::ByteBuffer> inputTmp;
                std::lock_guard<std::mutex> lock(mutex);
                {
                    handshakeTmp.swap(ToHandshakeDone);
                    socketCloseTmp.swap(ToSocketClose);
                    inputTmp.swap(ToBuffer);
                }

                for (auto& e : socketCloseTmp)
                {
                    CallbackSocketClose(e);
                }

                for (auto& e : handshakeTmp)
                {
                    CallbackSocketHandshakeDone(e);
                }

                for (auto& e : inputTmp)
                {
                    CallbackSocketProcessInputBuffer(e.first, new Puerts.ArrayBuffer(e.second.Bytes), e.second.ReadIndex, e.second.Size());
                }
            }
        };
	}
}