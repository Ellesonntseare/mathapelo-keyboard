"use strict";
const THEMES=["Rose","Midnight","Lavender","Pearl"];
const PALETTE=["background","bar","key","pressed","special","accent","accentDown","text","muted","onAccent"];
const TOOLS=["clipboard","shortcuts","oneHanded","settings"];
const DEFAULT={theme:"Rose",keyHeight:46,keyRadius:12,animations:true,predictions:true,autocorrect:true,numberRow:true,toolbar:[...TOOLS],dictionary:[],colors:{}};
function valid(o){
  if(!o||typeof o!=="object"||Array.isArray(o))throw Error("Configuration must be an object");
  const keys=Object.keys(DEFAULT);
  if(Object.keys(o).some(k=>!keys.includes(k)))throw Error("Unknown configuration field");
  const c={...DEFAULT,...o};
  if(!THEMES.includes(c.theme))throw Error("Invalid theme");
  if(!Number.isInteger(c.keyHeight)||c.keyHeight<38||c.keyHeight>64)throw Error("Key height must be 38–64");
  if(!Number.isInteger(c.keyRadius)||c.keyRadius<3||c.keyRadius>24)throw Error("Key radius must be 3–24");
  for(const f of ["animations","predictions","autocorrect","numberRow"])if(typeof c[f]!=="boolean")throw Error("Invalid "+f);
  if(!Array.isArray(c.toolbar)||c.toolbar.length>TOOLS.length||new Set(c.toolbar).size!==c.toolbar.length||c.toolbar.some(x=>!TOOLS.includes(x)))throw Error("Invalid toolbar");
  if(!Array.isArray(c.dictionary)||c.dictionary.length>500||c.dictionary.some(w=>typeof w!=="string"||!/^[\p{L}'-]{2,32}$/u.test(w)))throw Error("Dictionary must contain up to 500 ordinary words");
  if(!c.colors||typeof c.colors!=="object"||Array.isArray(c.colors)||Object.entries(c.colors).some(([k,v])=>!PALETTE.includes(k)||typeof v!=="string"||!/^#[0-9A-Fa-f]{6}$/.test(v)))throw Error("Invalid colors");
  return JSON.parse(JSON.stringify(c));
}
module.exports={valid,DEFAULT,THEMES,PALETTE,TOOLS};
