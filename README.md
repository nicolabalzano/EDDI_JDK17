# E.D.D.I

E.D.D.I (Enhanced Dialog Driven Interface) is an enterprise-certified chatbot middleware that offers configurable NLP,
Behavior Rules, and API connectivity for seamless integration with various conversational services.

Developed in Java (with Quarkus), provided with Docker, orchestrated with Kubernetes or Openshift.

Latest stable version: 5.0.4

License: Apache License 2.0

Project website: [here](https://eddi.labs.ai/)

Documentation: [here](https://docs.labs.ai/)

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/2c5d183d4bd24dbaa77427cfbf5d4074)](https://www.codacy.com/gh/labsai/EDDI/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=labsai/EDDI&amp;utm_campaign=Badge_Grade) [![CircleCI](https://circleci.com/gh/labsai/EDDI/tree/main.svg?style=svg)](https://circleci.com/gh/labsai/EDDI/tree/main)



## Intro

E.D.D.I is a highly scalable and enterprise-certified cloud-native chatbot middleware
that serves as a trusted gatekeeper for various conversational services. Designed to run smoothly in cloud environments
like Docker, Kubernetes, and Openshift, E.D.D.I offers configurable NLP and Behavior Rules that facilitate conversations
and can act as watchdog for sensitive topics. Its API integration capabilities make it easy to connect
with other conversational or classical REST APIs. Multiple bots can be easily integrated and run side by side,
including multiple versions of the same bot for a smooth upgrading transition.

Features worth mentioning:

* Easily integrate other conversational or classical REST APIs
* Configurable NLP and Behavior rules allow conversation facilitation as well as watch dog for sensitive topics
* Multiple bots could be easily integrated and run side by side even multiple versions of the same bot 

technical spec:

* Resource- / REST-oriented architecture
* Java Quarkus
* JAX-RS
* Dependency Injection
* Prometheus integration (Metrics endpoint)
* Kubernetes integration (Liveness/Readiness endpoint)
* MongoDB for storing bot configurations and conversation logs
* OAuth 2.0 (Keycloak) for authentication and user management
* HTML, CSS, Javascript (Dashboard & Basic Chat UI)

# What we have done
We perfom a vulnerabilty assessment and penestration testing over this project forked by https://github.com/labsai/EDDI/ 

# Security Assessment Summary

This repository contains the results and documentation of a vulnerability assessment and penetration testing activity performed on the E.D.D.I. project.
You can found the discovered vulnerability in the [presentation](https://github.com/nicolabalzano/EDDI_JDK17/blob/main/va_pt_EDDI_presentation.pdf) or in [report](https://github.com/nicolabalzano/EDDI_JDK17/blob/main/va_pt_EDDI_report.pdf).

## Scope
- Source code review (Java, Quarkus, JS, HTML)
- Static and dynamic analysis (Fortify, ZAP and Burpsuite)
- Manual and automated vulnerability testing

## How to Run Securely
- Set the `EDDI_GIT_AES_KEY` environment variable for encryption.
- Use Docker or Kubernetes for isolated deployment.
- Access the dashboard at [http://localhost:7070](http://localhost:7070)

For more details, see the `SECURITY.md` and `CRYPTO_MIGRATION.md` files.

---