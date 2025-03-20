# ShareWorker

분산 환경에서의 데이터 처리 및 공유를 위한 워커 시스템

## 시스템 개요
- 다양한 대상 서비스로의 데이터 공유 및 전파
- Circuit Breaker 패턴을 통한 장애 전파 방지
- 코루틴 기반의 비동기 처리
- Kafka를 활용한 이벤트 기반 아키텍처

## 아키텍처
![System Architecture](docs/images/architecture.png)

### 주요 컴포넌트
1. **Dispatcher**
   - 페이로드 타입별 적절한 핸들러로 라우팅
   - Circuit Breaker를 통한 장애 관리
   - 코루틴 기반 비동기 처리

2. **Share Handlers**
   - SNS, Email 등 대상 서비스별 전용 핸들러
   - 자체 상태 관리 및 모니터링
   - 실패 시 자동 재시도 및 알림

3. **Event System**
   - Kafka 기반 이벤트 수신 및 처리
   - 페이로드 검증 및 변환
   - 비동기 메시지 전달

4. **모니터링 & 알림**
   - Slack을 통한 실시간 장애 알림
   - 작업 처리 로그 기록
   - 시스템 상태 모니터링

## 기술 스택
- Kotlin 1.8.x
- Spring Boot 3.x
- Apache Kafka
- MongoDB
- Coroutines
- Slack API (알림)

## 주요 기능
1. **장애 관리**
   - Circuit Breaker 패턴으로 장애 전파 차단
   - 장애 발생 시 Slack을 통한 즉시 알림
   - 핸들러별 독립적인 장애 상태 관리

2. **확장성**
   - 독립적인 핸들러 구조로 신규 서비스 쉽게 추가
   - 코루틴 기반 비동기 처리로 리소스 효율화
   - 핸들러별 설정 분리 및 커스터마이징 가능

3. **운영 관리**
   - MongoDB 기반 공유 작업 이력 관리
   - 핸들러별 상태 모니터링
   - 실시간 알림 시스템

## 설정 가이드
- `application.yml`에서 Kafka, MongoDB 설정
- Slack 웹훅 URL 설정 필요
- 핸들러별 Circuit Breaker 파라미터 조정 가능

## 개발 가이드
- 새로운 핸들러 추가 시 `AbstractShareHandler` 상속
- Circuit Breaker 설정은 핸들러별로 커스터마이즈 가능
- 코루틴 스코프 및 예외 처리 주의
