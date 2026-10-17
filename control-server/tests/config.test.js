"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {valid,DEFAULT}=require("../config");
test("defaults are valid and unchanged",()=>assert.deepEqual(valid(DEFAULT),DEFAULT));
test("remote config cannot include sensitive fields",()=>{
  for(const bad of [{clipboard:"private"},{password:"x"},{shortcuts:[".pas"]},{uploads:true}])assert.throws(()=>valid({...DEFAULT,...bad}));
});
test("rejects invalid color and executable injection",()=>{
  assert.throws(()=>valid({...DEFAULT,colors:{accent:"url(javascript:evil)"}}));
  assert.throws(()=>valid({...DEFAULT,dictionary:["<script>"]}));
  assert.throws(()=>valid({...DEFAULT,toolbar:["clipboard","clipboard"]}));
});
test("enforces layout bounds",()=>{
  assert.throws(()=>valid({...DEFAULT,keyHeight:500}));
  assert.throws(()=>valid({...DEFAULT,keyRadius:-1}));
  assert.equal(valid({...DEFAULT,keyRadius:18}).keyRadius,18);
});
