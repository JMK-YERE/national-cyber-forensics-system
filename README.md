# 🇹🇿 National Cyber Security & Digital Forensics System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Tanzania](https://img.shields.io/badge/🇹🇿-Tanzania-green.svg)]()

Mfumo wa kitaifa wa **kuripoti matukio ya usalama wa mtandao** na **uchambuzi wa ushahidi wa kidijitali** kwa Tanzania.

---

## 🎯 Features

### 🔒 Security
- ✅ Spring Security + BCrypt
- ✅ Role-Based Access Control (ADMIN, ANALYST, INVESTIGATOR, REPORTER)
- ✅ Account Lockout (majaribio 5)
- ✅ Audit Log (kila kitendo kinarekodiwa)
- ✅ CSRF Protection
- ✅ Secure File Upload

### 🔬 Digital Forensics
- ✅ Evidence Upload (picha, video, docs, logs)
- ✅ AES-256 Encryption ya evidence
- ✅ SHA-256 Hash Integrity Verification
- ✅ Chain of Custody Tracking
- ✅ IOC (Indicators of Compromise) Tracking
- ✅ CVSS Severity Scoring
- ✅ MITRE ATT&CK Mapping

### 🇹🇿 Tanzania-Specific
- ✅ TCRA Reporting Format
- ✅ Police Cybercrime Case Number
- ✅ TZS Financial Loss Tracking
- ✅ Region Tagging (Mikoa yote 31)

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2.5, Spring Security, Spring Data JPA |
| Frontend | Thymeleaf, HTML5, CSS3 (Custom Cyber Theme) |
| Database | PostgreSQL 15 |
| Build | Maven |
| Java | 17 |
| IDE | IntelliJ IDEA |

---

## 📦 Installation

### 1. Prerequisites
- Java 17+
- PostgreSQL 15+
- IntelliJ IDEA
- Maven 3.8+
- Git

### 2. Clone Repository
```bash
git clone https://github.com/YOUR_USERNAME/national-cyber-forensics-system.git
cd national-cyber-forensics-system
```

### 3. Setup PostgreSQL (PgAdmin4)
1. Fungua PgAdmin4
2. Unda database: `incident_db`
3. Query Tool → Run `database.sql`

### 4. Configure `application.properties`
```properties
spring.datasource.password=YOUR_PASSWORD
```

### 5. Run
- IntelliJ IDEA → Run `ForensicsApplication.java`
- Browser: http://localhost:8080

### 6. Login
- **Username:** `admin`
- **Password:** `Admin@123`

---

## 📞 Support

Kwa msaada, fungua **Issue** kwenye GitHub.

---

**🇹🇿 Kwa Usalama wa Taifa Letu 🇹🇿**
