# Spring Boot – Hướng Dẫn Setup Project Website Tìm Kiếm Gia Sư

## 1. Spring Initializr – Cấu hình khởi tạo

Truy cập [https://start.spring.io](https://start.spring.io) và điền thông tin:

| Trường | Giá trị |
|---|---|
| Project | Maven |
| Language | Java 21 |
| Spring Boot | 3.3.x (latest stable) |
| Group | `com.tutornet` |
| Artifact | `tutor-net` |
| Name | `tutor-net` |
| Packaging | Jar |

### Dependencies cần thêm:

```
- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Spring Security
- Spring Validation
- Lombok
- MapStruct (thêm thủ công vào pom.xml sau)
- Spring Mail
- Spring Cache (Caffeine)
- Flyway Migration
- Spring Boot DevTools
- Docker Compose Support (tuỳ chọn)
```

---

## 2. Cấu trúc thư mục – Kiến trúc phân lớp theo Domain

Dự án dùng **Package by Feature** (theo domain/module) thay vì package by layer để dễ mở rộng và bảo trì.

```
tutor-net/
├── src/
│   ├── main/
│   │   ├── java/com/tutornet/
│   │   │   │
│   │   │   ├── TutorNetApplication.java          # Main class
│   │   │   │
│   │   │   ├── config/                            # Cấu hình toàn cục
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtConfig.java
│   │   │   │   ├── CacheConfig.java
│   │   │   │   ├── AuditConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   │
│   │   │   ├── common/                            # Shared utilities & base classes
│   │   │   │   ├── base/
│   │   │   │   │   ├── BaseEntity.java            # Abstract: id, createdAt, updatedAt
│   │   │   │   │   ├── BaseService.java           # Template Method pattern (xem Mục 4)
│   │   │   │   │   └── BaseAuditEntity.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   │   ├── AccessDeniedException.java
│   │   │   │   │   └── BusinessException.java
│   │   │   │   ├── response/
│   │   │   │   │   ├── ApiResponse.java           # Wrapper response chuẩn
│   │   │   │   │   └── PageResponse.java
│   │   │   │   ├── visitor/
│   │   │   │   │   ├── Visitable.java             # Interface Visitor pattern (xem Mục 5)
│   │   │   │   │   └── ProfileVisitor.java
│   │   │   │   ├── enums/                         # Enum tương ứng DB ENUM types
│   │   │   │   │   ├── UserStatus.java
│   │   │   │   │   ├── TutorStatus.java
│   │   │   │   │   ├── SessionStatus.java
│   │   │   │   │   ├── TeachingMode.java
│   │   │   │   │   ├── PaymentStatus.java
│   │   │   │   │   ├── ProficiencyLevel.java
│   │   │   │   │   └── EducationLevel.java
│   │   │   │   └── util/
│   │   │   │       ├── SlugUtils.java
│   │   │   │       └── DateUtils.java
│   │   │   │
│   │   │   ├── domain/                            # Các domain/module nghiệp vụ
│   │   │   │
│   │   │   │   ├── auth/                          # Đăng ký, đăng nhập, JWT
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   └── AuthController.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── AuthService.java
│   │   │   │   │   │   └── JwtService.java
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   │   ├── LoginResponse.java
│   │   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   │   └── TokenRefreshRequest.java
│   │   │   │   │   └── model/
│   │   │   │   │       ├── UserSession.java       # Entity → bảng user_sessions
│   │   │   │   │       └── VerificationToken.java # Entity → bảng verification_tokens
│   │   │   │   │
│   │   │   │   ├── user/                          # User & RBAC
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   └── UserController.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── UserService.java
│   │   │   │   │   │   └── RoleService.java
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   │   ├── RoleRepository.java
│   │   │   │   │   │   └── PermissionRepository.java
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── UserDto.java
│   │   │   │   │   │   ├── UserCreateRequest.java
│   │   │   │   │   │   └── UserUpdateRequest.java
│   │   │   │   │   └── model/
│   │   │   │   │       ├── User.java              # Entity → bảng users
│   │   │   │   │       ├── Role.java              # Entity → bảng roles
│   │   │   │   │       ├── Permission.java
│   │   │   │   │       ├── UserRole.java
│   │   │   │   │       └── RolePermission.java
│   │   │   │   │
│   │   │   │   ├── tutor/                         # Hồ sơ gia sư, duyệt, tìm kiếm
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── TutorController.java   # Public: tìm kiếm, xem profile
│   │   │   │   │   │   └── TutorAdminController.java # Duyệt/từ chối hồ sơ
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── TutorService.java
│   │   │   │   │   │   ├── TutorApprovalService.java  # Template Method (xem Mục 4)
│   │   │   │   │   │   └── TutorSearchService.java
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── TutorProfileRepository.java
│   │   │   │   │   │   ├── TutorCertificateRepository.java
│   │   │   │   │   │   └── TutorAvailabilityRepository.java
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── TutorProfileDto.java
│   │   │   │   │   │   ├── TutorSearchRequest.java
│   │   │   │   │   │   ├── TutorSearchResponse.java
│   │   │   │   │   │   └── TutorApprovalRequest.java
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── TutorProfile.java
│   │   │   │   │   │   ├── TutorCertificate.java
│   │   │   │   │   │   └── TutorAvailability.java
│   │   │   │   │   └── mapper/
│   │   │   │   │       └── TutorMapper.java       # MapStruct mapper
│   │   │   │   │
│   │   │   │   ├── student/
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   └── StudentController.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   └── StudentService.java
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── StudentProfileRepository.java
│   │   │   │   │   │   └── ParentStudentLinkRepository.java
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   └── StudentProfileDto.java
│   │   │   │   │   └── model/
│   │   │   │   │       ├── StudentProfile.java
│   │   │   │   │       ├── ParentProfile.java
│   │   │   │   │       └── ParentStudentLink.java
│   │   │   │   │
│   │   │   │   ├── subject/
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   └── SubjectController.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   └── SubjectService.java
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── SubjectRepository.java
│   │   │   │   │   │   └── TutorSubjectRepository.java
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   └── SubjectDto.java
│   │   │   │   │   └── model/
│   │   │   │   │       ├── Subject.java
│   │   │   │   │       └── TutorSubject.java
│   │   │   │   │
│   │   │   │   ├── session/                       # Đặt lịch & quản lý buổi học
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   └── SessionController.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── SessionService.java
│   │   │   │   │   │   └── SessionWorkflowService.java  # Template Method (xem Mục 4)
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── SessionRepository.java
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── SessionCreateRequest.java
│   │   │   │   │   │   └── SessionDto.java
│   │   │   │   │   └── model/
│   │   │   │   │       └── Session.java
│   │   │   │   │
│   │   │   │   ├── payment/
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   └── PaymentController.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── PaymentService.java
│   │   │   │   │   │   └── PaymentGatewayService.java   # Template Method cho VNPay/Momo
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── PaymentRepository.java
│   │   │   │   │   │   └── WalletRepository.java
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   └── PaymentDto.java
│   │   │   │   │   └── model/
│   │   │   │   │       ├── Payment.java
│   │   │   │   │       ├── TutorWallet.java
│   │   │   │   │       └── WalletTransaction.java
│   │   │   │   │
│   │   │   │   ├── review/
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   └── ReviewController.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   └── ReviewService.java
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── ReviewRepository.java
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   └── ReviewDto.java
│   │   │   │   │   └── model/
│   │   │   │   │       └── Review.java
│   │   │   │   │
│   │   │   │   └── notification/
│   │   │   │       ├── service/
│   │   │   │       │   ├── NotificationService.java
│   │   │   │       │   └── EmailService.java
│   │   │   │       ├── repository/
│   │   │   │       │   └── NotificationRepository.java
│   │   │   │       └── model/
│   │   │   │           └── Notification.java
│   │   │   │
│   │   │   └── infrastructure/                    # Tích hợp ngoài (S3, VNPay, ...)
│   │   │       ├── storage/
│   │   │       │   └── S3StorageService.java
│   │   │       └── audit/
│   │   │           └── AuditLogService.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/                      # Flyway migrations
│   │           ├── V1__init_schema.sql            # Schema từ file SQL của bạn
│   │           ├── V2__seed_roles_permissions.sql
│   │           └── V3__seed_subjects.sql
│   │
│   └── test/
│       └── java/com/tutornet/
│           ├── domain/
│           │   ├── auth/AuthServiceTest.java
│           │   ├── tutor/TutorServiceTest.java
│           │   └── session/SessionServiceTest.java
│           └── integration/
│               └── TutorSearchIntegrationTest.java
│
├── pom.xml
└── docker-compose.yml                             # PostgreSQL local dev
```

---

## 3. application.yml – Cấu hình cơ bản

```yaml
spring:
  application:
    name: tutor-net

  datasource:
    url: jdbc:postgresql://localhost:5432/tutor_net
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:secret}
    hikari:
      maximum-pool-size: 10

  jpa:
    hibernate:
      ddl-auto: validate          # Flyway quản lý schema, JPA chỉ validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration

jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 900       # 15 phút (giây)
  refresh-token-expiry: 2592000  # 30 ngày (giây)
```

---

## 4. Design Pattern: Template Method

Template Method định nghĩa bộ khung (skeleton) của một thuật toán trong class cha, cho phép class con ghi đè các bước cụ thể mà không thay đổi cấu trúc chung.

### Ứng dụng 1: Luồng duyệt hồ sơ gia sư

```java
// common/base/BaseApprovalService.java
public abstract class BaseApprovalService<T> {

    // Template method – luồng chính KHÔNG được ghi đè
    public final void approve(T entity, Long reviewerId) {
        validate(entity);           // Bước 1: kiểm tra điều kiện
        doPreApprove(entity);       // Bước 2: xử lý trước (hook)
        markAsApproved(entity, reviewerId); // Bước 3: cập nhật trạng thái
        doPostApprove(entity);      // Bước 4: xử lý sau (hook)
        sendApprovalNotification(entity);   // Bước 5: thông báo
    }

    public final void reject(T entity, String reason, Long reviewerId) {
        validate(entity);
        markAsRejected(entity, reason, reviewerId);
        sendRejectionNotification(entity, reason);
    }

    protected abstract void validate(T entity);
    protected abstract void markAsApproved(T entity, Long reviewerId);
    protected abstract void markAsRejected(T entity, String reason, Long reviewerId);
    protected abstract void sendApprovalNotification(T entity);
    protected abstract void sendRejectionNotification(T entity, String reason);

    // Hook – class con có thể override hoặc để trống
    protected void doPreApprove(T entity) {}
    protected void doPostApprove(T entity) {}
}
```

```java
// domain/tutor/service/TutorApprovalService.java
@Service
@RequiredArgsConstructor
public class TutorApprovalService extends BaseApprovalService<TutorProfile> {

    private final TutorProfileRepository tutorRepo;
    private final NotificationService notificationService;
    private final AuditLogService auditLog;

    @Override
    protected void validate(TutorProfile tutor) {
        if (tutor.getStatus() != TutorStatus.PENDING_REVIEW) {
            throw new BusinessException("Chỉ duyệt hồ sơ đang ở trạng thái pending_review");
        }
    }

    @Override
    protected void markAsApproved(TutorProfile tutor, Long reviewerId) {
        tutor.setStatus(TutorStatus.APPROVED);
        tutor.setVerifiedAt(Instant.now());
        tutor.setVerifiedBy(reviewerId);
        tutorRepo.save(tutor);
        auditLog.log("tutor_profile", tutor.getId(), "APPROVED", reviewerId);
    }

    @Override
    protected void markAsRejected(TutorProfile tutor, String reason, Long reviewerId) {
        tutor.setStatus(TutorStatus.REJECTED);
        tutorRepo.save(tutor);
        auditLog.log("tutor_profile", tutor.getId(), "REJECTED: " + reason, reviewerId);
    }

    @Override
    protected void sendApprovalNotification(TutorProfile tutor) {
        notificationService.send(tutor.getUserId(), "Hồ sơ của bạn đã được duyệt!");
    }

    @Override
    protected void sendRejectionNotification(TutorProfile tutor, String reason) {
        notificationService.send(tutor.getUserId(), "Hồ sơ bị từ chối: " + reason);
    }

    // Hook: sau khi duyệt, tự động index vào search engine (Elasticsearch nếu có)
    @Override
    protected void doPostApprove(TutorProfile tutor) {
        // tutorSearchIndexer.index(tutor);
    }
}
```

### Ứng dụng 2: Payment Gateway (VNPay / Momo / ZaloPay)

```java
public abstract class PaymentGatewayTemplate {

    public final PaymentResult process(PaymentRequest request) {
        validateRequest(request);
        PaymentResult result = callGateway(request);   // khác nhau theo cổng
        handleCallback(result);
        updatePaymentRecord(result);
        return result;
    }

    protected abstract void validateRequest(PaymentRequest request);
    protected abstract PaymentResult callGateway(PaymentRequest request);
    protected abstract void handleCallback(PaymentResult result);
    protected void updatePaymentRecord(PaymentResult result) { /* default logic */ }
}

@Service
public class VNPayGatewayService extends PaymentGatewayTemplate {
    @Override
    protected PaymentResult callGateway(PaymentRequest request) {
        // gọi VNPay API
    }
    // ...
}

@Service
public class MomoGatewayService extends PaymentGatewayTemplate {
    @Override
    protected PaymentResult callGateway(PaymentRequest request) {
        // gọi Momo API
    }
    // ...
}
```

---

## 5. Design Pattern: Visitor

Visitor cho phép thêm hành vi mới vào các đối tượng mà không thay đổi class của chúng. Phù hợp khi có nhiều loại profile (Tutor, Student, Parent) cần xử lý khác nhau cho từng nghiệp vụ.

### Khai báo interface

```java
// common/visitor/Visitable.java
public interface Visitable {
    <R> R accept(ProfileVisitor<R> visitor);
}

// common/visitor/ProfileVisitor.java
public interface ProfileVisitor<R> {
    R visitTutor(TutorProfile tutor);
    R visitStudent(StudentProfile student);
    R visitParent(ParentProfile parent);
}
```

### Các entity implement Visitable

```java
// domain/tutor/model/TutorProfile.java
@Entity
public class TutorProfile implements Visitable {
    // ... fields ...

    @Override
    public <R> R accept(ProfileVisitor<R> visitor) {
        return visitor.visitTutor(this);
    }
}

// domain/student/model/StudentProfile.java
@Entity
public class StudentProfile implements Visitable {
    @Override
    public <R> R accept(ProfileVisitor<R> visitor) {
        return visitor.visitStudent(this);
    }
}
```

### Visitor cụ thể

```java
// Visitor 1: Tính phí nền tảng (platform fee) theo loại người dùng
@Component
public class PlatformFeeVisitor implements ProfileVisitor<BigDecimal> {

    @Override
    public BigDecimal visitTutor(TutorProfile tutor) {
        // Gia sư trả phí hoa hồng 10% mỗi buổi học
        return BigDecimal.valueOf(0.10);
    }

    @Override
    public BigDecimal visitStudent(StudentProfile student) {
        // Học sinh không mất phí nền tảng
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal visitParent(ParentProfile parent) {
        // Phụ huynh không mất phí nền tảng
        return BigDecimal.ZERO;
    }
}

// Visitor 2: Tạo notification message phù hợp từng vai trò
@Component
public class WelcomeNotificationVisitor implements ProfileVisitor<String> {

    @Override
    public String visitTutor(TutorProfile tutor) {
        return String.format(
            "Chào mừng gia sư %s! Hãy hoàn thiện hồ sơ để được duyệt và xuất hiện trên trang tìm kiếm.",
            tutor.getUser().getFullName()
        );
    }

    @Override
    public String visitStudent(StudentProfile student) {
        return String.format(
            "Chào %s! Tìm ngay gia sư phù hợp và đặt lịch học đầu tiên.",
            student.getUser().getFullName()
        );
    }

    @Override
    public String visitParent(ParentProfile parent) {
        return String.format(
            "Chào %s! Liên kết tài khoản con để quản lý lịch học và thanh toán dễ dàng hơn.",
            parent.getUser().getFullName()
        );
    }
}

// Visitor 3: Export dữ liệu (CSV/Excel cho admin)
@Component
public class ProfileExportVisitor implements ProfileVisitor<Map<String, Object>> {

    @Override
    public Map<String, Object> visitTutor(TutorProfile tutor) {
        return Map.of(
            "type", "TUTOR",
            "name", tutor.getUser().getFullName(),
            "rating", tutor.getRatingAvg(),
            "hourlyRate", tutor.getHourlyRate(),
            "status", tutor.getStatus()
        );
    }

    @Override
    public Map<String, Object> visitStudent(StudentProfile student) {
        return Map.of(
            "type", "STUDENT",
            "name", student.getUser().getFullName(),
            "grade", student.getGradeLevel()
        );
    }

    @Override
    public Map<String, Object> visitParent(ParentProfile parent) {
        return Map.of(
            "type", "PARENT",
            "name", parent.getUser().getFullName(),
            "occupation", parent.getOccupation()
        );
    }
}
```

### Sử dụng trong Service

```java
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final PlatformFeeVisitor feeVisitor;
    private final WelcomeNotificationVisitor welcomeVisitor;

    public BigDecimal getPlatformFee(Visitable profile) {
        return profile.accept(feeVisitor); // không cần instanceof
    }

    public String buildWelcomeMessage(Visitable profile) {
        return profile.accept(welcomeVisitor);
    }
}
```

---

## 6. pom.xml – Các dependency quan trọng

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>

    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.6.3</version>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>1.6.3</version>
        <optional>true</optional>
    </dependency>

    <!-- Caffeine Cache -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
</dependencies>
```

---

## 7. BaseEntity – Tránh lặp code ở mọi Entity

```java
// common/base/BaseEntity.java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

// Entity kế thừa:
@Entity
@Table(name = "tutor_profiles")
@Getter @Setter
public class TutorProfile extends BaseEntity implements Visitable {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String headline;
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "tutor_status")
    private TutorStatus status = TutorStatus.DRAFT;

    // ... các field khác ...

    @Override
    public <R> R accept(ProfileVisitor<R> visitor) {
        return visitor.visitTutor(this);
    }
}
```

---

## 8. RBAC với Spring Security

```java
// Annotation custom để kiểm tra permission slug
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@permissionEvaluator.hasPermission(authentication, #permission)")
public @interface RequirePermission {
    String value(); // ví dụ: "tutor:approve"
}

// Sử dụng trong controller
@RestController
@RequestMapping("/api/admin/tutors")
public class TutorAdminController {

    @PatchMapping("/{id}/approve")
    @RequirePermission("tutor:approve")
    public ResponseEntity<Void> approve(@PathVariable Long id, ...) { ... }

    @PatchMapping("/{id}/reject")
    @RequirePermission("tutor:approve")
    public ResponseEntity<Void> reject(@PathVariable Long id, ...) { ... }
}
```

---

## 9. Tóm tắt các quyết định thiết kế

| Vấn đề | Quyết định | Lý do |
|---|---|---|
| Package structure | Package by Feature | Dễ tìm file, cohesive theo domain |
| Schema migration | Flyway | Quản lý version, tránh `ddl-auto=create` |
| ORM mapping | JPA + Hibernate | Standard, native query khi cần |
| DTO mapping | MapStruct | Compile-time, không reflection |
| Template Method | Approval & Payment workflow | Bộ khung cố định, bước thay đổi được |
| Visitor | Profile behavior dispatch | Tránh instanceof, dễ thêm behavior mới |
| Auth | JWT (stateless) + Refresh Token | Scale được, refresh token lưu DB để revoke |
| RBAC | Permission slug `module:action` | Granular, thêm permission không sửa code |
