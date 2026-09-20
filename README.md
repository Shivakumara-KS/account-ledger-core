# Account Ledger Core

This is a Java 21/Spring Boot command-line application containing a deterministic,
append-only in-memory ledger. It has no web layer, database, persistence
framework, or UI.

## Running

Prerequisites:

* Java 21
* Maven, or the included Maven wrapper

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
.\mvnw.cmd -Pintentional-failure test
```

The first command runs the normal green suite. The second replays E1-E10 and
prints one report for each simulation day. The final command is deliberately
red: it demonstrates that three literal BHD 3.334 installments would create
BHD 10.002 and violate conservation.

The normal suite is expected to pass. The intentional-failure profile is
expected to fail by design and must not be used as the normal build gate.

## Model

Events are processed in supplied booking order. `bookingDay` identifies when
an event becomes known; `valueDate` identifies which historical balances it
affects. E7 is therefore received on Day 5 but changes the Day 2 projection.
Daily reports use the end-of-that-day booking perspective.

Authorization holds affect available balance only. Auth-A is approved, then
settled with a single AED 185 settlement debit; its unused AED 15 is released
by removing the hold, not by creating a ledger entry. Unknown Auth-Z is
rejected without a debit, and Auth-B is rejected because the account is already
negative.

Overdraft fees are assessed once per account and value date using the pre-fee
balance. E7 creates assessments for Days 2, 4, and 5. E9 creates an immutable
compensating posting, after which fee reversals restore net balances while
preserving the original fee history. Interest is calculated from the final
reconstructed balances and capitalized once on Day 6.

The expected final balances for the supplied fixture are AED 466.03 for
ACC-001 and BHD 10.008 for ACC-002. The console fields have these meanings:
`balances` are closing ledger balances at that day's booking perspective,
`fees` are fee assessments booked that day, `feeReversals` are compensating
fee postings booked that day, `authorizations` are the authorization
projection, and `errors` are expected business rejections for that day.

Packages are split into `domain`, `application`, and `infrastructure`.
`ReplayResult` exposes received and accepted events, rejected ingestion
attempts, immutable ledger entries, processing errors, fee assessments and
reversals, daily reports, interest, and final balances.
