# Project Tree

Generated from tracked repository files by `scripts/generate_tree.py`.

```text
ticketsbackend
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
│   │   └── 0007-durable-order-flow-jdbc.md
│   ├── contributors.md
│   └── product-flow.md
├── pom.xml
├── src
│   ├── main
│   │   ├── kotlin
│   │   │   └── com
│   │   │       └── karrad
│   │   │           └── bilets
│   │   │               ├── BiletsApplication.kt
│   │   │               ├── application
│   │   │               │   ├── lock
│   │   │               │   │   └── EventLockManager.kt
│   │   │               │   ├── service
│   │   │               │   │   ├── CategoryService.kt
│   │   │               │   │   ├── EventService.kt
│   │   │               │   │   ├── InventoryPlanService.kt
│   │   │               │   │   ├── LayoutTemplateService.kt
│   │   │               │   │   ├── OrderService.kt
│   │   │               │   │   ├── OrganizationApplicationService.kt
│   │   │               │   │   ├── OrganizationMemberService.kt
│   │   │               │   │   ├── OrganizationService.kt
│   │   │               │   │   ├── TicketService.kt
│   │   │               │   │   ├── UserEventVisitService.kt
│   │   │               │   │   ├── UserService.kt
│   │   │               │   │   └── VenueService.kt
│   │   │               │   ├── transaction
│   │   │               │   │   └── OrderFlowTransactionManager.kt
│   │   │               │   └── usecase
│   │   │               │       ├── ConfirmOrderPaymentUseCase.kt
│   │   │               │       ├── CreateCategoryUseCase.kt
│   │   │               │       ├── CreateEventUseCase.kt
│   │   │               │       ├── CreateLayoutTemplateUseCase.kt
│   │   │               │       ├── CreateOrderUseCase.kt
│   │   │               │       ├── CreateOrganizationUseCase.kt
│   │   │               │       ├── CreateUserUseCase.kt
│   │   │               │       ├── CreateVenueUseCase.kt
│   │   │               │       ├── ExpireOrderUseCase.kt
│   │   │               │       ├── GenerateEventInventoryUseCase.kt
│   │   │               │       ├── GetEventDiscoveryUseCase.kt
│   │   │               │       ├── HoldEventSeatsUseCase.kt
│   │   │               │       ├── HoldGeneralAdmissionUseCase.kt
│   │   │               │       ├── ReleaseEventSeatsUseCase.kt
│   │   │               │       ├── ReleaseGeneralAdmissionUseCase.kt
│   │   │               │       ├── ReviewOrganizationApplicationUseCase.kt
│   │   │               │       ├── SearchEventsUseCase.kt
│   │   │               │       ├── SellEventSeatsUseCase.kt
│   │   │               │       ├── SellGeneralAdmissionUseCase.kt
│   │   │               │       └── SubmitOrganizationApplicationUseCase.kt
│   │   │               ├── config
│   │   │               │   ├── JacksonConfig.kt
│   │   │               │   ├── JdbcOrderFlowPersistenceConfig.kt
│   │   │               │   ├── PurchaseProperties.kt
│   │   │               │   ├── RepositoryConfig.kt
│   │   │               │   └── TimeConfig.kt
│   │   │               ├── domain
│   │   │               │   ├── dto
│   │   │               │   │   └── VenueRenderDto.kt
│   │   │               │   ├── entity
│   │   │               │   │   ├── AdmissionQuantity.kt
│   │   │               │   │   ├── Category.kt
│   │   │               │   │   ├── City.kt
│   │   │               │   │   ├── Event.kt
│   │   │               │   │   ├── EventAdmissionInventory.kt
│   │   │               │   │   ├── EventInventoryPlan.kt
│   │   │               │   │   ├── EventSeat.kt
│   │   │               │   │   ├── InventoryMode.kt
│   │   │               │   │   ├── LayoutTemplate.kt
│   │   │               │   │   ├── Order.kt
│   │   │               │   │   ├── Organization.kt
│   │   │               │   │   ├── OrganizationApplication.kt
│   │   │               │   │   ├── OrganizationMember.kt
│   │   │               │   │   ├── PaymentSession.kt
│   │   │               │   │   ├── Row.kt
│   │   │               │   │   ├── SeatKey.kt
│   │   │               │   │   ├── SeatTemplate.kt
│   │   │               │   │   ├── Section.kt
│   │   │               │   │   ├── Subject.kt
│   │   │               │   │   ├── Ticket.kt
│   │   │               │   │   ├── TicketType.kt
│   │   │               │   │   ├── User.kt
│   │   │               │   │   ├── UserEventVisit.kt
│   │   │               │   │   ├── Venue.kt
│   │   │               │   │   ├── VenueSpace.kt
│   │   │               │   │   └── VenueStruct.kt
│   │   │               │   ├── enums
│   │   │               │   │   ├── OrderStatus.kt
│   │   │               │   │   ├── OrganizationApplicationStatus.kt
│   │   │               │   │   ├── OrganizationMemberRole.kt
│   │   │               │   │   ├── SeatStatus.kt
│   │   │               │   │   └── UserRole.kt
│   │   │               │   ├── payment
│   │   │               │   │   └── PaymentGateway.kt
│   │   │               │   └── repository
│   │   │               │       ├── CategoryRepository.kt
│   │   │               │       ├── EventInventoryPlanRepository.kt
│   │   │               │       ├── EventRepository.kt
│   │   │               │       ├── LayoutTemplateRepository.kt
│   │   │               │       ├── OrderInventoryRepository.kt
│   │   │               │       ├── OrderRepository.kt
│   │   │               │       ├── OrganizationApplicationRepository.kt
│   │   │               │       ├── OrganizationMemberRepository.kt
│   │   │               │       ├── OrganizationRepository.kt
│   │   │               │       ├── TicketRepository.kt
│   │   │               │       ├── UserEventVisitRepository.kt
│   │   │               │       ├── UserRepository.kt
│   │   │               │       └── VenueRepository.kt
│   │   │               ├── infrastructure
│   │   │               │   ├── lock
│   │   │               │   │   └── InMemoryEventLockManager.kt
│   │   │               │   ├── payment
│   │   │               │   │   └── MockPaymentGateway.kt
│   │   │               │   ├── persistence
│   │   │               │   │   ├── inmemory
│   │   │               │   │   │   ├── InMemoryCategoryRepository.kt
│   │   │               │   │   │   ├── InMemoryEventInventoryPlanRepository.kt
│   │   │               │   │   │   ├── InMemoryEventRepository.kt
│   │   │               │   │   │   ├── InMemoryLayoutTemplateRepository.kt
│   │   │               │   │   │   ├── InMemoryOrderInventoryRepository.kt
│   │   │               │   │   │   ├── InMemoryOrderRepository.kt
│   │   │               │   │   │   ├── InMemoryOrganizationApplicationRepository.kt
│   │   │               │   │   │   ├── InMemoryOrganizationMemberRepository.kt
│   │   │               │   │   │   ├── InMemoryOrganizationRepository.kt
│   │   │               │   │   │   ├── InMemoryTicketRepository.kt
│   │   │               │   │   │   ├── InMemoryUserEventVisitRepository.kt
│   │   │               │   │   │   ├── InMemoryUserRepository.kt
│   │   │               │   │   │   └── InMemoryVenueRepository.kt
│   │   │               │   │   └── jdbc
│   │   │               │   │       ├── JdbcCategoryRepository.kt
│   │   │               │   │       ├── JdbcEventInventoryPlanRepository.kt
│   │   │               │   │       ├── JdbcEventRepository.kt
│   │   │               │   │       ├── JdbcLayoutTemplateRepository.kt
│   │   │               │   │       ├── JdbcOrderInventoryRepository.kt
│   │   │               │   │       ├── JdbcOrderRepository.kt
│   │   │               │   │       ├── JdbcOrganizationApplicationRepository.kt
│   │   │               │   │       ├── JdbcOrganizationMemberRepository.kt
│   │   │               │   │       ├── JdbcOrganizationRepository.kt
│   │   │               │   │       ├── JdbcRepositorySupport.kt
│   │   │               │   │       ├── JdbcTicketRepository.kt
│   │   │               │   │       ├── JdbcUserEventVisitRepository.kt
│   │   │               │   │       ├── JdbcUserRepository.kt
│   │   │               │   │       └── JdbcVenueRepository.kt
│   │   │               │   └── transaction
│   │   │               │       └── NoOpOrderFlowTransactionManager.kt
│   │   │               ├── sandbox.kt
│   │   │               ├── serialization
│   │   │               │   └── VenueStructSerialization.kt
│   │   │               ├── support
│   │   │               │   └── MutableClock.kt
│   │   │               └── web
│   │   │                   ├── ApiExceptionHandler.kt
│   │   │                   ├── CategoryController.kt
│   │   │                   ├── DiscoveryController.kt
│   │   │                   ├── EventController.kt
│   │   │                   ├── EventInventoryController.kt
│   │   │                   ├── InventoryPlanController.kt
│   │   │                   ├── LayoutTemplateController.kt
│   │   │                   ├── OrderController.kt
│   │   │                   ├── OrganizationApplicationController.kt
│   │   │                   ├── OrganizationController.kt
│   │   │                   ├── OrganizationMemberController.kt
│   │   │                   ├── TicketController.kt
│   │   │                   ├── UserController.kt
│   │   │                   ├── VenueController.kt
│   │   │                   ├── VenuePreviewController.kt
│   │   │                   └── dto
│   │   │                       ├── CategoryRequests.kt
│   │   │                       ├── DiscoveryResponses.kt
│   │   │                       ├── EventRequests.kt
│   │   │                       ├── InventoryGenerationRequest.kt
│   │   │                       ├── LayoutTemplateRequests.kt
│   │   │                       ├── OrderRequests.kt
│   │   │                       ├── OrganizationApplicationRequests.kt
│   │   │                       ├── OrganizationRequests.kt
│   │   │                       ├── UserRequests.kt
│   │   │                       └── VenueRequests.kt
│   │   └── resources
│   │       ├── application-jdbc-order-flow.yml
│   │       ├── application.yml
│   │       └── db
│   │           └── migration
│   │               └── V1__jdbc_order_flow.sql
│   └── test
│       ├── kotlin
│       │   └── com
│       │       └── karrad
│       │           └── bilets
│       │               ├── SandboxSmokeTests.kt
│       │               ├── application
│       │               │   ├── service
│       │               │   │   ├── ApplicationServicesTestConfig.kt
│       │               │   │   └── ApplicationServicesTests.kt
│       │               │   └── usecase
│       │               │       ├── CreateCategoryUseCaseTests.kt
│       │               │       ├── CreateEventUseCaseTests.kt
│       │               │       ├── CreateLayoutTemplateUseCaseTests.kt
│       │               │       ├── CreateOrganizationUseCaseTests.kt
│       │               │       ├── CreateUserUseCaseTests.kt
│       │               │       ├── CreateVenueUseCaseTests.kt
│       │               │       ├── GeneralAdmissionInventoryLifecycleUseCaseTests.kt
│       │               │       ├── GenerateEventInventoryUseCaseTests.kt
│       │               │       ├── GetEventDiscoveryUseCaseTests.kt
│       │               │       ├── HoldEventSeatsUseCaseTests.kt
│       │               │       ├── JdbcDurableOrderFlowConcurrencyTests.kt
│       │               │       ├── JdbcDurableOrderFlowTestConfig.kt
│       │               │       ├── JdbcDurableOrderFlowTests.kt
│       │               │       ├── OrganizationApplicationFlowUseCaseTests.kt
│       │               │       ├── PurchaseFlowUseCaseTests.kt
│       │               │       ├── ReleaseEventSeatsUseCaseTests.kt
│       │               │       ├── SearchEventsUseCaseTests.kt
│       │               │       └── SellEventSeatsUseCaseTests.kt
│       │               ├── domain
│       │               │   └── entity
│       │               │       ├── DomainEntityBehaviorTests.kt
│       │               │       ├── DomainValidationTests.kt
│       │               │       └── VenueStrucTests
│       │               │           └── VenueStructTests.kt
│       │               ├── infrastructure
│       │               │   └── persistence
│       │               │       └── inmemory
│       │               │           └── InMemoryRepositoriesTests.kt
│       │               ├── support
│       │               │   └── YamlPropertySourceFactory.kt
│       │               └── web
│       │                   ├── ApiExceptionHandlerTests.kt
│       │                   ├── CategoryControllerIntegrationTests.kt
│       │                   ├── DiscoveryControllerIntegrationTests.kt
│       │                   ├── EventControllerIntegrationTests.kt
│       │                   ├── EventInventoryControllerIntegrationTests.kt
│       │                   ├── EventSearchIntegrationTests.kt
│       │                   ├── EventSeatHoldControllerIntegrationTests.kt
│       │                   ├── EventSeatReleaseAndSaleControllerIntegrationTests.kt
│       │                   ├── GeneralAdmissionInventoryLifecycleIntegrationTests.kt
│       │                   ├── JdbcOrderFlowProfileIntegrationTests.kt
│       │                   ├── LayoutTemplateControllerIntegrationTests.kt
│       │                   ├── OrderControllerIntegrationTests.kt
│       │                   ├── OrganizationApplicationControllerIntegrationTests.kt
│       │                   ├── OrganizationControllerIntegrationTests.kt
│       │                   ├── OrganizationMemberControllerIntegrationTests.kt
│       │                   ├── ReadApiIntegrationTests.kt
│       │                   ├── UserControllerIntegrationTests.kt
│       │                   ├── VenueControllerIntegrationTests.kt
│       │                   └── VenuePreviewControllerIntegrationTests.kt
│       └── resources
│           └── application.yml
└── target
    ├── classes
    │   ├── META-INF
    │   │   └── bilets.kotlin_module
    │   ├── application.properties
    │   └── com
    │       └── karrad
    │           └── bilets
    │               ├── BiletsApplication.class
    │               ├── BiletsApplicationKt.class
    │               ├── SandboxKt.class
    │               ├── application
    │               │   ├── service
    │               │   │   ├── CategoryService.class
    │               │   │   ├── EventService.class
    │               │   │   ├── InventoryPlanService.class
    │               │   │   ├── LayoutTemplateService.class
    │               │   │   ├── OrganizationApplicationService.class
    │               │   │   ├── OrganizationMemberService.class
    │               │   │   ├── OrganizationService.class
    │               │   │   ├── UserEventVisitService.class
    │               │   │   ├── UserService.class
    │               │   │   └── VenueService.class
    │               │   └── usecase
    │               │       ├── CreateCategoryUseCase.class
    │               │       ├── CreateEventUseCase.class
    │               │       ├── CreateLayoutTemplateUseCase.class
    │               │       ├── CreateOrganizationUseCase.class
    │               │       ├── CreateUserUseCase.class
    │               │       ├── CreateVenueUseCase.class
    │               │       ├── GenerateEventInventoryUseCase.class
    │               │       ├── GetEventDiscoveryUseCase$get$$inlined$compareByDescending$1.class
    │               │       ├── GetEventDiscoveryUseCase$get$$inlined$compareByDescending$2.class
    │               │       ├── GetEventDiscoveryUseCase$get$$inlined$groupingBy$1.class
    │               │       ├── GetEventDiscoveryUseCase$get$$inlined$groupingBy$2.class
    │               │       ├── GetEventDiscoveryUseCase$get$$inlined$sortedBy$1.class
    │               │       ├── GetEventDiscoveryUseCase$get$$inlined$sortedBy$2.class
    │               │       ├── GetEventDiscoveryUseCase$get$$inlined$thenBy$1.class
    │               │       ├── GetEventDiscoveryUseCase$get$$inlined$thenBy$2.class
    │               │       ├── GetEventDiscoveryUseCase.class
    │               │       ├── HoldEventSeatsUseCase.class
    │               │       ├── HoldGeneralAdmissionUseCase.class
    │               │       ├── ReleaseEventSeatsUseCase.class
    │               │       ├── ReleaseGeneralAdmissionUseCase.class
    │               │       ├── ReviewOrganizationApplicationUseCase.class
    │               │       ├── SearchEventsUseCase$search$$inlined$compareBy$1.class
    │               │       ├── SearchEventsUseCase$search$$inlined$thenBy$1.class
    │               │       ├── SearchEventsUseCase.class
    │               │       ├── SellEventSeatsUseCase.class
    │               │       ├── SellGeneralAdmissionUseCase.class
    │               │       └── SubmitOrganizationApplicationUseCase.class
    │               ├── config
    │               │   ├── JacksonConfig.class
    │               │   └── RepositoryConfig.class
    │               ├── domain
    │               │   ├── dto
    │               │   │   ├── BoundsDto.class
    │               │   │   ├── SeatLayoutDto.class
    │               │   │   ├── SectionRenderDto.class
    │               │   │   ├── StageRenderDto.class
    │               │   │   └── VenueRenderDto.class
    │               │   ├── entity
    │               │   │   ├── AdmissionQuantity.class
    │               │   │   ├── Category.class
    │               │   │   ├── City.class
    │               │   │   ├── Event.class
    │               │   │   ├── EventAdmissionInventory.class
    │               │   │   ├── EventInventoryPlan$Companion.class
    │               │   │   ├── EventInventoryPlan$WhenMappings.class
    │               │   │   ├── EventInventoryPlan$holdAdmission$$inlined$groupingBy$1.class
    │               │   │   ├── EventInventoryPlan$holdSeats$$inlined$groupingBy$1.class
    │               │   │   ├── EventInventoryPlan$releaseAdmission$$inlined$groupingBy$1.class
    │               │   │   ├── EventInventoryPlan$releaseSeats$$inlined$groupingBy$1.class
    │               │   │   ├── EventInventoryPlan$sellAdmission$$inlined$groupingBy$1.class
    │               │   │   ├── EventInventoryPlan$sellSeats$$inlined$groupingBy$1.class
    │               │   │   ├── EventInventoryPlan.class
    │               │   │   ├── EventSeat.class
    │               │   │   ├── InventoryMode.class
    │               │   │   ├── LayoutTemplate.class
    │               │   │   ├── Organization.class
    │               │   │   ├── OrganizationApplication$WhenMappings.class
    │               │   │   ├── OrganizationApplication.class
    │               │   │   ├── OrganizationMember.class
    │               │   │   ├── Row.class
    │               │   │   ├── SeatKey.class
    │               │   │   ├── SeatTemplate.class
    │               │   │   ├── Section$special$$inlined$groupingBy$1.class
    │               │   │   ├── Section.class
    │               │   │   ├── Subject.class
    │               │   │   ├── TicketType.class
    │               │   │   ├── User.class
    │               │   │   ├── UserEventVisit.class
    │               │   │   ├── Venue$special$$inlined$groupingBy$1.class
    │               │   │   ├── Venue.class
    │               │   │   ├── VenueSpace.class
    │               │   │   ├── VenueStruct$special$$inlined$groupingBy$1.class
    │               │   │   └── VenueStruct.class
    │               │   ├── enums
    │               │   │   ├── OrganizationApplicationStatus.class
    │               │   │   ├── OrganizationMemberRole.class
    │               │   │   ├── SeatStatus.class
    │               │   │   └── UserRole.class
    │               │   └── repository
    │               │       ├── CategoryRepository.class
    │               │       ├── EventInventoryPlanRepository.class
    │               │       ├── EventRepository.class
    │               │       ├── LayoutTemplateRepository.class
    │               │       ├── OrganizationApplicationRepository.class
    │               │       ├── OrganizationMemberRepository.class
    │               │       ├── OrganizationRepository.class
    │               │       ├── UserEventVisitRepository.class
    │               │       ├── UserRepository.class
    │               │       └── VenueRepository.class
    │               ├── infrastructure
    │               │   └── persistence
    │               │       └── inmemory
    │               │           ├── InMemoryCategoryRepository.class
    │               │           ├── InMemoryEventInventoryPlanRepository.class
    │               │           ├── InMemoryEventRepository.class
    │               │           ├── InMemoryLayoutTemplateRepository.class
    │               │           ├── InMemoryOrganizationApplicationRepository.class
    │               │           ├── InMemoryOrganizationMemberRepository.class
    │               │           ├── InMemoryOrganizationRepository.class
    │               │           ├── InMemoryUserEventVisitRepository.class
    │               │           ├── InMemoryUserRepository.class
    │               │           └── InMemoryVenueRepository.class
    │               ├── serialization
    │               │   └── VenueStructSerialization.class
    │               └── web
    │                   ├── ApiExceptionHandler.class
    │                   ├── CategoryController.class
    │                   ├── DiscoveryController.class
    │                   ├── EventController.class
    │                   ├── EventInventoryController.class
    │                   ├── InventoryPlanController.class
    │                   ├── LayoutTemplateController.class
    │                   ├── OrganizationApplicationController.class
    │                   ├── OrganizationController.class
    │                   ├── OrganizationMemberController.class
    │                   ├── UserController.class
    │                   ├── VenueController.class
    │                   ├── VenuePreviewController.class
    │                   └── dto
    │                       ├── AdmissionInventoryActionRequest.class
    │                       ├── AdmissionInventoryItemRequest.class
    │                       ├── CityRequest.class
    │                       ├── CreateCategoryRequest.class
    │                       ├── CreateEventRequest.class
    │                       ├── CreateLayoutTemplateRequest.class
    │                       ├── CreateOrganizationApplicationRequest.class
    │                       ├── CreateOrganizationRequest.class
    │                       ├── CreateUserRequest.class
    │                       ├── CreateVenueRequest.class
    │                       ├── EventDiscoveryResponse.class
    │                       ├── GeneralAdmissionInventoryGenerationRequest.class
    │                       ├── HoldSeatsRequest.class
    │                       ├── ReviewOrganizationApplicationRequest.class
    │                       ├── RowRequest.class
    │                       ├── SeatKeyRequest.class
    │                       ├── SeatedInventoryGenerationRequest.class
    │                       ├── SectionRequest.class
    │                       ├── SubjectRequest.class
    │                       ├── TicketTypeRequest.class
    │                       └── VenueSpaceRequest.class
    ├── maven-status
    │   └── maven-compiler-plugin
    │       ├── compile
    │       │   └── default-compile
    │       │       └── inputFiles.lst
    │       └── testCompile
    │           └── default-testCompile
    │               ├── createdFiles.lst
    │               └── inputFiles.lst
    ├── surefire-reports
    │   ├── TEST-com.karrad.bilets.application.service.ApplicationServicesTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.CreateCategoryUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.CreateEventUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.CreateLayoutTemplateUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.CreateOrganizationUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.CreateUserUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.CreateVenueUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.GeneralAdmissionInventoryLifecycleUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.GenerateEventInventoryUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.GetEventDiscoveryUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.HoldEventSeatsUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.OrganizationApplicationFlowUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.ReleaseEventSeatsUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.SearchEventsUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.application.usecase.SellEventSeatsUseCaseTests.xml
    │   ├── TEST-com.karrad.bilets.domain.entity.VenueStructTests.xml
    │   ├── TEST-com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryRepositoriesTests.xml
    │   ├── TEST-com.karrad.bilets.web.CategoryControllerIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.DiscoveryControllerIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.EventControllerIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.EventInventoryControllerIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.EventSearchIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.EventSeatHoldControllerIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.EventSeatReleaseAndSaleControllerIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.GeneralAdmissionInventoryLifecycleIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.LayoutTemplateControllerIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.OrganizationApplicationControllerIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.OrganizationControllerIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.OrganizationMemberControllerIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.ReadApiIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.UserControllerIntegrationTests.xml
    │   ├── TEST-com.karrad.bilets.web.VenueControllerIntegrationTests.xml
    │   ├── com.karrad.bilets.application.service.ApplicationServicesTests.txt
    │   ├── com.karrad.bilets.application.usecase.CreateCategoryUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.CreateEventUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.CreateLayoutTemplateUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.CreateOrganizationUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.CreateUserUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.CreateVenueUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.GeneralAdmissionInventoryLifecycleUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.GenerateEventInventoryUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.GetEventDiscoveryUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.HoldEventSeatsUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.OrganizationApplicationFlowUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.ReleaseEventSeatsUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.SearchEventsUseCaseTests.txt
    │   ├── com.karrad.bilets.application.usecase.SellEventSeatsUseCaseTests.txt
    │   ├── com.karrad.bilets.domain.entity.VenueStructTests.txt
    │   ├── com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryRepositoriesTests.txt
    │   ├── com.karrad.bilets.web.CategoryControllerIntegrationTests.txt
    │   ├── com.karrad.bilets.web.DiscoveryControllerIntegrationTests.txt
    │   ├── com.karrad.bilets.web.EventControllerIntegrationTests.txt
    │   ├── com.karrad.bilets.web.EventInventoryControllerIntegrationTests.txt
    │   ├── com.karrad.bilets.web.EventSearchIntegrationTests.txt
    │   ├── com.karrad.bilets.web.EventSeatHoldControllerIntegrationTests.txt
    │   ├── com.karrad.bilets.web.EventSeatReleaseAndSaleControllerIntegrationTests.txt
    │   ├── com.karrad.bilets.web.GeneralAdmissionInventoryLifecycleIntegrationTests.txt
    │   ├── com.karrad.bilets.web.LayoutTemplateControllerIntegrationTests.txt
    │   ├── com.karrad.bilets.web.OrganizationApplicationControllerIntegrationTests.txt
    │   ├── com.karrad.bilets.web.OrganizationControllerIntegrationTests.txt
    │   ├── com.karrad.bilets.web.OrganizationMemberControllerIntegrationTests.txt
    │   ├── com.karrad.bilets.web.ReadApiIntegrationTests.txt
    │   ├── com.karrad.bilets.web.UserControllerIntegrationTests.txt
    │   └── com.karrad.bilets.web.VenueControllerIntegrationTests.txt
    └── test-classes
        ├── META-INF
        │   └── bilets.kotlin_module
        └── com
            └── karrad
                └── bilets
                    ├── application
                    │   ├── service
                    │   │   ├── ApplicationServicesTestConfig.class
                    │   │   └── ApplicationServicesTests.class
                    │   └── usecase
                    │       ├── CreateCategoryUseCaseTests.class
                    │       ├── CreateEventUseCaseTests.class
                    │       ├── CreateLayoutTemplateUseCaseTests.class
                    │       ├── CreateOrganizationUseCaseTests.class
                    │       ├── CreateUserUseCaseTests.class
                    │       ├── CreateVenueUseCaseTests.class
                    │       ├── GeneralAdmissionInventoryLifecycleUseCaseTests.class
                    │       ├── GenerateEventInventoryUseCaseTests.class
                    │       ├── GetEventDiscoveryUseCaseTests.class
                    │       ├── HoldEventSeatsUseCaseTests.class
                    │       ├── OrganizationApplicationFlowUseCaseTests.class
                    │       ├── ReleaseEventSeatsUseCaseTests.class
                    │       ├── SearchEventsUseCaseTests.class
                    │       └── SellEventSeatsUseCaseTests.class
                    ├── domain
                    │   └── entity
                    │       └── VenueStructTests.class
                    ├── infrastructure
                    │   └── persistence
                    │       └── inmemory
                    │           └── InMemoryRepositoriesTests.class
                    └── web
                        ├── CategoryControllerIntegrationTests.class
                        ├── DiscoveryControllerIntegrationTests.class
                        ├── EventControllerIntegrationTests.class
                        ├── EventInventoryControllerIntegrationTests.class
                        ├── EventSearchIntegrationTests.class
                        ├── EventSeatHoldControllerIntegrationTests.class
                        ├── EventSeatReleaseAndSaleControllerIntegrationTests.class
                        ├── GeneralAdmissionInventoryLifecycleIntegrationTests.class
                        ├── LayoutTemplateControllerIntegrationTests.class
                        ├── OrganizationApplicationControllerIntegrationTests.class
                        ├── OrganizationControllerIntegrationTests.class
                        ├── OrganizationMemberControllerIntegrationTests.class
                        ├── ReadApiIntegrationTests.class
                        ├── UserControllerIntegrationTests.class
                        └── VenueControllerIntegrationTests.class
```
