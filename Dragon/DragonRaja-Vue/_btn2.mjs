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
// 去掉 HTML 注释后再判断嵌套
const clean = html.replace(/<!--[\s\S]*?-->/g, '')
const bad = /<button[^>]*>(?:(?!<\/button>)[\s\S])*?<button/.test(clean)
console.log('去注释后 button 嵌套:', bad)
console.log('button 总数:', (clean.match(/<button/g)||[]).length)
console.log('去掉注释后是否还有 192.168.68.1:', clean.includes('192.168.68.1'))
console.log('去掉注释后 auth-card__head:', clean.includes('auth-card__head'))
await server.close()
