#pragma once

#include "zeze/cxx/Bean.h"

namespace demo {
namespace Module1 {
/*
		bean的
		多行注释
*/
class BRemoved2 : public Zeze::Bean {
public:
    static const int64_t TYPEID = -3773667973386893417LL;

    int Int_1; // com aa

    BRemoved2();
    BRemoved2(int Int_1_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BRemoved2& other);
    BRemoved2& operator=(const BRemoved2& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
