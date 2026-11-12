import { createServer } from 'vite'
import { createSSRApp } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { createRouter, createMemoryHistory } from 'vue-router'
const mkStore=()=>{const m=new Map();return{getItem:k=>m.has(k)?m.get(k):null,setItem:(k,v)=>m.set(k,String(v)),removeItem:k=>m.delete(k),clear:()=>m.clear(),key:i=>[...m.keys()][i]??null,get length(){return m.size}}}
globalThis.localStorage=mkStore();globalThis.sessionStorage=mkStore()
globalThis.matchMedia=()=>({matches:false,addEventListener(){},removeEventListener(){},addListener(){},removeListener(){}})
const server=await createServer({server:{middlewareMode:true},appType:'custom',logLevel:'error'})
const mod=await server.ssrLoadModule('/src/views/LoginView.vue')
const stub={template:'<div />'}
const router=createRouter({history:createMemoryHistory(),routes:[{path:'/login',component:mod.default},{path:'/portal',component:stub},{path:'/register',component:stub},{path:'/forgot',component:stub},{path:'/forum',component:stub}]})
router.push('/login'); await router.isReady()
const app=createSSRApp(mod.default); app.use(router)
const html=await renderToString(app)
let i=html.indexOf('<button')
while(i>=0){ const seg=html.slice(i, i+400); if(/<button/.test(seg.slice(7))){ console.log('NEST AT',i); console.log(seg.slice(0,300).replace(/\n/g,' ')); break } i=html.indexOf('<button', i+7) }
await server.close()
