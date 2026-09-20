# Ambiguities and resolutions

1. **Rounding mode was unspecified.** We use `HALF_UP` at each currency posting
   boundary; intermediate division/multiplication retains extra precision.
2. **Booking versus value time was easy to conflate.** Source order is
   authoritative. E7 is booked Day 5 but has value date Day 2; E9 is booked Day
   6 and reverses that historical value.
3. **What happens to the unused Auth-A hold?** We chose full hold release when
   settlement is accepted. The AED 15 difference is not a ledger entry.
4. **How can an unknown settlement be represented?** It remains in received
   history and produces an error, but no debit or authorization is created.
5. **Does a fee affect the balance used to decide that fee?** No. Fee
   reconciliation uses the pre-fee historical balance, avoiding circularity.
6. **What happens after a fee-triggering debit is reversed?** A compensating
   `FEE_REVERSAL` is appended; the original assessment remains auditable.
7. **Can a fee be assessed again after it was reversed?** No: once-per-account/day
   is an assessment invariant, even after a compensating reversal.
8. **Interest timing.** Interest uses the final reconstructed projection after
   E9 and fee reversals, then posts once at Day 6.
9. **Installment remainder.** The smallest-unit remainder goes to installment
   one, producing 3.334/3.333/3.333 and conserving exactly BHD 10.000.
10. **Duplicate history.** Received events are kept separately from accepted
    events, so a rejected duplicate is never mistaken for a financial posting.
11. **Authorization expiry.** No expiry state is modeled because the
    requirement gives no expiry rule; an approved hold remains active until
    settlement within the simulation.
12. **Currency mismatch.** Mixed-currency arithmetic, settlement, and reversal
    attempts are rejected with `CURRENCY_MISMATCH`; no posting is generated.
13. **Repeated settlement and reversal.** A settlement is accepted only for an
    existing approved authorization. A second settlement is rejected, and an
    original event cannot be reversed twice.
14. **Fee timing.** Fees are reconciled when the relevant booking event is
    processed, using the historical balance before fee entries. A final Day 6
    reconciliation applies required reversals after E9.
15. **Fee booking versus value date.** A fee is booked on the day the negative
    condition is discovered and carries the negative assessment day's value
    date.
16. **Replay state.** Every replay creates fresh derived calculation state, so
    replaying the same ordered event list twice produces equivalent
    `ReplayResult` values. Injected event and ledger journals are different:
    they retain append-only records across replay invocations, and duplicate
    ledger entry IDs are not appended again. Journal persistence is therefore
    intentional and auditable rather than a mutation of prior records.
17. **Opening balances.** Both opening balances are zero and are represented
    by no synthetic ledger entries.
18. **Report perspective.** A daily report includes entries booked by that day
    and value-dated on or before the report day; later backdated events appear
    only in later reports.
