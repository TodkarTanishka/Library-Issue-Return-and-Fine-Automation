# Database Schema & Normalization Documentation (MMCOE Library)

## 1. Entity-Relationship Diagram (ERD)

```mermaid
erDiagram
    USERS ||--o{ ISSUED_BOOKS : "borrows"
    USERS ||--o{ ISSUE_REQUESTS : "requests"
    USERS ||--o{ WAITLIST : "queues"
    USERS ||--o{ FINES : "incurs"
    USERS ||--o{ NOTIFICATIONS : "receives"
    BOOKS ||--o{ ISSUED_BOOKS : "loaned as"
    BOOKS ||--o{ ISSUE_REQUESTS : "requested as"
    BOOKS ||--o{ WAITLIST : "queued for"
    ISSUED_BOOKS ||--o| FINES : "generates penalty"

    USERS {
        string id PK
        string full_name
        string email UK
        string password_hash
        string department
        string role
    }

    BOOKS {
        int id PK
        string book_id UK
        string title
        string author
        string isbn UK
        string category
        string department
        int total_quantity
        int available_quantity
    }

    ISSUED_BOOKS {
        int id PK
        string student_username FK
        int book_id FK
        date issue_date
        date due_date
        string status
    }

    ISSUE_REQUESTS {
        int id PK
        string user_email FK
        string book_id FK
        timestamp requested_at
        string status
        string librarian_id
        string decision_reason
    }

    WAITLIST {
        int id PK
        string student_username FK
        string book_id FK
        int position
        string status
        timestamp expires_at
    }

    FINES {
        int id PK
        int loan_id FK
        string student_username FK
        string book_id FK
        decimal fine_amount
        decimal paid_amount
        decimal remaining_amount
        string status
    }

    AUDIT_LOG {
        int id PK
        string actor_id
        string actor_role
        string action_type
        string entity_type
        string entity_id
        text details
        timestamp created_at
    }
```

---

## 2. Database Normalization Analysis (1NF ➔ 3NF)

### 1️⃣ First Normal Form (1NF)
- All columns contain atomic, non-divisible scalar values.
- Repeating groups and multi-valued attributes (such as array strings for books) were eliminated by decoupling into distinct entity tables (`users`, `books`, `issued_books`, `waitlist`, `fines`).
- Every table has a clearly defined Primary Key (`PK`).

### 2️⃣ Second Normal Form (2NF)
- All non-key attributes are fully functionally dependent on the entire Primary Key.
- Partial dependencies were removed:
  - Book metadata (`title`, `author`, `isbn`, `rack_number`) is stored solely in `books`.
  - Loan lifecycle events reference `books(id)` via Foreign Keys rather than repeating title/author attributes across transaction tables.

### 3️⃣ Third Normal Form (3NF)
- Transitive dependencies (`A -> B` and `B -> C`) were eliminated.
- User details (`full_name`, `department`, `role`) are dependent solely on `users.id`, not duplicated inside transaction rows (`issued_books`, `fines`, `waitlist`).
- Fine rate metrics (`fine_rate_per_day = ₹6.00`) and remaining balances (`remaining_amount = fine_amount - paid_amount`) are explicitly isolated to prevent anomaly cascades.
