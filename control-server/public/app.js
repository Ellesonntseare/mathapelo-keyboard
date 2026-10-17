"use strict";
const $=id=>document.getElementById(id);
const BASE={
 Rose:{background:"#1e1822",bar:"#302636",key:"#504056",special:"#3b3041",accent:"#f5a7c6",text:"#fff8fc",onAccent:"#311d2b"},
 Midnight:{background:"#111822",bar:"#202c3a",key:"#314155",special:"#243345",accent:"#8fc8ff",text:"#f4f9ff",onAccent:"#11243a"},
 Lavender:{background:"#21192c",bar:"#382a46",key:"#5a476c",special:"#453451",accent:"#d7b0ff",text:"#fff9ff",onAccent:"#352041"},
 Pearl:{background:"#f7f0f4",bar:"#efe2ea",key:"#ffffff",special:"#e9dbe5",accent:"#aa4778",text:"#2c2230",onAccent:"#ffffff"}
};
let draft=null,history=[],theme="Rose";
const request=async(url,method="GET",body)=>{
 const opt={method,credentials:"same-origin",headers:{"Accept":"application/json"}};
 if(body!==undefined){opt.headers["Content-Type"]="application/json";opt.body=JSON.stringify(body);}
 const res=await fetch(url,opt);const data=await res.json();
 if(!res.ok)throw Error(data.error||"Request failed");
 return data;
};
function status(msg,bad=false){$("status").textContent=msg;$("status").className=bad?"error":""}
function rows(){
 const list=["QWERTYUIOP","ASDFGHJKL","⇧ZXCVBNM⌫"];
 if($("numberRow").checked)list.unshift("1234567890");
 const target=$("kbRows");target.replaceChildren();
 for(const r of [...list,["?123","☺",",","SPACE",".","↵"]]){
   const el=document.createElement("div");el.className="kbrow";
   for(const ch of r){let item=document.createElement("span");item.textContent=ch==="SPACE"?"space":ch;
     if(ch==="SPACE")item.className="space";
     if(ch==="↵")item.className="action";
     el.appendChild(item);
   }target.appendChild(el);
 }
}
function preview(){
 const c={...BASE[theme]};
 if($("overrideAccent").checked)c.accent=$("accent").value;
 const keyboard=$("keyboard");
 const css={background:"--kb-bg",bar:"--kb-bar",key:"--kb-key",special:"--kb-special",accent:"--kb-accent",text:"--kb-text",onAccent:"--kb-on"};
 for(const [key,v] of Object.entries(css))keyboard.style.setProperty(v,c[key]);
 keyboard.style.setProperty("--kb-height",Math.round(34+(Number($("height").value)-38)*.5)+"px");
 keyboard.style.setProperty("--kb-radius",$("radius").value+"px");
 $("kbSuggestions").hidden=!$("predictions").checked;
 $("heightValue").textContent=$("height").value;
 $("radiusValue").textContent=$("radius").value;
 [...document.querySelectorAll(".theme")].forEach(b=>b.classList.toggle("selected",b.dataset.theme===theme));
}
function toDraft(){
 const c={
  theme,keyHeight:Number($("height").value),keyRadius:Number($("radius").value),
  animations:$("animations").checked,predictions:$("predictions").checked,
  autocorrect:$("autocorrect").checked,numberRow:$("numberRow").checked,
  toolbar:[...document.querySelectorAll("#toolbar input:checked")].map(x=>x.value),
  dictionary:[...new Set($("dictionary").value.split(/[,;\n]/).map(x=>x.trim()).filter(Boolean))],
  colors:$("overrideAccent").checked?{accent:$("accent").value}:{}
 };
 return c;
}
function hydrate(c){
 draft=c;theme=c.theme;
 $("height").value=c.keyHeight;$("radius").value=c.keyRadius;
 for(const x of ["animations","predictions","autocorrect","numberRow"])$(x).checked=c[x];
 $("dictionary").value=c.dictionary.join(", ");
 [...document.querySelectorAll("#toolbar input")].forEach(x=>x.checked=c.toolbar.includes(x.value));
 $("overrideAccent").checked=Boolean(c.colors.accent);
 $("accent").value=c.colors.accent||BASE[theme].accent;
 preview();
}
async function refresh(){
 const s=await request("/api/admin/state");
 $("revision").textContent="v"+s.published.revision;
 $("publishedAt").textContent=new Date(s.published.at).toLocaleString();
 const devices=$("devices");devices.replaceChildren();
 if(!s.devices.length) devices.textContent="No paired devices yet";
 for(const device of s.devices){
   const row=document.createElement("div");row.className="item";
   const title=document.createElement("span");
   title.textContent=device.name+" · paired "+new Date(device.created).toLocaleDateString();
   row.append(title);
   if(device.id){
     const revoke=document.createElement("button");revoke.textContent="Revoke";
     revoke.addEventListener("click",async()=>{
       if(!confirm("Revoke this phone's access to new configurations?"))return;
       try{await request("/api/admin/unpair","POST",{id:device.id});await refresh();status("Phone access revoked");}
       catch(e){status(e.message,true)}
     });
     row.append(revoke);
   }
   devices.append(row);
 }
 history=s.history;
 const list=$("history");list.replaceChildren();
 for(const item of history){
  const row=document.createElement("div");row.className="item";
  const name=document.createElement("span");name.textContent="Revision v"+item.revision;
  const btn=document.createElement("button");btn.textContent="Restore as new version";
  btn.addEventListener("click",async()=>{
   if(!confirm("Publish this older version to paired phones?"))return;
   try{await request("/api/admin/rollback","POST",{revision:item.revision});await refresh();status("Rollback published successfully");}catch(e){status(e.message,true)}
  });
  row.append(name,btn);list.append(row);
 }
 hydrate(s.draft);
}
function showStudio(){
 $("login").hidden=true;$("studio").hidden=false;$("logout").hidden=false;
}
$("loginForm").addEventListener("submit",async e=>{
 e.preventDefault();try{
  await request("/api/admin/login","POST",{password:$("password").value});
  $("password").value="";showStudio();await refresh();
 }catch(error){$("loginError").textContent=error.message}
});
$("logout").addEventListener("click",async()=>{
 await request("/api/admin/logout","POST");$("studio").hidden=true;$("login").hidden=false;$("logout").hidden=true;
});
$("save").addEventListener("click",async()=>{
 try{await request("/api/admin/draft","PUT",{config:toDraft()});status("Draft saved. Not published yet.");}catch(e){status(e.message,true)}
});
$("publish").addEventListener("click",async()=>{
 try{await request("/api/admin/draft","PUT",{config:toDraft()});await request("/api/admin/publish","POST",{});await refresh();status("Published. Phones will fetch this revision when keyboard is active.");}catch(e){status(e.message,true)}
});
$("pair").addEventListener("click",async()=>{
 try{
  const r=await request("/api/admin/pair","POST",{name:$("deviceName").value});
  $("token").value=r.token;$("tokenArea").hidden=false;status("New pairing token created. Save it securely."); 
 }catch(e){status(e.message,true)}
});
$("copyToken").addEventListener("click",async()=>{
 try{await navigator.clipboard.writeText($("token").value);status("Token copied");}
 catch{status("Select the token and copy manually",true)}
});
[...document.querySelectorAll("input,textarea")].forEach(el=>el.addEventListener("input",preview));
[...document.querySelectorAll(".theme")].forEach(el=>el.addEventListener("click",()=>{
 theme=el.dataset.theme;$("accent").value=BASE[theme].accent;preview();
}));
rows();
request("/api/admin/state").then(()=>{showStudio();refresh()}).catch(()=>{});
