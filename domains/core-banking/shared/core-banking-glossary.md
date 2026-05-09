# Core Banking Glossary

Essential terms and concepts used throughout the Core Banking domain.

---

## Account-Related Terms

### Account
A customer's relationship with Techcombank for financial transactions. Accounts hold deposits, facilitate payments, and earn interest. Each account has a unique account number and is associated with one or more customers.

**Types**:
- **Checking Account** — For frequent transactions, debit card linked
- **Savings Account** — For funds storage, earns interest, limited transactions
- **Investment Account** — For investment products (stocks, bonds, funds)

### Account Product
A predefined template for account creation defining features, fees, interest rates, and regulatory restrictions. Example: "Techcombank Savings 2026" product with 4.5% interest rate.

### CIF (Customer Information File)
Unique identifier assigned to each customer in Temenos T24. Used internally to link accounts and transactions to customer records. Example: CIF-123456789.

---

## Financial Accounting Terms

### General Ledger (GL)
Complete, chronological record of all financial transactions expressed in standardized accounting format. GL maintains accounts such as:
- Asset accounts (deposits, loans outstanding)
- Liability accounts (customer deposits)
- Income accounts (interest income, fees)
- Expense accounts (operating costs)

### GL Posting
The process of recording a transaction in the General Ledger. Each transaction creates two GL entries:
- **Debit** — Increases asset accounts or decreases liability accounts
- **Credit** — Decreases asset accounts or increases liability accounts

**Example**: Customer deposits VND 1,000,000
```
Debit: Asset Account (Bank Cash): +1,000,000
Credit: Liability Account (Customer Deposit): +1,000,000
```

### Trial Balance
A summary of all GL account balances at a specific point in time. Used to verify that all debits equal all credits (fundamental accounting principle).

### Chart of Accounts
Comprehensive list of all GL accounts organized by type:
- Assets (10xx accounts)
- Liabilities (20xx accounts)
- Equity (30xx accounts)
- Income (40xx accounts)
- Expenses (50xx accounts)

---

## Interest and Income Terms

### Interest Accrual
Daily calculation of interest earnings on deposit accounts based on:
- Account balance
- Interest rate
- Number of days in period

**Example**: Savings account with VND 100,000,000 balance at 4.5% annual rate:
- Daily accrual = (100,000,000 × 4.5%) / 365 = VND 12,329

### Interest Posting
Monthly process of crediting accrued interest to customer's account. Typically occurs on the last day of month.

### Interest Rate
Annual percentage return on deposit accounts set by bank based on:
- Product type
- Market conditions
- Customer tier
- Promotional rates

**2026 Rates** (example):
- Savings Account: 4.5% per annum
- Money Market Account: 5.2% per annum
- Corporate Account: 3.8% per annum

### Personal Income Tax (PIT) Withholding
Personal income tax withheld from interest income. Vietnam's standard PIT rate on interest is 10% (per SBV regulations).

**Example**: Customer receives VND 1,000,000 in interest
- Gross interest: VND 1,000,000
- PIT withheld (10%): VND 100,000
- Net interest credited: VND 900,000

---

## T24/Transact Terms

### T24 (Temenos T24)
Temenos T24 is a mature, enterprise-grade core banking system. Techcombank uses T24 as the primary ledger system storing GL and account master data.

**Key Components**:
- **Account Service** — Account master and GL
- **Customer Service** — Customer KYC and CIF
- **Product Service** — Product definitions and pricing
- **Batch Interface** — Scheduled jobs for interest posting, GL reconciliation

### Transact
Temenos Transact R23 is the latest version of T24. It modernizes T24 with:
- Cloud-native architecture
- RESTful APIs (instead of SOAP/COM)
- Enhanced scalability
- Improved user experience

---

## Regulatory and Compliance Terms

### SBV (State Bank of Vietnam)
Vietnam's central bank. Sets banking regulations including:
- Required GL reconciliation frequency
- Interest rate ceilings and floors
- Capital adequacy requirements
- Reporting standards

### SBV Circular 24
SBV's circular on general regulations on banking activities in Vietnam. Key requirements:
- Minimum account documentation
- Fraud prevention
- Transaction monitoring
- Customer complaint procedures

### KYC (Know Your Customer)
Customer verification and profile validation:
- Identity verification (ID card, passport)
- Address verification
- Beneficial ownership identification (corporate accounts)
- Periodic re-verification (annual for retail, quarterly for high-risk)

### AML (Anti-Money Laundering)
Compliance program to prevent money laundering:
- Suspicious transaction reporting
- Customer risk assessment
- Transaction monitoring
- Sanctions list screening

---

## Transaction Types

### Debit
Transaction reducing account balance:
- **ATM Withdrawal** — Cash withdrawal via ATM
- **Transfer Out** — Outgoing transfer to another account
- **Check Clearance** — Check presented for payment
- **Fee Deduction** — Monthly or transaction-based fees
- **Interest Reversal** — Reverse of previously accrued interest

### Credit
Transaction increasing account balance:
- **Deposit** — Cash or check deposit
- **Transfer In** — Incoming transfer from another account
- **Interest Posting** — Monthly interest accrual
- **Fee Reversal** — Reversal of applied fees

---

## Account Status

### Active
Account in normal operating state. Customer can conduct all transactions (deposits, withdrawals, transfers).

### Frozen
Account temporarily locked. Typically due to:
- Customer request (e.g., lost card)
- Regulatory investigation
- Fraud suspicion
- Customer death (pending estate settlement)

**Operations Blocked**: Withdrawals, transfers (customer initiated)
**Operations Allowed**: Deposits, interest posting, GL postings by bank

### Suspended
Account locked pending investigation or customer action. Examples:
- Customer hasn't accessed account in 5 years (dormant)
- Regulatory hold
- Debt collection proceedings

**Operations Blocked**: All customer transactions
**Operations Allowed**: GL postings, interest, regulatory actions

### Closed
Account permanently closed. No further transactions allowed.

**Closure Reasons**:
- Customer request (funds withdrawn)
- Dormancy (no activity for 10+ years)
- Regulatory compliance
- Merger/consolidation

---

## Customer Segmentation

### Retail Banking
Individual customers with personal bank accounts. Typically:
- Annual income: < VND 5 billion
- Account balance: < VND 500 million
- Transaction limits: VND 50 million/day
- Products: Checking, Savings, Debit Card

### SME (Small-Medium Enterprise)
Small business customers with business accounts. Typically:
- Annual revenue: VND 5-100 billion
- Account balance: VND 100 million - 5 billion
- Transaction limits: VND 500 million/day
- Products: Business Checking, Business Savings, Short-term Loans

### Corporate Banking
Large enterprise customers with corporate accounts. Typically:
- Annual revenue: > VND 100 billion
- Account balance: > VND 5 billion
- Transaction limits: Customized (unlimited for established customers)
- Products: Treasury, Trade Finance, Corporate Lending

---

## See Also

- [Core Banking Domain Model](../domain-model.md)
- [Context Map](../context-map.md)
- [Payment Glossary](../../payments/shared/payment-glossary.md)

---

Last Updated: March 8, 2026 | Domain: Core Banking
