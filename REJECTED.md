# Rejected criteria and approaches

The following supplied acceptance criteria are intentionally refused:

* **“The Day 2 closing ledger balance ... is AED -370.00.”** This criterion is
  accepted only with its stated end-of-Day-5 and pre-fee perspective. It is not
  the Day 2 report produced at the end of Day 2, where E7 is not known.
* **“E7 causes exactly one overdraft fee.”** E7 changes historical closing
  balances on Days 2, 4, and 5, so the once-per-day rule requires three
  assessments.
* **“After E9, all balances and fees return to pre-E7 values.”** Economic
  balances return through three compensating fee reversals, but append-only
  history cannot erase the original fee records. The literal “fees return”
  wording is therefore rejected while the net balance is restored.
* **“Each E10 installment is BHD 3.334.”** That creates BHD 10.002, not the
  requested BHD 10.000. The deterministic conserving allocation is
  3.334/3.333/3.333.
* **“Discard an interest remainder.”** Discarding violates exact conservation;
  rounded daily accruals are summed exactly into capitalization.

* **Auth-B's conditional “if approved” statement.** This is not applicable to
  the supplied stream rather than invalid as a business rule: Auth-B's
  available balance is negative, so the event is rejected. If an authorization
  were approved in another stream, its hold would reduce available balance but
  not ledger balance.

Approaches abandoned during implementation included mutating/deleting fees on
reversal, treating holds as ledger entries, sorting events by value date, and
posting daily interest entries, using a single mutable current balance,
floating-point money, and allocating three literal BHD 3.334 installments.
Each would lose auditability, violate conservation, or contradict the
specified temporal model.
