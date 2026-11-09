/* All low-frequency support entries show the developer Douyin code + profile link, without posting messages. */
const test = require('node:test');
const assert = require('node:assert/strict');
const { openPage } = require('./page.js');

function supportButton(page, scope){
  return [...page.document.querySelectorAll(scope+' button')].find(b=>b.textContent.trim()==='开发者支持');
}
function assertSupport(page){
  const box=page.document.querySelector('#modalbox');
  assert.equal(box.querySelector('h3').textContent,'开发者支持');
  assert.match(box.textContent,/开发者：某厂大牛马 · 带娃的小陈工/);
  assert.match(box.textContent,/72690415302/);
  assert.match(box.textContent,/8- 长按复制此条消息/);
  assert.match(box.textContent,/复制口令/);
  const link=box.querySelector('a[href="https://v.douyin.com/MMTUX7QHyO4/"]');
  assert.ok(link,'弹层里应有抖音主页直达链接');
  assert.equal(box.querySelectorAll('img').length,0,'不再展示二维码图片');
}

for(const entry of ['侧栏','关于','反馈']){
 test(entry+'开发者入口展示抖音口令与主页链接，不打开网址或发送内容',async()=>{
  const opened=[],sent=[];
  const page=await openPage({api:{
   about:()=>({name:'Ai.Clean',version:'1.0.0',build:'1108v1',author:'Ai.Clean',project_url:'https://example.com',license:'MIT'}),
   diagnostics:()=> '本地诊断',
   open_url:url=>opened.push(url),
   send_report:payload=>sent.push(payload),
  }});
  try{
   if(entry==='关于')await page.window.openAbout();
   if(entry==='反馈')await page.window.openFeedback();
   const scope=entry==='侧栏'?'.side-foot':'#modalbox';
   const button=supportButton(page,scope);
   assert.ok(button);
   button.click();
   assertSupport(page);
   assert.deepEqual(opened,[]);
   assert.deepEqual(sent,[]);
  }finally{page.close();}
 });
}
