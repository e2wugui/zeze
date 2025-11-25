#pragma once

#include <string>
#include <vector>
#include <unordered_set>
#include <unordered_map>
#include <mutex>
#include <algorithm>
#include <utility>
#include "ByteBuffer.h"
#include "LuaHelper.h"
#include "IDecodeAndDispatcher.h"

namespace Zeze
{
namespace Net
{
	using ByteBuffer = Zeze::ByteBuffer;
	class Service;
	class Socket;
	class ToLuaService;

	class ToLua : public IDecodeAndDispatcher
	{
		class BeanMeta;
		class VariableMeta
		{
		public:
			int Id{0};
			int Type{0};
			int Key{0};
			int Value{0};
			long long TypeBeanTypeId{0};
			long long KeyBeanTypeId{0};
			long long ValueBeanTypeId{0};
			std::string Name;

			// 这个变量目前仅用于dynamic访问得到var所在bean的名字，
			// 容器之类的不会直接包含dynamic，所以动态构造的var是没有初始化这个变量的。
			BeanMeta* BeanMeta{nullptr};

			VariableMeta()
			{
			}

			VariableMeta(int id, int type, long long typeBeanTypeId)
			{
				Id = id;
				Type = type;
				TypeBeanTypeId = typeBeanTypeId;
			}
		};
		class BeanMeta
		{
		public:
			std::string Name;
			std::vector<VariableMeta> Variables;
		};

		class ProtocolArgument
		{
		public:
			long long ArgumentBeanTypeId{0};
			long long ResultBeanTypeId{0};
			bool IsRpc{false};
		};

		LuaHelper Lua;

		typedef std::unordered_map<long long, BeanMeta> BeanMetasMap;
		typedef std::unordered_map<long long, ProtocolArgument> ProtocolMetasMap;

		BeanMetasMap BeanMetas; // Bean.TypeId -> vars
		ProtocolMetasMap ProtocolMetas; // protocol.TypeId -> Bean.TypeId

	public:
		ToLua()
		{
		}

		ToLua(const ToLua&) = delete;
		ToLua(const ToLua&&) = delete;
		ToLua& operator=(const ToLua&) = delete;
		ToLua& operator=(const ToLua&&) = delete;

