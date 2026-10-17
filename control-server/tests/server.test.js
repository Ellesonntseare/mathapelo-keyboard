"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const {srv}=require("../server");
let base;
test.before(async()=>{
 await new Promise(resolve=>srv.listen(0,"127.0.0.1",resolve));
 base="http://127.0.0.1:"+srv.address().port;
});
test.after(async()=>{await new Promise(resolve=>srv.close(resolve));});
test("no device config without a device token",async()=>{
 const r=await fetch(base+"/api/device/config");
 assert.equal(r.status,401);
 const body=await r.json();assert.match(body.error,/token/i);
});
test("admin configuration requires authentication",async()=>{
 for(const uri of ["/api/admin/state","/api/admin/publish"]){
  const r=await fetch(base+uri,{method:uri.endsWith("publish")?"POST":"GET"});
  assert.equal(r.status,401);
 }
});
test("no clipboard or typing upload endpoint",async()=>{
 const r=await fetch(base+"/api/device/upload",{method:"POST",headers:{"Content-Type":"application/json"},body:'{"keystrokes":"private"}'});
 assert.equal(r.status,404);
});
test("static dashboard uses secure headers",async()=>{
 const r=await fetch(base+"/");assert.equal(r.status,200);
 assert.match(r.headers.get("content-security-policy"),/script-src/);
 assert.equal(r.headers.get("x-frame-options"),"DENY");
});
