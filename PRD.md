# PRD: HAE Shop (High-Performance E-commerce Backend)

## 1. 프로젝트 개요

- **프로젝트명:** HAE Shop
- **목적:** Java 21 및 Spring Boot 3.5 기술 스택을 기반으로 한 DDD(도메인 주도 설계) 및 헥사고날 아키텍처(Hexagonal Architecture) 쇼핑몰 백엔드 구축.
- **핵심 가치:** - **고성능 및 확장성:** Java 21의 가상 스레드(Virtual Threads)를 활용한 동시성 처리 최적화 및 DB 커넥션 풀 고갈 방지.
  - **안정성 및 멱등성:** TDD, Testcontainers 기반의 검증, 분산 락(Distributed Lock)을 통한 데이터 정합성 보장 및 중복 결제/주문 방지(Idempotency).
  - **신뢰성:** Transactional Outbox 패턴과 Polling Publisher를 적용한 비동기 이벤트 처리 및 메시지 유실 완벽 차단.
  - **관측 가능성(Observability):** 가상 스레드 대기 상태 및 락 획득 모니터링 환경 구축.
  - **문서화 최적화:** AI 에이전트가 코드 구조와 비즈니스 흐름을 즉시 파악하고 컨텍스트를 유지할 수 있는 표준 설계 및 명시적 규칙 적용.

---

## 2. 기술 스택 (Technical Stack)

| 구분              | 기술 스택                            | 비고                                                 |
| :---------------- | :----------------------------------- | :--------------------------------------------------- |
| **Language**      | Java 21                              | Record, Virtual Threads 적극 활용                    |
| **Framework**     | Spring Boot 3.5.x                    | 최신 기능 및 성능 개선 반영                          |
| **Build Tool**    | Gradle (Groovy DSL)                  | **Kotlin 사용 금지**                                 |
| **Database**      | PostgreSQL 16+                       | 운영 및 로컬 개발용 핵심 데이터베이스                |
| **Cache & Lock**  | Redis (Redisson)                     | 로컬 캐시 외 분산 락 및 대기열 처리에 활용           |
| **Security**      | Spring Security & JWT                | Stateless 인증 및 RBAC 권한 관리                     |
| **Validation**    | Jakarta Bean Validation              | 도메인 엔티티 및 DTO 유효성 검증                     |
| **Local Cache**   | Caffeine Cache                       | 조회 성능 극대화 및 부하 분산                        |
| **ORM / Query**   | Spring Data JPA / Querydsl           | Type-safe 쿼리 구현                                  |
| **Resilience**    | Resilience4j                         | 외부 API 연동 시 서킷 브레이커 및 타임아웃 관리      |
| **Observability** | Micrometer & Prometheus              | 가상 스레드 및 커넥션 풀 상태 모니터링               |
| **Testing**       | JUnit 5, Mockito, **Testcontainers** | 멱등성 및 실제 DB 환경(PostgreSQL/Redis) 통합 테스트 |
| **Infra**         | Docker / Docker Compose              | 컨테이너 기반 실행 환경 제공                         |

---

## 3. 프로젝트 구조 (Folder Structure - DDD & Hexagonal)

도메인 모델의 응집도를 높이고 기술적 상세를 인프라 계층으로 완벽히 분리하기 위해 **DDD와 헥사고날 아키텍처(Ports and Adapters)**를 융합한 구조를 엄격히 따릅니다. 인프라는 도메인을 의존하지만, 도메인은 인프라를 모릅니다.

