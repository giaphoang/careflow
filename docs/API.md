# CareFlow REST API

Base: `/api/careflow`. Authenticate using the foundation's `POST /api/auth/login` with `{"username":"requester","password":"CareFlow@123"}` and send `Authorization: Bearer <accessToken>`. The token filter reads the current user's roles from the database. Existing refresh, logout, and administrator user APIs remain available.

Exactly 12 CareFlow routes are added; inherited hospital/auth routes are additional.

| # | Method | Route | Access and effect |
| --- | --- | --- | --- |
| 1 | GET | `/inventory` | All CareFlow roles; paginated lot views |
| 2 | POST | `/inventory` | Inventory manager/admin; create synthetic lot |
| 3 | PATCH | `/inventory/{id}` | Inventory manager/admin; stock delta or quality hold, with reason |
| 4 | GET | `/requests` | Requester sees own records; staff/admin see all |
| 5 | POST | `/requests` | Requester/clinical/admin; validate lot and create REQUESTED |
| 6 | GET | `/requests/{id}` | Owner or staff/admin |
| 7 | POST | `/requests/{id}/review` | Clinical/admin, never the requester; approve or reject REQUESTED |
| 8 | POST | `/requests/{id}/reserve` | Inventory manager/admin; APPROVED → RESERVED atomically |
| 9 | POST | `/requests/{id}/fulfill` | Inventory manager/admin; RESERVED → FULFILLED, requires idempotency header |
| 10 | POST | `/requests/{id}/cancel` | Owner or inventory manager/admin; cancel active request, releasing stock if reserved |
| 11 | POST | `/requests/{id}/expire` | Inventory manager/admin; release a RESERVED request at/after its deadline |
| 12 | GET | `/audit` | Admin sees full ledger; `?requestId=` permits owner or staff access to that request's history |

All three list routes accept `page` (zero-based, default 0) and `size` (1–200, default 50), sorted newest ID first. Responses reuse the upstream `ApiResponse` envelope. List `data` has stable fields `content`, `page`, `size`, `totalElements`, `totalPages`, and `last`.

## Request bodies

Create inventory:

```json
{
  "name": "Synthetic cardiac pacing kit",
  "udi": "SYN-UDI-10009",
  "lot": "SIM-26-709",
  "location": "North · Store A",
  "category": "Cardiovascular",
  "expiresOn": "2030-12-31",
  "totalStock": 50,
  "reorderPoint": 10
}
```

The expiry must be in the future. UDI plus lot is unique. Quantities and reorder point cannot be negative. Adjust inventory:

```json
{"stockDelta": 10, "quarantined": false, "reason": "Synthetic stock receipt"}
```

`quarantined` is the intended final quality state; send the current value when only changing stock. An adjustment cannot remove stock that is reserved. For a hold, send `stockDelta: 0, quarantined: true`. No reserved request is automatically fulfilled or cancelled by a hold; distribution is blocked until the hold is released or the reservation is cancelled/expired.

Submit a supply request:

```json
{"itemId": 1, "quantity": 2, "department": "Cardiac services", "purpose": "Synthetic store replenishment"}
```

Review:

```json
{"approve": true, "reason": "Simulated resource need confirmed"}
```

Cancel:

```json
{"reason": "Simulated replenishment no longer required"}
```

Reserve, fulfill, and expire have no request body. Fulfillment requires `Idempotency-Key: <key>`, consisting of 1–100 letters, digits, dots, underscores, colons, or hyphens. Reuse the same key after a timeout. Keys have global scope and persist with receipts. Reusing a key on a different request returns 409; changing keys on a fulfilled request also returns 409.

Receipt fields are `id`, `requestId`, `itemId`, `quantity`, `fulfilledBy`, `fulfilledAt`, and `idempotencyKey`. Repeated same-key responses have identical receipt data; they do not create additional audit or notification records.

## Errors and rules

| HTTP | Meaning |
| --- | --- |
| 200 | Successful operation or replayed fulfillment receipt; follows upstream response conventions |
| 400 | Invalid fields, malformed JSON, missing idempotency header, invalid pagination |
| 401 | Missing/invalid/revoked authentication |
| 403 | Role restriction, ownership restriction, or self review |
| 404 | Missing request or inventory item |
| 409 | Invalid state, unavailable stock, expired reservation/lot, quality hold, duplicate lot/key, or concurrent integrity conflict |

Fulfillment and reservation use UTC for reservation deadlines and date-based lot expiry. At the exact reservation deadline, fulfillment is blocked and expiry is allowed. Approval never allocates stock. Terminal states have no outgoing transitions. Inherited DOCTOR/RECEPTIONIST roles do not implicitly grant CareFlow access.

Interactive OpenAPI documentation, including record schemas, is generated from controllers at `/swagger-ui/index.html`; JSON is at `/v3/api-docs`.
