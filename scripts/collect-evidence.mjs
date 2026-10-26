import { readdir, readFile, mkdir, writeFile } from "node:fs/promises";

async function suites(directory) {
  const files=(await readdir(directory)).filter(name=>name.startsWith("TEST-")&&name.endsWith(".xml"));
  return Promise.all(files.sort().map(async file=>{
    const xml=await readFile(`${directory}/${file}`,"utf8");
    const tag=xml.match(/<testsuite\b[^>]*>/)?.[0]||"";
    const attr=name=>tag.match(new RegExp(`\\b${name}="([^"]*)"`))?.[1];
    return {suite:attr("name"),tests:Number(attr("tests")),failures:Number(attr("failures")),errors:Number(attr("errors")),skipped:Number(attr("skipped")),seconds:Number(attr("time")),javaVersion:xml.match(/<property name="java.version" value="([^"]*)"/)?.[1]};
  }));
}
const unitAndH2=await suites("target/surefire-reports");
const postgres=await suites("target/failsafe-reports");
const browser=JSON.parse(await readFile("frontend/test-results/results.json","utf8"));
const processing=JSON.parse(await readFile("target/benchmarks/processing.json","utf8"));
const concurrency={reservation:await readFile("target/benchmarks/reservation.txt","utf8"),fulfillment:await readFile("target/benchmarks/fulfillment.txt","utf8")};
const summary={collectedAt:new Date().toISOString(),upstreamRevision:"fcd3ebc9ae4e0b6a082af1fe4a19990357eb82b1",platform:process.platform,architecture:process.arch,unitAndH2,postgres,browserStats:browser.stats,processing,concurrency};
await mkdir("docs/evidence",{recursive:true});await writeFile("docs/evidence/local-verification.json",JSON.stringify(summary,null,2)+"\n");
console.log(`Evidence collected: ${unitAndH2.reduce((n,s)=>n+s.tests,0)} unit/H2 + ${postgres.reduce((n,s)=>n+s.tests,0)} PostgreSQL executions; ${browser.stats.expected} browser passes, ${browser.stats.unexpected} unexpected failures.`);
