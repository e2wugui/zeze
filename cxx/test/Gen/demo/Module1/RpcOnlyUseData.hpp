#pragma once

#include "zeze/cxx/Rpc.h"
#include "Gen/demo/Module1/BOnlyData.hpp"

namespace demo {
namespace Module1 {
class RpcOnlyUseData : public Zeze::Net::Rpc<demo::Module1::BOnlyData, Zeze::EmptyBean> {
public:
    static const int ModuleId_ = 1;
    static const int ProtocolId_ = -651743921; // 3643223375
    static const int64_t TypeId_ = Zeze::Net::Protocol::MakeTypeId(ModuleId_, ProtocolId_); // 7938190671

    virtual int ModuleId() const override {
        return ModuleId_;
    }

    virtual int ProtocolId() const override {
        return ProtocolId_;
    }

    int64_t TypeId() const {
        return TypeId_;
    }
};
}
}
