#pragma once

#include "zeze/cxx/Rpc.h"
#include "Gen/demo/Module1/BOnlyData.hpp"

namespace demo {
namespace Module1 {
class RpcOnlyData : public Zeze::Net::Rpc<Zeze::EmptyBean, demo::Module1::BOnlyData> {
public:
    static const int ModuleId_ = 1;
    static const int ProtocolId_ = -1253772235; // 3041195061
    static const int64_t TypeId_ = Zeze::Net::Protocol::MakeTypeId(ModuleId_, ProtocolId_); // 7336162357

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
