#pragma once

#include "zeze/cxx/Protocol.h"
#include "Gen/demo/Module1/BOnlyData.hpp"

namespace demo {
namespace Module1 {
class ProtocolOnlyData : public Zeze::Net::ProtocolWithArgument<demo::Module1::BOnlyData> {
public:
    static const int ModuleId_ = 1;
    static const int ProtocolId_ = -1918026873; // 2376940423
    static const int64_t TypeId_ = Zeze::Net::Protocol::MakeTypeId(ModuleId_, ProtocolId_); // 6671907719

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
