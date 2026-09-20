# Worklog

All timestamps below are shown in **UTC+04:00**. Commit entries are based on
the repository's committer timestamps after timezone conversion. Planning
started on 2026-09-19 before the implementation commits made overnight.

* 2026-09-19T12:30:00+04:00 (approx.) — Started requirement analysis and
  implementation planning for the deterministic in-memory ledger.
* 2026-09-19 (later) — Identified the temporal model, append-only journal
  constraints, rejected acceptance criteria, and required documentation.
* 2026-09-20T00:01:32+04:00 — Initialized the repository with the Spring Boot
  Java 21 project skeleton.
* 2026-09-20T00:37:47+04:00 — Added money, currency, account, authorization,
  and simulation-day identifiers.
* 2026-09-20T00:57:04+04:00 — Added the immutable ledger event hierarchy.
  The commit was authored at 00:40:22, but committed at 00:57:04.
* 2026-09-20T01:05:48+04:00 — Added append-only event and ledger journals.
* 2026-09-20T01:13:47+04:00 — Corrected identifier formatting in the domain
  model.
* 2026-09-20T13:23:02+04:00 — Added the processing error model.
* 2026-09-20T13:27:31+04:00 — Implemented deterministic replay, historical
  projections, authorization, settlement, reversal, fee reconciliation,
  installments, and interest capitalization.
* 2026-09-20T13:37:57+04:00 — Added the requirement fixture and console
  reporting.
* 2026-09-20T13:43:11+04:00 — Added end-to-end coverage and the intentional
  installment-failure profile.
* 2026-09-20T13:46:39+04:00 — Rechecked README, NUMBERS, AMBIGUITIES,
  REJECTED, and WORKLOG against the requirement and implementation; expanded
  report semantics, policy constants, ambiguity decisions, and criterion
  classifications.
* 2026-09-20T16:54:35+04:00 — Added pipe-delimited text event-stream support
  through `--file`, text fixtures from simple to complex, parser coverage, and
  command-line documentation.
* 2026-09-20T17:14:07+04:00 — Changed custom text fixtures and parsing from
  pipe-delimited records to the requirement-style human-readable event lines.
* 2026-09-20T18:01:03+04:00 — Added the production architecture note covering
  append-only scaling, value-date controls, authorization lifecycle gaps, and
  deliberate scope simplifications.
* 2026-09-20 — Corrected review findings: rejected postings can no longer be
  reversed, settlement captures are capped by the authorization hold,
  authorization identifiers cannot overwrite existing state, rejected
  authorizations are excluded from active state, event amounts are validated,
  reversal currency is inferred from its target, daily reports expose
  day-specific interest, and in-memory journals are synchronized.
* 2026-09-20T18:44:00+04:00 — Corrected the second review: fee assessment now
  runs after each complete booking day, rejected authorizations remain visible,
  console output prints entries and errors, word-based installment counts are
  parsed, known account currencies are enforced, reversals inherit the original
  value date, ingestion errors are reported by day, journal ports are wired into
  replay, ledger-entry invariants are enforced, and projection variants share
  the canonical projection service.
* 2026-09-20T19:10:00+04:00 — Resolved the final PR blockers: aligned fee
  assertions with the documented historical assessment policy, removed
  destructive journal clearing, preserved prior journal records across replay,
  and ensured booked fees contribute to final balances and interest while
  uncapitalized interest remains excluded.
* 2026-09-20T21:21:00+04:00 — Corrected `AMBIGUITIES.md` to distinguish fresh
  replay calculation state from intentionally persistent append-only journal
  records across replay invocations.