```text
hae-shop/
├── src/
│   ├── main/
│   │   ├── java/com/hae/shop/
│   │   │   ├── common/                # 공통 유틸, 글로벌 예외 처리, ErrorCode Enum
│   │   │   ├── config/                # Security, JPA, Redis, Virtual Thread, Actuator 설정
│   │   │   ├── domain/                # 핵심 도메인 (순수 Java, 프레임워크 의존성 최소화)
│   │   │   │   ├── member/
│   │   │   │   ├── product/           # 상품 및 재고 모델
│   │   │   │   ├── order/             # 주문 프로세스 및 결제
│   │   │   │   │   ├── model/         # Domain Entity, VO, Domain Event
│   │   │   │   │   └── port/          # 헥사고날 포트 (인터페이스)
│   │   │   │   │       ├── in/        # Usecase Interface (Application이 구현)
│   │   │   │   │       └── out/       # Repository, External API Interface (Infra가 구현)
│   │   │   │   └── coupon/            # 쿠폰 정책 및 할인 로직
│   │   │   ├── application/           # 유스케이스 서비스 (Transaction 관리, port.in 구현체)
│   │   │   ├── interfaces/            # API 컨트롤러, DTO, Mapper (v1 prefix, Web Adapter)
│   │   │   └── infrastructure/        # 구현체 어댑터 및 아웃박스 패턴
│   │   │       ├── external/          # 결제(PG), 이메일 연동 구현체 (Resilience4j 적용)
│   │   │       ├── outbox/            # Transactional Outbox 구현 (이벤트 저장 및 Polling)
│   │   │       └── persistence/       # JPA/Redis Repository 구현 (port.out 구현체) 및 Querydsl
│   │   └── resources/
│   │       ├── application.yml        # 공통 설정 (spring.threads.virtual.enabled=true)
│   │       ├── application-test.yml   # Testcontainers 설정
│   │       └── schema.sql             # DB 스키마 정의
│   └── test/
│       └── java/com/hae/shop/         # TDD 기반 단위/통합 테스트 및 ArchUnit 테스트
├── Dockerfile                         # Multi-stage 빌드 (JRE 21 기반)
├── docker-compose.yml                 # App + PostgreSQL + Redis 컨테이너 구성
└── build.gradle                       # Groovy 기반 빌드 설정
```

## 4. 핵심 기능 요구사항

### 4.1. 보안 및 인증 (Security)

- **JWT 기반 인증:** 로그인 시 Access Token과 Refresh Token을 발행하며, 서버는 세션 상태를 저장하지 않는 `STATELESS` 방식을 유지한다.
- **보안 로직:** Spring Security를 활용하여 비밀번호를 암호화(`BCrypt`)하고, `OncePerRequestFilter`를 상속받은 필터를 통해 요청마다 권한을 검증한다.
- **권한 관리:** RBAC(Role-Based Access Control)를 적용하여 일반 사용자(`ROLE_USER`)와 관리자(`ROLE_ADMIN`)의 API 접근 권한을 분리한다.

### 4.2. 선착순 타임 세일 (Concurrency & Idempotency)

- **동시성 해결 (분산 락 & 대기열):** 가상 스레드 사용 시 DB 락 대기로 인한 커넥션 풀 고갈을 방지하기 위해, 타임 세일 로직에는 **Redis 기반 분산 락(Redisson Pub/Sub)**을 우선 적용한다. 극단적인 트래픽이 예상되는 경우 Redis Waiting Queue를 도입하여 애플리케이션 유입량을 제어한다.
- **주문 멱등성 보장:** 클라이언트로부터 `Idempotency-Key` 헤더를 전달받아 처리하며, 네트워크 재시도로 인한 중복 주문 및 중복 결제를 원천 차단한다.
- **처리량 최적화:** Java 21의 **Virtual Threads**를 활성화하여 I/O 바운드 작업 시 서블릿 스레드가 고갈되지 않도록 설계하되, `synchronized` 블록 사용을 피하여 스레드 Pinning 현상을 방지한다.

### 4.3. 쿠폰 할인 시스템 (Cache Policy)

- **쿠폰 정책:** 정액(Fixed Amount) 및 정률(Percentage) 할인 기능을 구현한다. 쿠폰 적용 비즈니스 로직은 도메인 서비스 내부에 캡슐화한다.
- **상세 캐시 전략:** 조회 빈도가 높고 변경이 적은 카테고리 및 활성 쿠폰 목록은 `Caffeine Cache`를 적용하여 1차 로컬 캐싱한다.
  - **만료 정책:** `Expire After Write`를 10분으로 설정하여 주기적으로 최신화한다.
  - **메모리 보호:** `Maximum Size`를 1,000개로 제한하여 OutOfMemory를 방지한다.

