#pragma once

#include "zeze/cxx/Bean.h"

namespace demo {
namespace M6 {
class B67 : public Zeze::Bean {
public:
    static const int64_t TYPEID = 1541371653552011776LL;

    std::string Name;

    B67();
    B67(const std::string& Name_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const B67& other);
    B67& operator=(const B67& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
