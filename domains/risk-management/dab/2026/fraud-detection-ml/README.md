# ML-Based Real-Time Fraud Detection

| Property | Value |
|----------|-------|
| **Project Name** | ML-Based Real-Time Fraud Detection System |
| **Status** | In Review |
| **Submitted** | 2026-01-25 |
| **SA Lead** | @architect-lead |
| **Data Science Lead** | @data-science-lead |
| **Domain** | Risk Management |

---

## Executive Summary

Implementation of machine learning-based fraud detection system to replace legacy rule-based approach. This will improve fraud detection rate from 87% to 95%+ while reducing false positives from 6% to 2%.

### Objectives

1. Increase fraud detection rate to 95%+
2. Reduce false positive rate to < 2%
3. Achieve < 100ms detection latency (p99)
4. Enable automated decision-making for 95%+ of transactions
5. Provide explainability for fraud decisions

### Expected Benefits

- **Fraud Prevention**: $5M+ annual fraud reduction
- **Operational Efficiency**: 60% reduction in manual review workload
- **Customer Experience**: Reduced false declines
- **Regulatory Compliance**: Enhanced SBV compliance position

---

## Project Scope

### In Scope

- ML model development and training
- Real-time model serving infrastructure
- Feature engineering and data pipeline
- Model monitoring and retraining
- Integration with Payments domain
- Explainability and interpretability

### Out of Scope

- AML/KYC improvements (separate project)
- Customer blacklist management (separate domain)
- Manual investigation workflow (separate system)

---

## Technical Approach

### Models

**Phase 1**: Gradient Boosting (XGBoost)
- Tabular data, fast training/inference
- Baseline model: 89% precision, 93% recall

**Phase 2**: Deep Learning (Neural Networks)
- Capture complex patterns
- Target: 95% precision, 97% recall

**Phase 3**: Ensemble
- Combine multiple models
- Risk-based weighted voting

### Infrastructure

- **Feature Store**: Feast (real-time feature computation)
- **Model Training**: Python (scikit-learn, XGBoost, TensorFlow)
- **Model Serving**: KServe (model inference service)
- **Monitoring**: Evidently AI (model drift detection)

---

## Timeline

| Phase | Duration | Target |
|-------|----------|--------|
| **Data Preparation** | 6 weeks | March 2026 |
| **Model Development** | 8 weeks | April 2026 |
| **Validation & Testing** | 4 weeks | May 2026 |
| **Pilot & Tuning** | 4 weeks | June 2026 |
| **Production Launch** | 2 weeks | July 2026 |

---

## Success Criteria

| Metric | Target | Baseline |
|--------|--------|----------|
| Detection Rate | 95%+ | 87% |
| False Positive Rate | < 2% | 6% |
| Latency (p99) | < 100ms | 250ms |
| Model Accuracy | 96%+ | 91% |
| Explainability Score | > 90% | N/A |

---

## Key Challenges

1. **Data Quality** — Ensuring clean training data; handling label noise
2. **Model Explainability** — Fraud models must be interpretable for regulators
3. **Real-Time Performance** — < 100ms latency requirement at scale
4. **Model Drift** — Fraud patterns change; monitoring needed

---

## Risk Mitigation

- Comprehensive model validation and backtesting
- Parallel run with rule-based system (2 weeks)
- Quick rollback procedures
- Manual review for high-impact decisions
- Continuous monitoring for model performance

---

## Next Steps

1. Complete data exploration and feature engineering (March 2026)
2. Train and validate baseline models (April 2026)
3. Pilot system in non-critical environment (May-June 2026)
4. Production rollout (July 2026)

---

Last Updated: January 25, 2026 | Status: In Review
