# Numbers and policy constants

| Constant | Value | Why |
|---|---:|---|
| Simulation window | 6 days | The supplied event stream and requirement define Day 1 through Day 6. |
| ACC-001 opening balance | AED 0.00 | Explicit requirement; represented by no synthetic posting. |
| ACC-002 opening balance | BHD 0.000 | Explicit requirement; represented by no synthetic posting. |
| AED precision | 2 | Currency precision in the requirement; not half (1) because cents must be preserved. |
| BHD precision | 3 | Currency precision in the requirement; not half (1/2) because fils must be preserved. |
| Rounding | HALF_UP | Requirement leaves mode open; this conventional monetary mode is deterministic. |
| Overdraft fee | AED 25.00 | Explicit requirement; not half because that would violate the specified fee. |
| Daily interest | 0.04% = 0.0004 | Explicit requirement; retained as decimal, never floating point. |
| Installment count | 3 | Explicit E10 event. |
| Interest posting | one Day 6 capitalization | Explicit requirement that accruals capitalize as a single credit. |
| Interest accrual rounding | currency scale after each daily multiplication | Required so daily accruals are monetary records and their exact sum can be capitalized. |
| Fee assessment key | account plus value date | Required by “once per day per account”; a later booking event may discover an earlier value date. |
| Day identifiers | Day 1 through Day 6 | The event stream has a fixed six-day simulation window; no additional days are inferred. |
| Installment remainder unit | one currency minor unit | The remainder is allocated deterministically to the first installment so the total remains conserved. |
