# Digital Channels Domain

| Property | Value |
|----------|-------|
| **Domain** | Digital Channels |
| **Team** | Digital Technology |
| **Domain Lead** | @digital-lead |
| **Last Updated** | 2026-03-08 |
| **Slack Channel** | #digital-channels-domain |

---

## Overview

The Digital Channels domain provides customer-facing interfaces for accessing Techcombank services. This includes mobile banking app, internet banking website, chatbot, and notification services that enable customers to manage accounts, initiate payments, and access support.

### Key Responsibilities

- **Mobile Banking App** — iOS and Android native applications
- **Internet Banking** — Web-based banking platform
- **Push Notifications** — Real-time customer notifications
- **Chatbot/AI Assistant** — Customer service automation
- **Digital Onboarding** — New customer account opening flows

## Architecture

This domain integrates with:

- **Core Banking Domain** — Account data, balance, transactions
- **Payments Domain** — Payment initiation and confirmation
- **Data Platform Domain** — User analytics, engagement metrics
- **Risk Management Domain** — Fraud detection, security rules

### Related Folders

- [`./domain-model.md`](./domain-model.md) — Business capability map
- [`./shared/`](./shared/) — Shared digital assets

## Current Projects

| Project | Status | Lead | Updated |
|---------|--------|------|---------|
| Mobile App Redesign | Approved | @design-lead | 2026-02-28 |
| Chatbot AI Platform | Draft | @product-manager | 2026-02-15 |

See [`dab/2026/`](./dab/2026/) for active projects.

## Technology Stack

**Current Technologies:**
- **Mobile**: Flutter (cross-platform), native iOS/Android
- **Web**: React/TypeScript, Next.js
- **Backend APIs**: Spring Boot 3, REST
- **Messaging**: Firebase Cloud Messaging, Twilio (SMS)
- **Analytics**: Amplitude, DataDog

## Key Contacts

- **Domain Lead**: @digital-lead — Digital strategy and roadmap
- **Mobile Lead**: @mobile-lead — Mobile app development
- **Web Lead**: @web-lead — Web platform development
- **Product Manager**: @digital-pm — Feature prioritization

## Confluence Links

- [Digital Channel Guidelines](https://confluence.techcombank.io/digital-channels)
- [Mobile App Documentation](https://confluence.techcombank.io/mobile-app)
- [API Documentation](https://confluence.techcombank.io/api-docs)

## Related Domains

- [Core Banking Domain](../core-banking/README.md)
- [Payments Domain](../payments/README.md)
- [Risk Management Domain](../risk-management/README.md)

---

Last Updated: March 8, 2026 | Team: Digital Technology
