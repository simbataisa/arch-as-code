# T24 Modernization

| Property | Value |
|----------|-------|
| **Project Name** | T24 to Transact R23 Migration |
| **Status** | In Review |
| **Submitted** | 2026-01-20 |
| **SA Lead** | @architect-lead |
| **Project Manager** | @core-banking-pm |
| **Domain** | Core Banking |

---

## Executive Summary

Strategic migration from legacy Temenos T24 to modern Transact R23 platform. This modernization will enable cloud-native deployment, improve API capabilities, and support Techcombank's digital transformation initiatives.

### Objectives

1. Move from monolithic to cloud-native architecture
2. Replace legacy SOAP/COM interfaces with RESTful APIs
3. Improve system scalability and performance
4. Reduce operational maintenance overhead
5. Enable faster feature development and time-to-market

### Expected Benefits

- **Scalability**: Support 2x transaction volume with same infrastructure
- **API Availability**: 99.99% uptime (vs. 99.95% legacy)
- **Development Speed**: 40% faster feature deployment
- **Cost Reduction**: 20% reduction in operational expenses (cloud vs. on-prem)
- **User Experience**: Modern UI with faster response times

---

## Project Scope

### In Scope

- Complete migration of GL and account data
- RESTful API layer replacement for SOAP/COM
- Cloud deployment (AWS EKS)
- Integration with downstream systems (Payments, Lending, Digital)
- Comprehensive testing and parallel run

### Out of Scope

- Customer-facing applications (Digital Channels domain)
- Product definition system (separate modernization)
- Core business logic rewrites (functional parity only)

---

## Timeline

| Phase | Duration | Target |
|-------|----------|--------|
| **Phase 1**: Assessment & Planning | 4 weeks | Q1 2026 |
| **Phase 2**: Data Migration | 8 weeks | Q2 2026 |
| **Phase 3**: Integration & Testing | 6 weeks | Q2 2026 |
| **Phase 4**: Pilot & Cutover | 4 weeks | Q3 2026 |
| **Total Duration** | 22 weeks | September 2026 |

---

## Risk Assessment

### High Risks

| Risk | Mitigation |
|------|-----------|
| Data integrity during migration | Comprehensive data validation; multiple test cycles |
| Integration failures with downstream systems | Early API design; mock testing with all domains |
| Performance issues in production | Load testing; performance tuning in Phase 3 |

### Mitigation Strategy

- Parallel run (old + new systems) for 2 weeks
- Automated rollback procedures
- 24/7 support team during cutover
- Detailed runbooks for common issues

---

## Success Criteria

| Metric | Current (T24) | Target (Transact) | Timeline |
|--------|---|---|---|
| API Latency (p99) | 500ms | 200ms | Q3 2026 |
| System Availability | 99.95% | 99.99% | Q3 2026 |
| GL Reconciliation Time | 4 hours | 30 minutes | Q3 2026 |
| Manual Intervention Rate | 2% | < 0.5% | Q3 2026 |

---

## Next Steps

1. Complete architecture design review
2. Obtain InfoSec and compliance sign-offs
3. Finalize data migration strategy
4. Kick off Phase 1 in early April 2026

---

Last Updated: January 20, 2026 | Status: In Review
