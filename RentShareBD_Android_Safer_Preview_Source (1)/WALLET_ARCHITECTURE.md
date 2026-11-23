# RentShare BD Wallet / Payments — Proposed Architecture, Not Activated

**Requested design:** 1 RentCoin represents ৳1 BDT; users top up through bKash/Nagad and use their balance to pay rent, utility bills, and household expenses.

## Legal and operating prerequisite

A stored-value, spendable or transferable balance can be an e-wallet/prepaid payment instrument. Bangladesh's Payment and Settlement Systems Act, 2024, and Bangladesh Bank's payment licensing framework can apply. A rental-platform owner should not accept user money into a personal mobile-money account and promise redeemable app coins without a properly authorized payment structure. Confirm the precise arrangement with an authorized payment service provider and competent local counsel.

Relevant references:
- Bangladesh legal text: https://bdlaws.minlaw.gov.bd/act-print-1496.html
- Bangladesh Bank payment systems: https://www.bb.org.bd/en/index.php/financialactivity/paysystems
- bKash merchant services: https://www.bkash.com/en/business

## Recommended initial alternative (lower risk)

For a public MVP, **do not issue stored-value coins**. Offer payment instructions/requests, receipt uploads for record keeping (explicitly unverified), and when a merchant account is approved, direct hosted checkout via a licensed payment provider. If a landlord or utility provider is the true payee, confirm payouts and permitted settlement structures rather than sending all rental money to the platform owner's personal account.

## Later regulated-wallet integration (illustrative only)

- Each user has a wallet account backed by a regulated PSP/MFS partner; RentShare BD acts as the UI and authorized partner, not an unlicensed custodian.
- Amounts are stored in **integer paisa** (`100 paisa = ৳1`), never floating point.
- Ledger entries are **immutable** and double-entry, with transaction ID, status, created timestamp, settled timestamp, owner, counterparty, currency, provider and correlation/reference IDs.
- Every top-up starts as `PENDING` and is committed only after backend calls the provider's **authenticated verification API** and validates amount, merchant, currency, status and idempotency.
- Duplicate callbacks/webhooks must never double-credit a wallet; authorization is server-side, not Android-side.
- Spending a balance is one atomic backend transaction: check available balance, reserve funds, debit from sender and credit authorized recipient or settlement liability, then record provider payout.
- Failed transfers, disputes, refunds, fraud holds, and reversed settlements must have auditable compensating entries.
- Users see clear balance, payment status, transaction details, fees, refund policy, dispute support, and privacy/identity requirements.
- Required provider settlement bank/MFS account, ownership, eligibility, cash-out policies and fees are documented and approved before activation.

## Screens implemented in this source revision

**Wallet Preview** is intentionally read-only. It explains the proposed 1 RentCoin = ৳1 mapping, shows **no spendable balance**, and disables bKash/Nagad recharge controls. It includes a navigation link to the existing utility bill tracker. This is not an actual wallet or gateway integration. Your payment phone number and provider credentials are not embedded in code.
