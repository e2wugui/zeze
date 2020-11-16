"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const Zeze = require("./Zeze");
var bb = new Zeze.Serialize.ByteBuffer();
bb.WriteBytes(new ArrayBuffer(0));
console.log(bb.toString());
