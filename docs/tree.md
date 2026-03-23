# Project Tree

Generated from tracked repository files by `scripts/generate_tree.py`.

```text
ticketsbackend
├── .githooks
│   └── post-commit
├── .github
│   └── workflows
│       └── ci-cd.yml
├── .gitignore
├── Makefile
├── README.md
├── docs
│   ├── adr
│   │   ├── 0001-organization-v1.md
│   │   ├── 0002-organization-application-approval.md
│   │   ├── 0003-organization-owner-membership.md
│   │   ├── 0004-organization-owns-venues-and-events.md
│   │   ├── 0005-layout-template-ownership-via-venue.md
│   │   ├── 0006-sectioned-discovery-and-event-search.md
│   │   ├── 0007-durable-order-flow-jdbc.md
│   │   └── 0008-payment-attempt-and-current-user-boundary.md
│   ├── contributors.md
│   ├── current-state.md
│   └── product-flow.md
├── pom.xml
├── scripts
│   └── generate_tree.py
└── src
    ├── main
    │   ├── kotlin
    │   │   └── com
    │   │       └── karrad
    │   │           └── bilets
    │   │               ├── BiletsApplication.kt
    │   │               ├── application
    │   │               │   ├── lock
    │   │               │   │   └── EventLockManager.kt
    │   │               │   ├── ops
    │   │               │   │   └── OperationsScheduler.kt
    │   │               │   ├── service
    │   │               │   │   ├── CategoryService.kt
    │   │               │   │   ├── EventAvailabilityService.kt
    │   │               │   │   ├── EventService.kt
    │   │               │   │   ├── InventoryPlanService.kt
    │   │               │   │   ├── LayoutTemplateService.kt
    │   │               │   │   ├── OrderService.kt
    │   │               │   │   ├── OrganizationApplicationService.kt
    │   │               │   │   ├── OrganizationMemberService.kt
    │   │               │   │   ├── OrganizationService.kt
    │   │               │   │   ├── PaymentReconciliationService.kt
    │   │               │   │   ├── PaymentSettlementService.kt
    │   │               │   │   ├── TicketService.kt
    │   │               │   │   ├── UserEventVisitService.kt
    │   │               │   │   ├── UserService.kt
    │   │               │   │   └── VenueService.kt
    │   │               │   ├── transaction
    │   │               │   │   └── OrderFlowTransactionManager.kt
    │   │               │   └── usecase
    │   │               │       ├── CloseEventSalesUseCase.kt
    │   │               │       ├── ConfirmOrderPaymentUseCase.kt
    │   │               │       ├── CreateCategoryUseCase.kt
    │   │               │       ├── CreateEventUseCase.kt
    │   │               │       ├── CreateLayoutTemplateUseCase.kt
    │   │               │       ├── CreateOrderUseCase.kt
    │   │               │       ├── CreateOrganizationUseCase.kt
    │   │               │       ├── CreateUserUseCase.kt
    │   │               │       ├── CreateVenueUseCase.kt
    │   │               │       ├── ExpireOrderUseCase.kt
    │   │               │       ├── GenerateEventInventoryUseCase.kt
    │   │               │       ├── GetEventDiscoveryUseCase.kt
    │   │               │       ├── HandlePaymentCallbackUseCase.kt
    │   │               │       ├── HoldEventSeatsUseCase.kt
    │   │               │       ├── HoldGeneralAdmissionUseCase.kt
    │   │               │       ├── ProcessStalePaymentAttemptsUseCase.kt
    │   │               │       ├── ProcessStartedEventSalesUseCase.kt
    │   │               │       ├── ReleaseEventSeatsUseCase.kt
    │   │               │       ├── ReleaseGeneralAdmissionUseCase.kt
    │   │               │       ├── ReviewOrganizationApplicationUseCase.kt
    │   │               │       ├── SearchEventsUseCase.kt
    │   │               │       ├── SellEventSeatsUseCase.kt
    │   │               │       ├── SellGeneralAdmissionUseCase.kt
    │   │               │       └── SubmitOrganizationApplicationUseCase.kt
    │   │               ├── config
    │   │               │   ├── JacksonConfig.kt
    │   │               │   ├── JdbcOrderFlowPersistenceConfig.kt
    │   │               │   ├── OperationsProperties.kt
    │   │               │   ├── PurchaseProperties.kt
    │   │               │   ├── RepositoryConfig.kt
    │   │               │   └── TimeConfig.kt
    │   │               ├── domain
    │   │               │   ├── dto
    │   │               │   │   └── VenueRenderDto.kt
    │   │               │   ├── entity
    │   │               │   │   ├── AdmissionQuantity.kt
    │   │               │   │   ├── Category.kt
    │   │               │   │   ├── City.kt
    │   │               │   │   ├── Event.kt
    │   │               │   │   ├── EventAdmissionInventory.kt
    │   │               │   │   ├── EventInventoryPlan.kt
    │   │               │   │   ├── EventSeat.kt
    │   │               │   │   ├── InventoryMode.kt
    │   │               │   │   ├── LayoutTemplate.kt
    │   │               │   │   ├── Order.kt
    │   │               │   │   ├── Organization.kt
    │   │               │   │   ├── OrganizationApplication.kt
    │   │               │   │   ├── OrganizationMember.kt
    │   │               │   │   ├── PaymentAttempt.kt
    │   │               │   │   ├── PaymentCallbackAudit.kt
    │   │               │   │   ├── PaymentSession.kt
    │   │               │   │   ├── Row.kt
    │   │               │   │   ├── SeatKey.kt
    │   │               │   │   ├── SeatTemplate.kt
    │   │               │   │   ├── Section.kt
    │   │               │   │   ├── Subject.kt
    │   │               │   │   ├── Ticket.kt
    │   │               │   │   ├── TicketType.kt
    │   │               │   │   ├── User.kt
    │   │               │   │   ├── UserEventVisit.kt
    │   │               │   │   ├── Venue.kt
    │   │               │   │   ├── VenueSpace.kt
    │   │               │   │   └── VenueStruct.kt
    │   │               │   ├── enums
    │   │               │   │   ├── OrderStatus.kt
    │   │               │   │   ├── OrganizationApplicationStatus.kt
    │   │               │   │   ├── OrganizationMemberRole.kt
    │   │               │   │   ├── PaymentAttemptStatus.kt
    │   │               │   │   ├── PaymentCallbackStatus.kt
    │   │               │   │   ├── SeatStatus.kt
    │   │               │   │   └── UserRole.kt
    │   │               │   ├── payment
    │   │               │   │   └── PaymentGateway.kt
    │   │               │   └── repository
    │   │               │       ├── CategoryRepository.kt
    │   │               │       ├── EventInventoryPlanRepository.kt
    │   │               │       ├── EventRepository.kt
    │   │               │       ├── LayoutTemplateRepository.kt
    │   │               │       ├── OrderInventoryRepository.kt
    │   │               │       ├── OrderRepository.kt
    │   │               │       ├── OrganizationApplicationRepository.kt
    │   │               │       ├── OrganizationMemberRepository.kt
    │   │               │       ├── OrganizationRepository.kt
    │   │               │       ├── PaymentAttemptRepository.kt
    │   │               │       ├── PaymentCallbackAuditRepository.kt
    │   │               │       ├── TicketRepository.kt
    │   │               │       ├── UserEventVisitRepository.kt
    │   │               │       ├── UserRepository.kt
    │   │               │       └── VenueRepository.kt
    │   │               ├── infrastructure
    │   │               │   ├── lock
    │   │               │   │   └── InMemoryEventLockManager.kt
    │   │               │   ├── payment
    │   │               │   │   └── MockPaymentGateway.kt
    │   │               │   ├── persistence
    │   │               │   │   ├── inmemory
    │   │               │   │   │   ├── InMemoryCategoryRepository.kt
    │   │               │   │   │   ├── InMemoryEventInventoryPlanRepository.kt
    │   │               │   │   │   ├── InMemoryEventRepository.kt
    │   │               │   │   │   ├── InMemoryLayoutTemplateRepository.kt
    │   │               │   │   │   ├── InMemoryOrderInventoryRepository.kt
    │   │               │   │   │   ├── InMemoryOrderRepository.kt
    │   │               │   │   │   ├── InMemoryOrganizationApplicationRepository.kt
    │   │               │   │   │   ├── InMemoryOrganizationMemberRepository.kt
    │   │               │   │   │   ├── InMemoryOrganizationRepository.kt
    │   │               │   │   │   ├── InMemoryPaymentAttemptRepository.kt
    │   │               │   │   │   ├── InMemoryPaymentCallbackAuditRepository.kt
    │   │               │   │   │   ├── InMemoryTicketRepository.kt
    │   │               │   │   │   ├── InMemoryUserEventVisitRepository.kt
    │   │               │   │   │   ├── InMemoryUserRepository.kt
    │   │               │   │   │   └── InMemoryVenueRepository.kt
    │   │               │   │   └── jdbc
    │   │               │   │       ├── JdbcCategoryRepository.kt
    │   │               │   │       ├── JdbcEventInventoryPlanRepository.kt
    │   │               │   │       ├── JdbcEventRepository.kt
    │   │               │   │       ├── JdbcLayoutTemplateRepository.kt
    │   │               │   │       ├── JdbcOrderInventoryRepository.kt
    │   │               │   │       ├── JdbcOrderRepository.kt
    │   │               │   │       ├── JdbcOrganizationApplicationRepository.kt
    │   │               │   │       ├── JdbcOrganizationMemberRepository.kt
    │   │               │   │       ├── JdbcOrganizationRepository.kt
    │   │               │   │       ├── JdbcPaymentAttemptRepository.kt
    │   │               │   │       ├── JdbcPaymentCallbackAuditRepository.kt
    │   │               │   │       ├── JdbcRepositorySupport.kt
    │   │               │   │       ├── JdbcTicketRepository.kt
    │   │               │   │       ├── JdbcUserEventVisitRepository.kt
    │   │               │   │       ├── JdbcUserRepository.kt
    │   │               │   │       └── JdbcVenueRepository.kt
    │   │               │   └── transaction
    │   │               │       └── NoOpOrderFlowTransactionManager.kt
    │   │               ├── sandbox.kt
    │   │               ├── serialization
    │   │               │   └── VenueStructSerialization.kt
    │   │               ├── support
    │   │               │   └── MutableClock.kt
    │   │               └── web
    │   │                   ├── ApiExceptionHandler.kt
    │   │                   ├── CategoryController.kt
    │   │                   ├── CurrentUserProvider.kt
    │   │                   ├── DiscoveryController.kt
    │   │                   ├── EventController.kt
    │   │                   ├── EventInventoryController.kt
    │   │                   ├── InventoryPlanController.kt
    │   │                   ├── LayoutTemplateController.kt
    │   │                   ├── OperationsController.kt
    │   │                   ├── OrderController.kt
    │   │                   ├── OrganizationApplicationController.kt
    │   │                   ├── OrganizationController.kt
    │   │                   ├── OrganizationMemberController.kt
    │   │                   ├── PaymentController.kt
    │   │                   ├── TicketController.kt
    │   │                   ├── UserController.kt
    │   │                   ├── VenueController.kt
    │   │                   ├── VenuePreviewController.kt
    │   │                   └── dto
    │   │                       ├── CategoryRequests.kt
    │   │                       ├── DiscoveryResponses.kt
    │   │                       ├── EventRequests.kt
    │   │                       ├── InventoryGenerationRequest.kt
    │   │                       ├── LayoutTemplateRequests.kt
    │   │                       ├── OperationsBatchResponse.kt
    │   │                       ├── OrderRequests.kt
    │   │                       ├── OrganizationApplicationRequests.kt
    │   │                       ├── OrganizationRequests.kt
    │   │                       ├── PaymentRequests.kt
    │   │                       ├── UserRequests.kt
    │   │                       └── VenueRequests.kt
    │   └── resources
    │       ├── application-in-memory.yml
    │       ├── application-jdbc-order-flow.yml
    │       ├── application.yml
    │       └── db
    │           └── migration
    │               ├── V1__jdbc_order_flow.sql
    │               ├── V2__payment_model.sql
    │               └── V3__event_sales_closure.sql
    └── test
        ├── kotlin
        │   └── com
        │       └── karrad
        │           └── bilets
        │               ├── SandboxSmokeTests.kt
        │               ├── application
        │               │   ├── service
        │               │   │   ├── ApplicationServicesTestConfig.kt
        │               │   │   └── ApplicationServicesTests.kt
        │               │   └── usecase
        │               │       ├── CreateCategoryUseCaseTests.kt
        │               │       ├── CreateEventUseCaseTests.kt
        │               │       ├── CreateLayoutTemplateUseCaseTests.kt
        │               │       ├── CreateOrganizationUseCaseTests.kt
        │               │       ├── CreateUserUseCaseTests.kt
        │               │       ├── CreateVenueUseCaseTests.kt
        │               │       ├── GeneralAdmissionInventoryLifecycleUseCaseTests.kt
        │               │       ├── GenerateEventInventoryUseCaseTests.kt
        │               │       ├── GetEventDiscoveryUseCaseTests.kt
        │               │       ├── HoldEventSeatsUseCaseTests.kt
        │               │       ├── JdbcDurableOrderFlowConcurrencyTests.kt
        │               │       ├── JdbcDurableOrderFlowTestConfig.kt
        │               │       ├── JdbcDurableOrderFlowTests.kt
        │               │       ├── JdbcOrganizationApprovalTransactionTests.kt
        │               │       ├── OrganizationApplicationFlowUseCaseTests.kt
        │               │       ├── PurchaseFlowUseCaseTests.kt
        │               │       ├── ReleaseEventSeatsUseCaseTests.kt
        │               │       ├── SearchEventsUseCaseTests.kt
        │               │       └── SellEventSeatsUseCaseTests.kt
        │               ├── domain
        │               │   └── entity
        │               │       ├── DomainEntityBehaviorTests.kt
        │               │       ├── DomainValidationTests.kt
        │               │       └── VenueStrucTests
        │               │           └── VenueStructTests.kt
        │               ├── infrastructure
        │               │   └── persistence
        │               │       └── inmemory
        │               │           └── InMemoryRepositoriesTests.kt
        │               ├── support
        │               │   └── YamlPropertySourceFactory.kt
        │               └── web
        │                   ├── ApiExceptionHandlerTests.kt
        │                   ├── CategoryControllerIntegrationTests.kt
        │                   ├── DiscoveryControllerIntegrationTests.kt
        │                   ├── EventControllerIntegrationTests.kt
        │                   ├── EventInventoryControllerIntegrationTests.kt
        │                   ├── EventSearchIntegrationTests.kt
        │                   ├── EventSeatHoldControllerIntegrationTests.kt
        │                   ├── EventSeatReleaseAndSaleControllerIntegrationTests.kt
        │                   ├── GeneralAdmissionInventoryLifecycleIntegrationTests.kt
        │                   ├── JdbcOrderFlowProfileIntegrationTests.kt
        │                   ├── LayoutTemplateControllerIntegrationTests.kt
        │                   ├── OperationsControllerIntegrationTests.kt
        │                   ├── OrderControllerIntegrationTests.kt
        │                   ├── OrganizationApplicationControllerIntegrationTests.kt
        │                   ├── OrganizationControllerIntegrationTests.kt
        │                   ├── OrganizationMemberControllerIntegrationTests.kt
        │                   ├── PaymentControllerIntegrationTests.kt
        │                   ├── ReadApiIntegrationTests.kt
        │                   ├── UserControllerIntegrationTests.kt
        │                   ├── VenueControllerIntegrationTests.kt
        │                   └── VenuePreviewControllerIntegrationTests.kt
        └── resources
            ├── application-jdbc-order-flow.yml
            └── application.yml
```
