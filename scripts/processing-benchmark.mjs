import { mkdir, writeFile } from "node:fs/promises";
import { performance } from "node:perf_hooks";
import { randomUUID } from "node:crypto";

const base=process.env.CF_API_URL || "http://localhost:8080";
const samples=Number(process.env.BENCHMARK_SAMPLES || 12);
const manualStepMs=Number(process.env.MANUAL_STEP_MS || 100);
if (!Number.isInteger(samples)||samples<1||samples>1000||!Number.isFinite(manualStepMs)||manualStepMs<0||manualStepMs>5000) throw new Error("Invalid benchmark parameters");
async function call(path,token,method="GET",body,key) {
  const response=await fetch(base+path,{method,headers:{"Content-Type":"application/json",...(token?{Authorization:`Bearer ${token}`} : {}),...(key?{"Idempotency-Key":key}:{})},...(body===undefined?{}:{body:JSON.stringify(body)})});
  const result=await response.json();if(!response.ok)throw new Error(`${path}: ${result.message}`);return result.data;
}
async function login(username) {return (await call("/api/auth/login",null,"POST",{username,password:process.env.BENCHMARK_PASSWORD||"CareFlow@123"})).accessToken;}
const [requester,clinical,inventory]=await Promise.all([login("requester"),login("clinical"),login("inventory")]);
async function measure(manual) {
  const lot=await call("/api/careflow/inventory",inventory,"POST",{name:"Synthetic benchmark kit",udi:`SYN-BENCH-${randomUUID()}`,lot:`BENCH-${manual?"MANUAL":"AUTO"}-${Date.now()}`,location:"Benchmark store",category:"Surgical",expiresOn:"2035-01-01",totalStock:samples,reorderPoint:0});
  const times=[];
  for(let n=0;n<samples;n++) {
    const start=performance.now();
    async function manualStep(id) {
      if(!manual)return;
      // Explicit simulated operator overhead; not a clinical or real-world time study.
      await call("/api/careflow/inventory?size=200",inventory);
      if(id)await call(`/api/careflow/requests/${id}`,inventory);
      await new Promise(resolve=>setTimeout(resolve,manualStepMs));
    }
    await manualStep();
    const r=await call("/api/careflow/requests",requester,"POST",{itemId:lot.id,quantity:1,department:"Benchmark simulation",purpose:"Synthetic processing-time experiment"});
    await manualStep(r.id);await call(`/api/careflow/requests/${r.id}/review`,clinical,"POST",{approve:true,reason:"Simulated review"});
    await manualStep(r.id);await call(`/api/careflow/requests/${r.id}/reserve`,inventory,"POST");
    await manualStep(r.id);await call(`/api/careflow/requests/${r.id}/fulfill`,inventory,"POST",undefined,randomUUID());
    times.push(performance.now()-start);
  }
  const sorted=[...times].sort((a,b)=>a-b);
  return {samples,meanMs:times.reduce((a,b)=>a+b,0)/samples,p50Ms:sorted[Math.floor(samples*.5)],p95Ms:sorted[Math.min(samples-1,Math.floor(samples*.95))],observationsMs:times};
}
// Warm up serialization, security, and the connection pool before either measurement.
await call("/api/careflow/inventory",inventory);
const baseline=await measure(true),automated=await measure(false);
const reductionPercent=(1-automated.meanMs/baseline.meanMs)*100;
const report={generatedAt:new Date().toISOString(),type:"synthetic-manual-overhead-model",warning:"Includes configured operator delays and redundant lookups in baseline. Not evidence of real hospital labor savings.",manualStepMs,manualStepsPerRequest:4,baseline,automated,reductionPercent,meetsSimulationTarget:reductionPercent>=60};
await mkdir("target/benchmarks",{recursive:true});await writeFile("target/benchmarks/processing.json",JSON.stringify(report,null,2)+"\n");
console.log(JSON.stringify(report,null,2));
