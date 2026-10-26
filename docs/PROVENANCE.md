# Source provenance

CareFlow extends `https://github.com/anshuBuilds/Hospital-Mangement-System`, cloned at commit `fcd3ebc9ae4e0b6a082af1fe4a19990357eb82b1`. The repository's Git history and source remote are retained, with the original source configured as `upstream` and `https://github.com/giaphoang/careflow` configured as `origin`. The upstream README is preserved as `UPSTREAM_README.md`.

The hospital domain modules, JWT/security implementation, notification outbox, authentication context, original dashboard, and Docker foundation originate with that repository. CareFlow's supply-domain classes, database migration, workflow UI, tests, benchmarks, CI workflow, and documentation are additions. Integration changes upgrade Java to 21, extend roles, use UTC, gate demo seeding, add bootstrap administration, map workflow errors, and route the default UI to CareFlow. Vite and its lockfile were updated to address inherited dependency advisories.

`https://github.com/divyansh224/hospital-management-system` was cloned into temporary reference storage at commit `c36f1b6795bf74d4744f6d75875dcfffb5480986`. Its `AppointmentServiceImpl` and `AppointmentServiceImplTest` were inspected as supporting references for domain rules in service methods and focused Mockito verification. No application files were copied from it. Its separate MySQL persistence model was not substituted for the selected PostgreSQL base.

No explicit license file was present in the cloned base at the pinned revision. This implementation preserves attribution and does not invent a license grant. Review upstream licensing/permission before redistributing source or presenting inherited modules as original work.

All UDI-like strings, lots, facilities, accounts, and device inventory records supplied by CareFlow are synthetic. Product category names illustrate medical technology operations; they are not manufacturer device specifications. CareFlow is not affiliated with or endorsed by Medtronic and makes no regulatory certification or medical-device quality-system compliance claim.
