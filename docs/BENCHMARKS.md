# Verification and benchmark record

Local implementation verification was performed on October 26, 2026, on macOS/Apple Silicon using Java 21.0.13 and PostgreSQL 16.15. Machine-readable reports are collected in [evidence/local-verification.json](evidence/local-verification.json). Raw JUnit XML/text and workload outputs remain in `target/`; browser screenshots, HTML, and JSON are generated under `frontend/`. The initial processing experiment used Java 22 compiled for release 21; the final Java acceptance and browser checks use Java 21.

## Verification scope

| Check | Local result |
| --- | --- |
| Standard Java suite | 120 passing executions: 90 new unit, 24 new H2 integration, 6 inherited tests |
| PostgreSQL acceptance suite | 27 passing executions: the same 24 integration scenarios, 2 stress tests, and a legacy-schema upgrade regression |
| Browser suite | 6 scenarios: dashboard, inventory search/export/holds, requester submission/history, full cross-role fulfillment, administrator audit/access, mobile overflow |
| REST routes | 12 CareFlow method/path combinations, in addition to inherited APIs |
| Frontend | Production build verified; inherited dependency advisories addressed with updated Vite/lockfile |
| Docker/CI | Compose configuration validated; Java 21 Dockerfiles and GitHub Actions supplied. Docker image builds, Testcontainers launch, and hosted Actions were not run locally because Docker was stopped; PostgreSQL tests used an installed isolated instance |

The shared integration scenarios are executed against two database engines; they are not counted as distinct business requirements. Parameterized unit executions include the complete 49-pair state-transition matrix. Browser tests use real JWT login and the running PostgreSQL backend, with synthetic data.

## 1,000-operation consistency workloads

`WorkflowPostgresIT` creates the fixtures before timing. Both workloads use a 32-thread executor and the application's bounded connection pool; there are 1,000 operations, not 1,000 simultaneous database connections. Every future must complete successfully or with the explicitly expected stock conflict. An unexpected exception fails the test.

| Workload | Expected and observed invariants |
| --- | --- |
| 1,000 approved requests compete for one available unit | Exactly 1 reservation succeeds; 999 fail for insufficient stock; total 1, reserved 1, available 0 |
| 1,000 concurrent retries of one fulfillment/key | Exactly 1 receipt, 1 fulfilled transition, 0 duplicate fulfillments; total 0, reserved 0 |

Final Java 21 timings were 159 ms for the reservation phase and 345 ms for fulfillment retries, retained in the machine-readable evidence. These measurements exclude fixture creation, startup, HTTP, and authentication; they demonstrate consistency under the defined workload, not production capacity.

## Processing-time simulation

Run a demo backend, then from the repository root:

```sh
BENCHMARK_SAMPLES=12 MANUAL_STEP_MS=100 node scripts/processing-benchmark.mjs
```

The script runs two batches of synthetic requests through the same four authorized transitions. Authentication and lot creation happen outside the timed region. The manual baseline performs repeated inventory/request lookups and adds 100 ms of modeled operator overhead before each of four handoffs. The automated variant sends the four writes directly with identical role requirements. Warmup occurs before measurement. Each fulfillment uses a unique retry key.

| Metric | Observed initial local run |
| --- | --- |
| Samples per variant | 12 |
| Modeled manual overhead per request | 400 ms plus repeated lookups |
| Baseline mean | 491.29 ms |
| Automated mean | 19.07 ms |
| Baseline p95 | 535.32 ms |
| Automated p95 | 27.71 ms |
| Mean reduction | 96.12% |

The result exceeds the 60% target **within this explicit synthetic model**. The modeled delay is an input, not a measured human task time. These numbers do not establish a 60% real-world reduction in hospital processing effort, staff time, or patient outcomes. A real-world claim would need a measured task protocol and user study. Change `MANUAL_STEP_MS=0` to inspect the effect of redundant lookups alone. Requests/lots created by this script remain in its configured demonstration database.

## Reproduce the evidence

```sh
mvn -Ppostgres-tests verify
cd frontend
npm ci
npm run build
npx playwright install chromium
npm run test:e2e
cd ..
node scripts/processing-benchmark.mjs
node scripts/collect-evidence.mjs
```

Use the isolated PostgreSQL settings from the README if Docker is unavailable. Start the demo backend before the browser suite and processing benchmark. The collector requires all three report sets and does not convert missing or skipped work into a pass. CI retains its reports for 30 days; publishing the workflow does not assert a hosted run has already passed.
