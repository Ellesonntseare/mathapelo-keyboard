"use strict";
// Dependency-free, PC-local config server. Does not receive keystrokes, screenshots or clipboards.
const http=require("node:http"),fs=require("node:fs"),path=require("node:path"),crypto=require("node:crypto");
const {valid,DEFAULT}=require("./config");
const PORT=Number(process.env.MATHAPELO_PORT||8787);
const HOST="127.0.0.1"; // use Tailscale Serve for private, TLS-authenticated remote phone access.
const DATA=path.join(__dirname,"data"),STORE=path.join(DATA,"state.json");
fs.mkdirSync(DATA,{recursive:true});
const random=()=>crypto.randomBytes(32).toString("hex");
const digest=x=>crypto.createHash("sha256").update(x).digest("hex");
const hashPassword=(value,salt)=>crypto.scryptSync(value,salt,32).toString("hex");
let state;
if(fs.existsSync(STORE)){state=JSON.parse(fs.readFileSync(STORE,"utf8"));}
else {
  const password=random().slice(0,24),salt=random();
  state={adminSalt:salt,adminHash:hashPassword(password,salt),devices:[],published:{revision:1,config:valid(DEFAULT),at:new Date().toISOString()},draft:valid(DEFAULT),history:[]};
  fs.writeFileSync(STORE,JSON.stringify(state,null,2),{mode:0o600});
  console.log("\n====================================\nFIRST-RUN ADMIN PASSWORD (save it):\n"+password+"\n====================================\n");
}
function save(){
  const tmp=STORE+".tmp";
  fs.writeFileSync(tmp,JSON.stringify(state,null,2),{mode:0o600});
  fs.renameSync(tmp,STORE);
}
function device(name){
  const token=random();
  state.devices.push({id:random().slice(0,16),name:name.slice(0,60)||"Android phone",hash:digest(token),created:new Date().toISOString()});
  save();
  return token;
}
if(process.argv.includes("--pair")){
  const pos=process.argv.indexOf("--pair");
  console.log("PHONE PAIRING TOKEN (keep private):\n"+device(process.argv[pos+1]||"Mathapelo phone"));
  process.exit(0);
}
if(process.argv.includes("--reset-admin")){
  const password=random().slice(0,24);
  state.adminSalt=random();
  state.adminHash=hashPassword(password,state.adminSalt);
  save();
  console.log("NEW ADMIN PASSWORD:\n"+password);
  process.exit(0);
}
const sessions=new Map();
function send(res,status,data,headers={}){
  const body=JSON.stringify(data);
  res.writeHead(status,{"Content-Type":"application/json; charset=utf-8","Cache-Control":"no-store","X-Content-Type-Options":"nosniff",...headers});
  res.end(body);
}
function receive(req){
  return new Promise((resolve,reject)=>{
    let body="";
    req.on("data",x=>{body+=x;if(body.length>65536){reject(Error("Body too large"));req.destroy();}});
    req.on("end",()=>{try{resolve(body?JSON.parse(body):{});}catch{reject(Error("Invalid JSON"));}});
    req.on("error",reject);
  });
}
function sessionFor(req){
  const cookie=(req.headers.cookie||"").match(/(?:^|;\s*)mathapelo_sid=([0-9a-f]{64})/);
  if(!cookie)return false;
  const s=sessions.get(cookie[1]);
  return Boolean(s&&s> Date.now());
}
function checkOrigin(req){
  // Browser forms must come from the dashboard origin. Machine clients use Bearer auth, not cookies.
  const origin=req.headers.origin;
  if(!origin)return true;
  try{
    const u=new URL(origin);
    const host=req.headers.host||"";
    return u.host===host && (u.protocol==="http:" && /^127\.0\.0\.1(:|$)|^localhost(:|$)/.test(host) || u.protocol==="https:");
  }catch{return false;}
}
function authenticatedDevice(req){
  const h=req.headers.authorization||"";
  if(!h.startsWith("Bearer "))return false;
  const hash=digest(h.slice(7));
  return state.devices.some(d=>crypto.timingSafeEqual(Buffer.from(d.hash,"hex"),Buffer.from(hash,"hex")));
}
const html=fs.readFileSync(path.join(__dirname,"public","index.html"));
const css=fs.readFileSync(path.join(__dirname,"public","style.css"));
const js=fs.readFileSync(path.join(__dirname,"public","app.js"));
const srv=http.createServer(async(req,res)=>{
  try{
    const url=new URL(req.url,"http://localhost");
    if(req.method==="GET" && ["/","/style.css","/app.js"].includes(url.pathname)){
      const file=url.pathname==="/" ? html :url.pathname==="/style.css"?css:js;
      res.writeHead(200,{"Content-Type":url.pathname==="/"?"text/html; charset=utf-8":url.pathname.endsWith("css")?"text/css; charset=utf-8":"application/javascript; charset=utf-8","Cache-Control":"no-store","X-Content-Type-Options":"nosniff","X-Frame-Options":"DENY","Content-Security-Policy":"default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; base-uri 'none'; form-action 'self'"});
      return res.end(file);
    }
    if(url.pathname==="/api/device/config"&&req.method==="GET"){
      if(!authenticatedDevice(req))return send(res,401,{error:"Device token invalid"});
      return send(res,200,{schema:1,...state.published});
    }
    if(url.pathname==="/api/admin/login"&&req.method==="POST"){
      if(!checkOrigin(req))return send(res,403,{error:"Origin rejected"});
      const body=await receive(req),input=typeof body.password==="string"?body.password:"";
      const ok=input.length<=200&&crypto.timingSafeEqual(Buffer.from(hashPassword(input,state.adminSalt),"hex"),Buffer.from(state.adminHash,"hex"));
      if(!ok)return send(res,401,{error:"Incorrect password"});
      const sid=random();sessions.set(sid,Date.now()+8*3600000);
      return send(res,200,{ok:true},{"Set-Cookie":"mathapelo_sid="+sid+"; HttpOnly; SameSite=Strict; Path=/; Max-Age=28800"+(req.headers["x-forwarded-proto"]==="https"?"; Secure":"")});
    }
    if(!url.pathname.startsWith("/api/admin/"))return send(res,404,{error:"Not found"});
    if(!sessionFor(req))return send(res,401,{error:"Admin login required"});
    if(req.method==="POST"&&!checkOrigin(req))return send(res,403,{error:"Origin rejected"});
    if(url.pathname==="/api/admin/state"&&req.method==="GET")
      return send(res,200,{published:state.published,draft:state.draft,history:state.history.map(x=>({revision:x.revision,at:x.at})),devices:state.devices.map(x=>({id:x.id,name:x.name,created:x.created}))});
    if(url.pathname==="/api/admin/draft"&&req.method==="PUT"){
      if(!checkOrigin(req))return send(res,403,{error:"Origin rejected"});
      state.draft=valid((await receive(req)).config);save();return send(res,200,{ok:true});
    }
    if(url.pathname==="/api/admin/publish"&&req.method==="POST"){
      const c=valid(state.draft);
      state.history.unshift(state.published);state.history=state.history.slice(0,20);
      state.published={revision:state.published.revision+1,config:c,at:new Date().toISOString()};
      save();return send(res,200,{published:state.published});
    }
    if(url.pathname==="/api/admin/rollback"&&req.method==="POST"){
      const r=Number((await receive(req)).revision);
      const found=state.history.find(x=>x.revision===r);
      if(!found)return send(res,404,{error:"Revision not found"});
      state.history.unshift(state.published);state.history=state.history.slice(0,20);
      state.draft=valid(found.config);
      state.published={revision:state.published.revision+1,at:new Date().toISOString(),config:state.draft};
      save();return send(res,200,{published:state.published});
    }
    if(url.pathname==="/api/admin/pair"&&req.method==="POST"){
      const body=await receive(req);
      return send(res,200,{token:device(typeof body.name==="string"?body.name:"Android phone")});
    }
    if(url.pathname==="/api/admin/unpair"&&req.method==="POST"){
      const body=await receive(req);
      if(typeof body.id!=="string" || body.id.length!==16)return send(res,400,{error:"Invalid device"});
      const old=state.devices.length;
      state.devices=state.devices.filter(d=>d.id!==body.id);
      if(state.devices.length===old)return send(res,404,{error:"Device not found"});
      save();return send(res,200,{ok:true});
    }
    if(url.pathname==="/api/admin/logout"&&req.method==="POST"){
      const cookie=(req.headers.cookie||"").match(/mathapelo_sid=([0-9a-f]{64})/);
      if(cookie)sessions.delete(cookie[1]);
      return send(res,200,{ok:true},{"Set-Cookie":"mathapelo_sid=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0"});
    }
    return send(res,404,{error:"Not found"});
  }catch(e){console.error(e.message);send(res,400,{error:e.message||"Invalid request"});}
});
if(require.main===module)srv.listen(PORT,HOST,()=>console.log("Mathapelo Control Panel: http://"+HOST+":"+PORT+"\nRemote phone access: configure Tailscale Serve HTTPS; server never exposes a public port."));
module.exports={srv};
