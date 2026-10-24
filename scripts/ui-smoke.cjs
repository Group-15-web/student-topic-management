const { chromium } = require('playwright');
const fs = require('node:fs');
async function main() {
  fs.mkdirSync('target/screenshots', { recursive: true });
  const browser = await chromium.launch({headless:true, channel:process.env.BROWSER_CHANNEL||'msedge'});
  const page = await browser.newPage({ viewport: { width:1440, height:1000 } });
  const errors=[]; page.on('pageerror',e=>errors.push(e.message));
  await page.goto('http://127.0.0.1:8080/login');
  await page.screenshot({path:'target/screenshots/login.png',fullPage:true});
  await page.locator('[name="username"]').fill('khoa@demo.local');
  await page.locator('[name="password"]').fill('Demo@2026!');
  await Promise.all([page.waitForURL('http://127.0.0.1:8080/'),page.locator('button[type="submit"]').click()]);
  await page.screenshot({path:'target/screenshots/dashboard.png',fullPage:true});
  for(const path of ['/topics','/registrations/1','/forms/cycles','/forms/topics','/accounts']){
    const response=await page.goto('http://127.0.0.1:8080'+path);
    if(response.status()!==200)errors.push(path+': HTTP '+response.status());
    await page.screenshot({path:'target/screenshots/'+path.replaceAll('/','_')+'.png',fullPage:true});
  }
  await page.setViewportSize({width:390,height:844});
  await page.goto('http://127.0.0.1:8080/');
  await page.screenshot({path:'target/screenshots/mobile.png',fullPage:true});
  if(await page.evaluate(()=>document.documentElement.scrollWidth>window.innerWidth))errors.push('Mobile horizontal overflow');
  console.log(JSON.stringify({errors,screenshots:'target/screenshots'}));
  await browser.close();
  if(errors.length)process.exitCode=1;
}
main().catch(e=>{console.error(e);process.exitCode=1});