		void LoadMeta(lua_State* L)
		{
			Lua.L = L;

			if (Lua.DoString("return (require 'Zeze')"))
				throw std::runtime_error("require 'Zeze' failed");

			BeanMetas.clear();
			ProtocolMetas.clear();

			if (Lua.DoString("return (require 'ZezeMeta')"))
				throw std::runtime_error("require 'ZezeMeta' failed");
			if (!Lua.IsTable(-1))
				throw std::runtime_error("require 'ZezeMeta' not return a table");
			Lua.GetField(-1, "beans");
			for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1)) // -1 value of vars(table) -2 key of bean.TypeId
			{
				long long beanTypeId = Lua.ToInteger(-2);
				BeanMeta& beanMeta = BeanMetas[beanTypeId];
				for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1)) // -1 value of varmeta(table) -2 key of varid
				{
					int varId = (int)Lua.ToInteger(-2);
					if (varId == 0)
					{
						beanMeta.Name = Lua.ToString(-1);
						continue;
					}
					VariableMeta var;
					var.BeanMeta = &beanMeta;
					var.Id = varId;
					for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1)) // -1 value of typetag -2 key of index
					{
						switch (Lua.ToInteger(-2))
						{
							case 1: var.Type = (int)Lua.ToInteger(-1); break;
							case 2: var.TypeBeanTypeId = Lua.ToInteger(-1); break;
							case 3: var.Key = (int)Lua.ToInteger(-1); break;
							case 4: var.KeyBeanTypeId = Lua.ToInteger(-1); break;
							case 5: var.Value = (int)Lua.ToInteger(-1); break;
							case 6: var.ValueBeanTypeId = Lua.ToInteger(-1); break;
							case 7: var.Name = Lua.ToString(-1); break;
							default: throw std::runtime_error("error index for typetag");
						}
					}
					beanMeta.Variables.push_back(var);
				}
				std::sort(beanMeta.Variables.begin(), beanMeta.Variables.end(), [](const auto& a, const auto& b)
				{
					return a.Id < b.Id;
				});
			}
			Lua.Pop(1);

			Lua.GetField(-1, "protocols");
			for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1)) // -1 value of Protocol.Argument(is table) -2 Protocol.TypeId
			{
				ProtocolArgument pa;
				for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1)) // -1 value of beantypeid -2 key of index
				{
					switch (Lua.ToInteger(-2))
					{
					case 1: pa.ArgumentBeanTypeId = Lua.ToInteger(-1); pa.IsRpc = false; break;
					case 2: pa.ResultBeanTypeId   = Lua.ToInteger(-1); pa.IsRpc = true;  break;
					default: throw std::runtime_error("error index for protocol argument bean typeId");
					}
				}
				ProtocolMetas[Lua.ToInteger(-2)] = pa;
			}
			Lua.Pop(1);
		}

		void CallSocketClose(Service* service, long long socketSessionId)
		{
			if (Lua.GetGlobal("ZezeSocketClose") != LuaHelper::LuaType::Function) // push func onto stack
			{
				Lua.Pop(1);
				return;
			}

			Lua.PushObject(service);
			Lua.PushInteger(socketSessionId);
			Lua.Call(2, 0);
		}

		void CallHandshakeDone(Service* service, long long socketSessionId)
		{
			if (Lua.GetGlobal("ZezeHandshakeDone") != LuaHelper::LuaType::Function) // push func onto stack
			{
				Lua.Pop(1);
				throw std::runtime_error("ZezeHandshakeDone is not a function");
			}

			Lua.PushObject(service);
			Lua.PushInteger(socketSessionId);
			Lua.Call(2, 0);
		}

		static int ZezeSendProtocol(lua_State* luaState);
		static int ZezeUpdate(lua_State* luaState);
		static int ZezeConnect(lua_State* luaState);

		void RegisterGlobalAndCallback(ToLuaService* service);
		void SendProtocol(Socket* socket);

		void EncodeBean(ByteBuffer& bb, long long beanTypeId)
		{
			if (!Lua.IsTable(-1))
				throw std::runtime_error("EncodeBean need a table");
			if (beanTypeId != 0)
			{
				BeanMetasMap::const_iterator bit = BeanMetas.find(beanTypeId);
				if (bit == BeanMetas.cend())
					throw std::runtime_error("bean not found in meta for beanTypeId=" + beanTypeId);
				int lastId = 0;
				for (const auto& v : bit->second.Variables)
				{
					Lua.PushInteger(v.Id);
					Lua.GetTable(-2);
					if (!Lua.IsNil(-1)) // allow var not set
						lastId = EncodeVariable(bb, v, lastId, -1);
					Lua.Pop(1);
				}
			}
			bb.WriteByte(0);
		}

		int EncodeGetTableLength()
		{
			if (!Lua.IsTable(-1))
				throw std::runtime_error("EncodeGetTableLength: not a table");
			int len = 0;
			for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1))
				len++;
			return len;
		}

		int EncodeVariable(ByteBuffer& bb, const VariableMeta& v, int lastId, int index)
		{
			int id = v.Id;
			switch (v.Type)
			{
			case ByteBuffer::LUA_BOOL:
			{
				bool vb = Lua.ToBoolean(index);
				if (vb || id <= 0)
				{
					if (id > 0)
						lastId = bb.WriteTag(lastId, id, ByteBuffer::INTEGER);
					bb.WriteBool(vb);
				}
				break;
			}
			case ByteBuffer::INTEGER:
			{
				long long vi = Lua.ToInteger(index);
				if (vi != 0 || id <= 0)
				{
					if (id > 0)
						lastId = bb.WriteTag(lastId, id, ByteBuffer::INTEGER);
					bb.WriteLong(vi);
				}
				break;
			}
			case ByteBuffer::FLOAT:
			{
				float vf = (float)Lua.ToNumber(index);
				if (vf != 0 || id <= 0)
				{
					if (id > 0)
						lastId = bb.WriteTag(lastId, id, ByteBuffer::FLOAT);
					bb.WriteFloat(vf);
				}
				break;
			}
			case ByteBuffer::DOUBLE:
			{
				double vd = Lua.ToNumber(index);
				if (vd != 0 || id <= 0)
				{
					if (id > 0)
						lastId = bb.WriteTag(lastId, id, ByteBuffer::DOUBLE);
					bb.WriteDouble(vd);
				}
				break;
			}
			case ByteBuffer::BYTES:
			{
				const std::string str = Lua.ToBuffer(index);
				if (!str.empty() || id <= 0)
				{
					if (id > 0)
						lastId = bb.WriteTag(lastId, id, ByteBuffer::BYTES);
					bb.WriteBytes(str);
				}
				break;
			}
			case ByteBuffer::LIST:
			{
				if (!Lua.IsTable(-1))
					throw std::runtime_erro("list must be a table");
				if (id <= 0)
					throw std::runtime_err("list cannot be defined in container");
				int n = EncodeGetTableLength();
				if (n > 0)
				{
					lastId = bb.WriteTag(lastId, id, ByteBuffer::LIST);
					bb.WriteListType(n, v.Value & ByteBuffer::TAG_MASK);
					for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1))
						EncodeVariable(bb, VariableMeta(0, v.Value, v.ValueBeanTypeId), 0, -1);
				}
				break;
			}
			case ByteBuffer::LUA_SET:
			{
				if (!Lua.IsTable(-1))
					throw std::runtime_err("set must be a table");
				if (id <= 0)
					throw std::runtime_err("set cannot be defined in container");
				int n = EncodeGetTableLength();
				if (n > 0)
				{
					lastId = bb.WriteTag(lastId, id, ByteBuffer::LIST);
					bb.WriteListType(n, v.Value & ByteBuffer::TAG_MASK);
					for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1))
						EncodeVariable(bb, VariableMeta(0, v.Value, v.ValueBeanTypeId), 0, -2); // set：encode key
				}
				break;
			}
			case ByteBuffer::MAP:
			{
				if (!Lua.IsTable(-1))
					throw std::runtime_error("map must be a table");
				if (id <= 0)
					throw std::runtime_error("map cannot be defined in container");
				int n = EncodeGetTableLength();
				if (n > 0)
				{
					lastId = bb.WriteTag(lastId, id, ByteBuffer::MAP);
					bb.WriteMapType(n, v.Key & ByteBuffer::TAG_MASK, v.Value & ByteBuffer::TAG_MASK);
					for (Lua.PushNil(); Lua.Next(-2); Lua.Pop(1))
					{
						EncodeVariable(bb, VariableMeta(0, v.Key, v.KeyBeanTypeId), 0, -2);
						EncodeVariable(bb, VariableMeta(0, v.Value, v.ValueBeanTypeId), 0, -1);
					}
				}
				break;
			}
			case ByteBuffer::BEAN:
				if (id > 0)
				{
					int a = bb.WriteIndex;
					int j = bb.WriteTag(lastId, id, ByteBuffer::BEAN);
					int b = bb.WriteIndex;
					EncodeBean(bb, v.TypeBeanTypeId);
					if (b + 1 == bb.WriteIndex) // only bean end mark
						bb.WriteIndex = a;
					else
						lastId = j;
				}
				else
					EncodeBean(bb, v.TypeBeanTypeId);
				break;
			case ByteBuffer::DYNAMIC:
			{
				if (id <= 0)
					throw std::runtime_error("dynamic cannot be defined in container");
				Lua.GetField(-1, "_TypeId_");
				if (Lua.IsNil(-1))
					throw std::runtime_error("'_TypeId_' not found. dynamic bean needed.");
				long long beanTypeId = Lua.ToInteger(-1);
				Lua.Pop(1);

				std::string funcName = "Zeze_GetRealBeanTypeIdFromSpecial_" + v.BeanMeta->Name + "_" + v.Name;
				if (Lua.GetGlobal(funcName.c_str()) != LuaHelper::LuaType::Function) // push func onto stack
				{
					Lua.Pop(1);
					throw std::runtime_error((funcName + " is not a function").c_str());
				}
				Lua.PushInteger(beanTypeId);
				Lua.Call(1, 1);
				long long realBeanTypeId = Lua.ToInteger(-1);
				Lua.Pop(1);

				if (id > 0)
					lastId = bb.WriteTag(lastId, id, ByteBuffer::DYNAMIC);
				bb.WriteLong(beanTypeId);
				EncodeBean(bb, realBeanTypeId);
				break;
			}
			default:
				throw std::runtime_erro("Unkown Tag Type");
			}
			return lastId;
		}
	public:
		virtual bool DecodeAndDispatch(Service* service, long long sessionId, int moduleId, int protocolId, ByteBuffer& bb) override;
		void CallRpcTimeout(long long sid);

	private:
		void DecodeBean(ByteBuffer& bb, long long typeId)
		{
			BeanMetasMap::const_iterator it = BeanMetas.find(typeId);
			const BeanMeta* const beanMeta = it != BeanMetas.cend() ? &it->second : nullptr;
			Lua.CreateTable(0, 32);
			for (int id = 0;;)
			{
				int t = bb.ReadByte();
				if (t == 0)
					return;
				id += bb.ReadTagSize(t);
				Lua.PushInteger(id);
				DecodeVariable(bb, id, t, beanMeta);
				Lua.SetTable(-3);
			}
		}

	private:
		static const VariableMeta* FindVarMeta(const BeanMeta* const beanMeta, int id)
		{
			if (!beanMeta)
				return nullptr;
			auto it = std::find_if(beanMeta->Variables.cbegin(), beanMeta->Variables.cend(), [id](const VariableMeta& varMeta)
			{
				return varMeta.Id == id;
			});
			return it != beanMeta->Variables.cend() ? &*it : nullptr;
		}

	public:
		void DecodeVariable(ByteBuffer& bb, int id, int type, const BeanMeta* const beanMeta)
		{
			switch (type)
			{
			case ByteBuffer::INTEGER:
			{
				const VariableMeta* const varMeta = FindVarMeta(beanMeta, id);
				if (varMeta && varMeta->Type == ByteBuffer::LUA_BOOL)
					Lua.PushBoolean(bb.ReadBool());
				else
					Lua.PushInteger(bb.ReadLong());
				break;
			}
			case ByteBuffer::FLOAT:
				Lua.PushNumber(bb.ReadFloat());
				break;
			case ByteBuffer::DOUBLE:
				Lua.PushNumber(bb.ReadDouble());
				break;
			case ByteBuffer::BYTES:
			{
				int outlen;
				const char* outstr = bb.ReadStringNoCopy(outlen);
				Lua.PushBuffer(outstr, outlen);
				break;
			}
			case ByteBuffer::LIST:
			{
				int t = bb.ReadByte();
				int n = bb.ReadTagSize(t);
				t &= ByteBuffer::TAG_MASK;
				const VariableMeta* const varMeta = FindVarMeta(beanMeta, id);
				if (varMeta && varMeta->Type == ByteBuffer::LUA_SET)
				{
					Lua.CreateTable(0, std::min(n, 1000));
					for (; n > 0; n--)
					{
						DecodeVariable(bb, 0, t, nullptr);
						Lua.PushInteger(0);
						Lua.SetTable(-3);
					}
				}
				else
				{
					Lua.CreateTable(std::min(n, 1000), 0);
					for (int i = 1; i <= n; i++) // 从1开始？
					{
						Lua.PushInteger(i);
						DecodeVariable(bb, 0, t, nullptr);
						Lua.SetTable(-3);
					}
				}
				break;
			}
			case ByteBuffer::MAP:
			{
				int t = bb.ReadByte();
				int s = t >> ByteBuffer::TAG_SHIFT;
				t &= ByteBuffer::TAG_MASK;
				int n = bb.ReadUInt();
				Lua.CreateTable(0, std::min(n, 1000));
				for (; n > 0; n--)
				{
					DecodeVariable(bb, 0, s, nullptr);
					DecodeVariable(bb, 0, t, nullptr);
					Lua.SetTable(-3);
				}
				break;
			}
			case ByteBuffer::BEAN:
			{
				const VariableMeta* const varMeta = FindVarMeta(beanMeta, id);
				DecodeBean(bb, varMeta ? varMeta->TypeBeanTypeId : 0);
				break;
			}
			case ByteBuffer::DYNAMIC:
			{
				long long beanTypeId = bb.ReadLong();
				DecodeBean(bb, beanTypeId);
				// 动态bean额外把TypeId加到变量里面。总是使用varid==0表示。程序可以使用这个动态判断是哪个具体的bean。
				Lua.PushInteger(0);
				Lua.PushInteger(beanTypeId);
				Lua.SetTable(-3);
				break;
			}
			default:
				throw std::runtime_erro("Unkown Tag Type");
			}
		}

	private:
		typedef std::unordered_map<long long, std::string> ToLuaBufferMap;
		typedef std::unordered_map<long long, Service*> ToLuaHandshakeDoneMap;
		typedef std::unordered_map<long long, Service*> ToLuaSocketCloseMap;
		ToLuaBufferMap ToLuaBuffer;
		ToLuaHandshakeDoneMap ToLuaHandshakeDone;
		ToLuaSocketCloseMap ToLuaSocketClose;
		std::unordered_set<long long> ToLuaRpcTimeout;
		std::mutex mutex;

	public:
		void SetRpcTimeout(long long sid)
		{
			std::lock_guard<std::mutex> lock(mutex);
			ToLuaRpcTimeout.insert(sid);
		}

		void SetHandshakeDone(long long socketSessionId, Service* service)
		{
			std::lock_guard<std::mutex> lock(mutex);
			ToLuaHandshakeDone[socketSessionId] = service;
		}

		void SetSocketClose(long long socketSessionId, Service* service)
		{
			std::lock_guard<std::mutex> lock(mutex);
			ToLuaSocketClose[socketSessionId] = service;
		}

		void AppendInputBuffer(long long socketSessionId, ByteBuffer& buffer)
		{
			std::lock_guard<std::mutex> lock(mutex);
			ToLuaBuffer[socketSessionId].append((const char*)(buffer.Bytes + buffer.ReadIndex), buffer.Size());
		}

		void Update(Service* service);
	};

} // namespace Net
} // namespace Zeze
