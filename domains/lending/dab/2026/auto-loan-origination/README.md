# Auto Loan Origination

| Property | Value |
|----------|-------|
| **Project Name** | Automated Loan Origination with Straight-Through Processing |
| **Status** | Draft |
| **Submitted** | 2026-02-20 |
| **Product Manager** | @lending-pm |
| **Domain** | Lending |

---

## Project Summary

Implementation of automated loan origination system enabling straight-through processing (STP) for auto loans. This will enable 100% digital application-to-disbursement workflow without manual intervention for pre-approved customers.

### Goals

1. Enable 100% digital loan origination for auto loans
2. Achieve 95%+ straight-through processing rate
3. Reduce loan approval time from 3 days to < 4 hours
4. Reduce cost per loan by 40%
5. Improve customer satisfaction (NPS +25 points)

### Key Features

- **Digital Application** — Complete application via mobile/web
- **Auto-KYC** — Real-time KYC verification
- **Credit Decisioning** — Instant credit decision via automated rules
- **Document Collection** — Digital document submission and validation
- **Instant Offer** — Real-time offer generation
- **eSignature** — Digital signing of loan documents
- **Auto-Disbursement** — Automatic fund transfer to customer account

---

## Status

**Current Status**: Draft (Business case and requirements gathering)

This project is in early planning phases. Expected submission for formal review in May 2026.

---

## Scope

### In Scope
- Auto loan origination (STP capable)
- Instant credit decisioning
- Digital KYC integration
- Document management
- eSignature workflows

### Out of Scope
- Other loan products (will be phased in later)
- Fraud detection (separate Risk Management domain)
- Collections workflow (separate initiative)

---

## Technology Approach (Preliminary)

- **Loan Origination System**: Fiserv CoreLend modernization
- **Credit Decisioning**: Rule engine + ML scoring
- **Document Management**: AWS S3 + OCR
- **eSignature**: DocuSign integration
- **Integration**: API-first architecture

---

## Timeline (Preliminary)

| Phase | Duration | Target |
|-------|----------|--------|
| **Design & Requirements** | 6 weeks | May 2026 |
| **Development** | 10 weeks | July 2026 |
| **Testing & UAT** | 4 weeks | August 2026 |
| **Pilot** | 2 weeks | September 2026 |
| **Production** | 1 week | October 2026 |

---

## Success Criteria

| Metric | Target |
|--------|--------|
| Straight-Through Processing Rate | 95%+ |
| Approval Time | < 4 hours |
| Digital Application Rate | 90%+ |
| Customer Satisfaction (NPS) | 70+ |
| Loan Processing Cost | 60% reduction |

---

## Key Challenges

1. **Integration Complexity** — Integrating with legacy Fiserv system
2. **Regulatory Approval** — Ensuring SBV compliance for auto decisions
3. **Credit Risk** — Balancing convenience with credit quality
4. **Operational Readiness** — Training team for new workflows

---

## Next Steps

1. Complete requirements gathering (March-April 2026)
2. Design system architecture (April-May 2026)
3. Conduct POC on core features (May 2026)
4. Submit for formal approval (May 2026)
5. Begin development (June 2026, if approved)

---

Last Updated: February 20, 2026 | Status: Draft
