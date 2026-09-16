# LogStream — Distributed Log Analytics & Alerting Platform

LogStream is a distributed log analytics and alerting platform designed to ingest, index, search, analyze, and monitor application logs in real time.

It uses Java and gRPC for high-throughput log ingestion, Apache Lucene for embedded indexing and search, scheduled alert evaluation, and a React dashboard for analytics and visualization.

---

## Project Overview

LogStream is designed to demonstrate a complete observability pipeline:

```text
Log Producers
      |
      | gRPC
      v
Java Ingestion Server
      |
      v
Bounded Log Queue
      |
      v
Lucene Indexing
      |
      v
Search & Analytics API
      |
      | REST
      v
React Dashboard
      |
      +---- Search
      +---- Analytics
      +---- Alerts
      +---- Service Monitoring