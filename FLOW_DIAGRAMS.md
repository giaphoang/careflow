# Hospital Management System - Flow Diagrams

Here are complete flow diagrams that you can use for your college project documentation. These diagrams use [Mermaid.js](https://mermaid.js.org/) syntax, which is natively supported by GitHub, GitLab, and many Markdown editors (like Notion or Obsidian). 

You can either screenshot the rendered diagrams below or copy the code blocks directly into your project file if it supports Mermaid.

## 1. High-Level System Architecture Flow

This diagram illustrates how different users interact with the system, how the frontend communicates with the backend, and how external services (like Google OAuth and WhatsApp) are integrated.

```mermaid
flowchart TD
    %% Actors
    Admin([Admin])
    Receptionist([Receptionist])
    Doctor([Doctor])
    Patient([Patient / User])

    %% Frontend
    subgraph Frontend [Frontend (React + Vite)]
        WebUI[Web Application Interface]
    end

    %% External Identity
    GoogleOAuth[Google OAuth Provider]

    %% Backend
    subgraph Backend [Backend (Spring Boot 3)]
        Security[Spring Security + JWT Filter]
        
        subgraph Services [Business Logic & Modules]
            AuthService[Auth Management]
            PatientService[Patient Management]
            DoctorService[Doctor Management]
            ApptService[Appointment Scheduling]
            AdmissionService[Admission & Discharge]
            BillingService[Billing & Invoices]
        end
        
        subgraph Notifications [Asynchronous Notification System]
            OutboxWriter[Outbox Writer]
            OutboxPoller[Background Worker Poller]
        end
        
        Security --> Services
        ApptService --> OutboxWriter
        OutboxPoller -.-> OutboxWriter
    end

    %% External Services
    WhatsApp[WhatsApp / Twilio API]

    %% Database
    subgraph DatabaseLayer [PostgreSQL Database]
        DB[(HMS Relational DB)]
        OutboxTable[(Notification Outbox Table)]
    end

    %% Connections
    Admin --> WebUI
    Receptionist --> WebUI
    Doctor --> WebUI
    Patient -.-> WebUI
    
    WebUI -- "1. Login (Google)" --> GoogleOAuth
    GoogleOAuth -- "2. OAuth Code" --> WebUI
    WebUI -- "3. REST API Requests (JWT Bearer)" --> Security
    
    Services <--> DB
    OutboxWriter --> OutboxTable
    OutboxPoller <--> OutboxTable
    
    OutboxPoller -- "Webhook / HTTP POST" --> WhatsApp
    WhatsApp -- "Sends SMS/Message" --> Patient
```

---

## 2. Appointment Booking & Notification Sequence

This sequence diagram explains the exact chronological flow of what happens when a receptionist books an appointment, emphasizing the transactional outbox pattern used to ensure notifications are sent reliably.

```mermaid
sequenceDiagram
    autonumber
    actor Receptionist
    participant UI as Frontend (React)
    participant API as Spring Boot API
    participant DB as PostgreSQL
    participant Worker as Background Worker
    participant WhatsApp as WhatsApp API
    actor Patient

    Receptionist->>UI: Select Doctor, Patient, Time & Book
    UI->>API: POST /api/appointments (JWT)
    
    rect rgb(235, 245, 255)
    note right of API: Database Transaction Starts
    API->>DB: Save Appointment Record
    API->>DB: Save Notification to Outbox (PENDING)
    DB-->>API: Transaction Committed
    end
    
    API-->>UI: 201 Created (Appointment Details)
    UI-->>Receptionist: Show Success Message
    
    loop Every 5 seconds
        Worker->>DB: Poll PENDING notifications (Batch)
        DB-->>Worker: Return Notifications List
        
        Worker->>WhatsApp: Send Message (HTTP POST)
        
        alt Success
            WhatsApp-->>Worker: 200 OK
            Worker->>DB: Update Outbox Status = SENT
            WhatsApp->>Patient: Delivers WhatsApp Message
        else Failure
            WhatsApp-->>Worker: Error / Timeout
            Worker->>DB: Update Outbox (Increment attempts, PENDING/FAILED)
        end
    end
```

---

## 3. Core Entity-Relationship Diagram (ERD)

This diagram highlights the relational database structure, showing how tables connect with each other.

```mermaid
erDiagram
    USERS {
        Long id PK
        String username
        String password
        String role
    }
    
    PATIENTS {
        Long id PK
        String name
        String phone
        String email
    }
    
    DOCTORS {
        Long id PK
        String name
        String specialization
        Long user_id FK
    }
    
    APPOINTMENTS {
        Long id PK
        Long patient_id FK
        Long doctor_id FK
        DateTime appointment_time
        String status
    }
    
    ADMISSIONS {
        Long id PK
        Long patient_id FK
        DateTime admission_date
        DateTime discharge_date
        String status
    }
    
    INVOICES {
        Long id PK
        Long patient_id FK
        BigDecimal amount
        String status
    }
    
    NOTIFICATION_OUTBOX {
        Long id PK
        String recipient
        String message
        String status
        Int attempts
    }

    USERS ||--o{ DOCTORS : "acts as"
    PATIENTS ||--o{ APPOINTMENTS : "books"
    DOCTORS ||--o{ APPOINTMENTS : "assigned to"
    PATIENTS ||--o{ ADMISSIONS : "has"
    PATIENTS ||--o{ INVOICES : "receives"
```

---

## 4. Role-Based Access Control (RBAC) Flow

This diagram shows what different actors are authorized to do within the system.

```mermaid
flowchart LR
    Admin([Admin])
    Receptionist([Receptionist])
    Doctor([Doctor])

    subgraph System APIs
        UserMgmt[User Management API]
        PatientMgmt[Patient & Appointment API]
        BillingMgmt[Billing & Admissions API]
        DoctorView[Doctor Specific API]
    end

    Admin -->|Full Access| UserMgmt
    Admin -->|Full Access| PatientMgmt
    Admin -->|Full Access| BillingMgmt
    Admin -->|Full Access| DoctorView

    Receptionist -->|Manage| PatientMgmt
    Receptionist -->|Manage| BillingMgmt
    Receptionist -.->|View Only| DoctorView

    Doctor -->|Read/Complete Appts| DoctorView
    Doctor -.->|Read Only| PatientMgmt
```
