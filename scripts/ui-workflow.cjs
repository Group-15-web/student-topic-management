// Mutating E2E: run ONLY against a freshly seeded, isolated test database.
const { chromium } = require('playwright');
const fs = require('node:fs');
const base = process.env.E2E_BASE_URL || 'http://127.0.0.1:8081';
if (!base.endsWith(':8081')) throw new Error('This script is restricted to isolated test port 8081.');
async function main(){
  fs.mkdirSync('target/screenshots',{recursive:true});
  const browser=await chromium.launch({headless:true,channel:process.env.BROWSER_CHANNEL||'msedge'});
  const contexts=[];const checks=[];
  async function login(name){const context=await browser.newContext({viewport:{width:1440,height:1000}});contexts.push(context);const page=await context.newPage();page.on('dialog',d=>d.accept());await page.goto(base+'/login');await page.locator('[name=username]').fill(name+'@demo.local');await page.locator('[name=password]').fill('Demo@2026!');await Promise.all([page.waitForURL(base+'/'),page.locator('button[type=submit]').click()]);return page;}
  async function submit(page,selector,url){await Promise.all([page.waitForURL(base+url,{waitUntil:'networkidle'}),page.locator(selector).click()]);}
  try{
    const student=await login('sv4');await student.goto(base+'/registrations/1');
    await student.locator('[name=file]').setInputFiles({name:'bao-cao-demo.pdf',mimeType:'application/pdf',buffer:Buffer.from('%PDF-1.4\n1 0 obj <</Type /Catalog>> endobj\n%%EOF')});
    await Promise.all([student.waitForResponse(r=>r.url().endsWith('/submissions')&&r.request().method()==='POST'),student.getByRole('button',{name:'Tải báo cáo lên'}).click()]);
    await student.waitForLoadState('networkidle');
    const download=await student.request.get(base+'/submissions/1/download');if(download.status()!==200)throw new Error('Download failed '+download.status());checks.push('Leader upload + authorized download');
    const scores=[8,9,7,10];
    for(let i=0;i<4;i++){const page=await login('gv'+(i+2));await page.goto(base+'/evaluations/'+(i+1));await page.locator('[name=score]').fill(String(scores[i]));await page.locator('[name=comment]').fill('Sản phẩm đạt yêu cầu; nội dung và kết quả có minh chứng.');await submit(page,'button[name=submit][value=true]','/evaluations');checks.push('Lecturer '+(i+2)+' submitted score '+scores[i]);}
    const president=await login('gv3');await president.goto(base+'/registrations/1');await Promise.all([president.waitForResponse(r=>r.url().endsWith('/confirm')&&r.request().method()==='POST'),president.getByRole('button',{name:'Xác nhận tổng hợp'}).click()]);await president.waitForLoadState('networkidle');checks.push('President confirmation');
    const faculty=await login('khoa');await faculty.goto(base+'/results');await faculty.locator('[name=registrationIds][value="1"]').check();await submit(faculty,'form[action="/results/publish"] button','/results');
    await faculty.locator('.score-circle b').filter({hasText:'8.50'}).waitFor();checks.push('Batch publication produces 8.50');
    await faculty.screenshot({path:'target/screenshots/e2e-published.png',fullPage:true});
    let previews=0;for(let i=0;i<15;i++){await faculty.goto(base+'/mail');previews=await faculty.getByText('PREVIEW',{exact:true}).count();if(previews===3)break;await new Promise(r=>setTimeout(r,1000));}if(previews!==3)throw new Error('Expected 3 email previews, found '+previews);checks.push('Three asynchronous per-student emails');
    await faculty.screenshot({path:'target/screenshots/e2e-mail.png',fullPage:true});
    await student.goto(base+'/results');await student.locator('.score-circle b').filter({hasText:'8.50'}).waitFor();
    const other=await login('sv1');await other.goto(base+'/results');if(await other.locator('.score-circle').count())throw new Error('Cross-group score leaked');const blocked=await other.goto(base+'/registrations/1');if(blocked.status()!==403)throw new Error('Cross-group request not denied');checks.push('Own result visible; cross-group data denied');
    console.log(JSON.stringify({passed:true,checks}));
  }finally{for(const context of contexts)await context.close();await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1});
