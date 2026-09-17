# 🇹🇿 National Cyber Security & Digital Forensics System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Tanzania](https://img.shields.io/badge/🇹🇿-Tanzania-green.svg)](https://github.com/JMK-YERE/national-cyber-forensics-system)

> Mfumo wa Kitaifa wa **Kuripoti Matukio ya Usalama wa Mtandao** na **Uchambuzi wa Ushahidi wa Kidijitali** kwa Tanzania.

---

## 📖 Maelezo ya Mfumo

Mfumo huu umetengenezwa maalum kwa **Tanzania** ili kuwezesha:

- 🔐 **Kuripoti matukio ya usalama wa mtandao** (Data Breach, Phishing, Ransomware, n.k.)
- 🔬 **Kuchambua ushahidi wa kidijitali** (Digital Evidence Analysis)
- 📋 **Kufuatilia chain of custody** ya kila ushahidi
- 📊 **Kutoa ripoti** kwa **TCRA** (Tanzania Communications Regulatory Authority)
- 🚔 **Kushirikiana na Polisi Cybercrime Unit**
- 💰 **Kufuatilia hasara kifedha** kwa Shilingi (TZS)

Mfumo unafaa kwa:
- 🏦 Benki na Taasisi za Fedha
- 🏛️ Serikali na Taasisi za Umma
- 🏥 Hospitali na Sekta ya Afya
- 📡 Makampuni ya Mawasiliano
- 🎓 Vyuo Vikuu na Taasisi za Elimu
- 🏢 Makampuni Binafsi

---

## ✨ Features

### 🔒 Usalama (Security)
- ✅ **Spring Security** + BCrypt password hashing
- ✅ **Role-Based Access Control (RBAC)** — ADMIN, ANALYST, INVESTIGATOR, REPORTER
- ✅ **Account Lockout** baada ya majaribio 5 ya kuingia vibaya
- ✅ **Audit Log** — kila kitendo kinarekodiwa (nani, lini, wapi, kwa nini)
- ✅ **CSRF Protection** (Spring Security built-in)
- ✅ **Session Management** na timeout ya dakika 30
- ✅ **Secure File Upload** na validation ya aina ya file
- ✅ **AES-256 Encryption** ya evidence files

### 🔬 Digital Forensics
- ✅ **Evidence Upload** — picha, video, audio, documents, logs, memory dumps
- ✅ **SHA-256 Hash Verification** — integrity ya evidence inathibitishwa
- ✅ **MD5 Hash** kwa backward compatibility
- ✅ **Chain of Custody** — kila hatua ya evidence inarekodiwa
- ✅ **Evidence Verification** na digital signature
- ✅ **Source Device Tracking** (Laptop, Server, Mobile)
- ✅ **Acquisition Method** documentation
- ✅ **Encrypted Storage** ya evidence zote

### 📊 Incident Management
- ✅ **Unique Incident ID** (mfano: `SEC-20250101-123`)
- ✅ **Severity Levels** (LOW, MEDIUM, HIGH, CRITICAL)
- ✅ **CVSS Score** support (0.0 - 10.0)
- ✅ **Category Tagging** (Malware, Phishing, Data Breach, Ransomware, DDoS)
- ✅ **MITRE ATT&CK Mapping** (Tactics & Techniques)
- ✅ **Status Tracking** (Under Investigation, Critical, Resolved, Closed)
- ✅ **Search by Incident ID**
- ✅ **Region Tagging** (Mikoa yote ya Tanzania)

### 🇹🇿 Tanzania-Specific
- ✅ **TCRA Reporting Format** — export inayokubalika
- ✅ **Police Cybercrime Case Number** integration
- ✅ **TZS Currency** for financial loss tracking
- ✅ **Region Selection** — Dar es Salaam, Arusha, Mwanza, Dodoma, Mbeya, Morogoro, Tanga, Zanzibar
- ✅ **Organization Tagging** — Bank, Hospital, Government, n.k.

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 3.2.5, Spring Security, Spring Data JPA |
| **Frontend** | Thymeleaf, HTML5, CSS3 (Custom Cyber Theme) |
| **Database** | PostgreSQL 15 |
| **Build Tool** | Maven |
| **Java Version** | 17 |
| **Encryption** | AES-256 |
| **Hashing** | SHA-256, MD5, BCrypt |
| **File Detection** | Apache Tika |
| **IDE** | IntelliJ IDEA / VS Code |
| **Deployment** | Railway / Render / Heroku |

---

## 📦 Installation (Local Development)

### 1. Prerequisites
Hakikisha unayo:
- ✅ **Java 17+** (https://adoptium.net)
- ✅ **PostgreSQL 15+** (https://www.postgresql.org/download/)
- ✅ **PgAdmin4** (inakuja na PostgreSQL)
- ✅ **Maven 3.8+** (https://maven.apache.org)
- ✅ **Git** (https://git-scm.com)
- ✅ **IntelliJ IDEA** au **VS Code**

### 2. Clone Repository
```bash
git clone https://github.com/JMK-YERE/national-cyber-forensics-system.git
cd national-cyber-forensics-system