### 4.4. 주문/결제 및 비동기 처리 (Transactional Outbox)

- **결제 연동:** 외부 PG사 연동을 위한 인터페이스(`PaymentGateway Port`)를 정의하고, 테스트 환경에서는 가상 성공/실패를 반환하는 Mock 구현체를 사용한다.
- **안전한 이벤트 발행:** 결제 트랜잭션과 알림 발송을 분리하기 위해 **Transactional Outbox 패턴**을 사용한다. 결제 성공 시 비즈니스 상태 변경과 함께 Outbox 테이블에 이벤트를 동일 트랜잭션으로 저장한다.
- **Relay 메커니즘 (Polling Publisher):** Spring `@Scheduled`를 이용하여 일정 주기(예: 3초)마다 Outbox 테이블의 미처리 이벤트를 읽어(Polling) 메시지 브로커나 외부 API로 발행하고, 성공 시 완료 상태로 마킹하여 메시지 유실을 완벽히 방지한다.

---

## 5. 개발 및 운영 가이드라인

### 5.1. TDD 및 테스트 전략

- **사이클 준수:** 핵심 비즈니스 로직은 **Red-Green-Refactor** TDD 사이클을 거쳐 개발한다.
- **컨테이너 기반 통합 테스트:** H2 데이터베이스 대신 **Testcontainers**를 도입하여, 테스트 실행 시 실제 운영과 동일한 PostgreSQL 및 Redis 인스턴스를 띄워 쿼리 및 락 동시성 검증의 신뢰성을 극대화한다.
- **동시성 테스트:** `CountDownLatch` 또는 `ExecutorService`를 활용하여 실제 100건 이상의 동시 재고 차감 요청이 완벽히 처리되는지 확인하는 통합 테스트를 의무화한다.
- **아키텍처 테스트:** `ArchUnit` 라이브러리를 도입하여 도메인 계층이 인프라 계층을 의존하지 않는지(`port.in/out` 규칙) 자동 검증한다.

### 5.2. 관측 가능성 및 로깅 (Observability)

- **Actuator 연동:** `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` 엔드포인트를 노출하여 앱 상태와 가상 스레드, DB 커넥션 풀(HikariCP) 현황을 외부 모니터링 시스템과 연동한다.
- **가상 스레드 로깅:** ThreadLocal 사용을 최소화하고, 가상 스레드가 플랫폼 스레드에 고정(Pinned)되는 현상이 발생하는지 추적할 수 있도록 JVM 옵션(`-Djdk.tracePinnedThreads=full`)을 활용한다.

### 5.3. 글로벌 에러 핸들링

- **ErrorCode Enum:** 비즈니스 예외 상황에 대해 고유한 에러 코드와 메시지를 정의한다.
- **통일된 응답:** `@RestControllerAdvice`를 통해 모든 예외를 `ErrorResponse` 객체로 변환하여 클라이언트에게 일관된 JSON 포맷을 반환한다.

---

## 6. AI 에이전트(AI Coding Assistant)를 위한 분석 지침

1. **에이전트 규칙 파일 운용:** 프로젝트 루트에 AI 에이전트가 항상 참조해야 하는 명시적인 룰셋 파일(예: `AGENTS.md` 형식의 규칙 파일)을 생성하여, 에이전트가 "컨트롤러에서 도메인 엔티티 반환 금지", "Outbox 패턴 필수 적용" 등의 프로젝트 컨텍스트를 잃지 않도록 강제한다.
2. **Javadoc 상세화:** 상태를 변경하는 모든 메서드에 대해 발생 가능한 `DomainEvent`, `ErrorCode`(Side-Effect), 비즈니스 의도를 Javadoc에 명시한다.
3. **DTO 분리 원칙:** 인터페이스(API) 계층에서는 오직 DTO만 사용하며, 도메인 엔티티(`Entity`)는 컨트롤러 계층 밖으로 절대 노출하지 않는다.
4. **API 명세:** `Springdoc-openapi`를 통해 Swagger UI를 제공하며, 모든 외부 API 엔드포인트는 `/api/v1/` 접두사를 사용한다.
