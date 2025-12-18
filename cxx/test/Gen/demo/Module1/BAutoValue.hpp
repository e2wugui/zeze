#pragma once

#include "zeze/cxx/Bean.h"

namespace demo {
namespace Module1 {
// bean的注释
class BAutoValue : public Zeze::Bean {
public:
    static const int64_t TYPEID = 6242142801771358229LL;

    int64_t Current;
    std::string Name;
    int64_t LocalId;

    BAutoValue();
    BAutoValue(int64_t Current_, const std::string& Name_, int64_t LocalId_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BAutoValue& other);
    BAutoValue& operator=(const BAutoValue& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
