# ===== Server =====
server.port=${PORT:8080}
server.error.include-message=always
server.forward-headers-strategy=framework

# ===== PostgreSQL (Environment Variables kwa Cloud) =====
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/incident_db}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver

# ===== JPA =====
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.open-in-view=false

# ===== Thymeleaf =====
spring.thymeleaf.cache=true

# ===== File Upload =====
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=100MB

# ===== Custom App =====
app.upload.dir=${UPLOAD_DIR:uploads/evidence}
app.encryption.key=${ENCRYPTION_KEY:TanzaniaCyberSecurityKey2024!!!}

# ===== Session =====
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=false

# ===== Logging =====
logging.level.root=INFO
logging.level.org.springframework.security=INFO
logging.level.com.tz.forensics=INFO
