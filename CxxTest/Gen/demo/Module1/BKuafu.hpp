#pragma once

#include "zeze/cxx/Bean.h"

namespace demo {
namespace Module1 {
class BKuafu : public Zeze::Bean {
public:
    static const int64_t TYPEID = -8805935299408250239LL;

    int64_t Account;
    int64_t Money;

    BKuafu();
    BKuafu(int64_t Account_, int64_t Money_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BKuafu& other);
    BKuafu& operator=(const BKuafu& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
