# MMCOE Library Management System

An end-to-end B.Tech Computer Engineering Pitch Competition Project for **Library Issue, Return, Fine Management Automation, and Custom DSA Realization**.

---

## 🏗️ System Architecture

```
Browser Frontend (HTML/Vanilla JS/Tailwind CSS)
       │
       ▼ (HTTP / REST API - Port 8080)
Java HTTP Gateway (`gateway/`)
       │
       ▼ (Custom TCP Socket Protocol - Port 9000)
Java Multi-Threaded TCP Server & Library Engine (`tcp-server/`)
       ├── In-Memory Data Structures (`dsa/`) [Trie, Min-Heap, FIFO Queues, Hash Tables]
       ├── Autonomous Background Scheduler (`AutomationScheduler`)
       └── MySQL Persistence & Transactions (`database/`) [JDBC Pool]
```

### Key Technical Innovations
- **Core Computer Engineering Realization**: No Spring Boot or React. Plain HTML5/Vanilla JS/Tailwind + multi-threaded Java engine + JDBC connection pool (~10 connections).
- **Hand-Written In-Memory DSA Layer**:
  - **Trie Autocomplete Index** (`datastructure.book.Trie`): $O(K)$ prefix search supporting digits, spaces, `+`, `-`, `.`, ranked by book issue popularity ($<1\text{ ms}$ latency).
  - **Binary Min-Heap Overdue Priority Queue** (`datastructure.fine.FineManager`): Binary min-heap ordered by `(due_date ASC, fine_amount DESC)` with an auxiliary `HashMap<loanId, index>` for $O(1)$ lookup and $O(\log N)$ decreaseKey.
  - **Linked-List FIFO Waitlist Queue** (`datastructure.issue.WaitingListManager`): Write-through linked list synced with MySQL `waitlist` table supporting $O(1)$ enqueue and $O(N)$ mid-queue cancellation.
  - **Hash Table User Search** (`datastructure.user.UserSearch`): $O(1)$ average direct user lookup via hash chaining.
- **OS Concurrency & Thread Pooling**:
  - `ThreadPoolExecutor` (core=10, max=50, bounded queue=100, CallerRunsPolicy).
  - Fine-grained synchronization using `ReentrantReadWriteLock` for reads and `ConcurrentHashMap<String, ReentrantLock>` per-book locks for writes.
  - 50-thread concurrent race test verified with zero stock underflow.
- **Autonomous Background Scheduler & Time Machine**:
  - `AutomationScheduler`: `ScheduledExecutorService` running `OverdueSweep` (daily 00:05), `DueReminders` (daily 09:00), `WaitlistReaper` (every 5 min), and `KPISnapshot` (every 15 sec).
  - **Time Machine Demo Mode**: Fast-forward virtual system clock (+1 day, +7 days, +14 days) via `POST /api/admin/time-travel`, executing all automation jobs on demand with live Server-Sent Events (SSE).

---

## 📁 Project Directory Structure

```
EDI project/
├── common/                  # Shared Domain Models (Book, User, Issue, FineRecord, VirtualClock, Session)
├── dsa/                     # Hand-written Data Structures & Algorithms
│   └── src/datastructure/   (Trie, Min-Heap, FIFO Queue, UserSearch, KPIManager)
├── database/                # Database Repositories, Migrations & Mermaid ERD
│   ├── mmcoe_library.sql    # Core database dump
│   ├── migration_v3.sql     # Indexes & audit_log table migration
│   └── schema_erd.md        # Mermaid ERD & 1NF➔3NF normalization notes
├── tcp-server/              # Core TCP Server, LibraryEngine & AutomationScheduler
├── gateway/                 # HTTP Gateway (REST & SSE Stream - Port 8080)
├── frontend/                # Production Dashboards & Admin "Judge Screen"
│   ├── landing.html         # Portal Landing Page & Architecture Explainer
│   ├── login.html           # Unified Login Page with Quick Demo Buttons
│   ├── dashboard.html       # Student Dashboard (Trie Autocomplete, Queue Tracker)
│   ├── librarian-dashboard.html # Librarian Desk (3-Column Workflow, Min-Heap Panel, Receipts)
│   ├── admin-dashboard.html # Admin "Judge Screen" (5 DSA Visualizers, Time Machine, Benchmarks)
│   └── js/api.js            # Frontend REST API client
├── tools/                   # Test & Benchmark Tool Suite
│   ├── ConcurrencyRaceTest.java # 50-Thread race condition stress test
│   └── AutomationTest.java      # Time Travel & Automation scheduler test
├── compile.sh               # One-click Linux build script
├── run-tests.sh             # Executes all automated verification tests
├── start-all.sh             # Launches TCP Server & HTTP Gateway daemons
├── PITCH_KIT.md             # 3-Minute pitch script, slide deck outline & Judge Q&A cheat sheet
├── KPI_AND_FAILURE_MODES.md # Empirical microsecond benchmarks & failure mode matrix
└── README.md                # System documentation
```

---

## 🚀 How to Run & Verify

### 1. Compile All Modules
```bash
./compile.sh
```

### 2. Run Automated Verification Tests
```bash
./run-tests.sh
```
*Runs the 50-thread OS concurrency race test and the Time Machine automation test suite.*

### 3. Launch Servers & Open Web UI
```bash
./start-all.sh
```
Open browser to: [http://localhost:8080/landing.html](http://localhost:8080/landing.html)

---

## 🔑 Demo Credentials

| Role | Email / ID | Password | Access Rights |
| :--- | :--- | :--- | :--- |
| **Student** | `student1@mmcoe.edu.in` | `student123` | Search Trie autocomplete, issue requests, view loans & queue status |
| **Librarian** | `librarian1@mmcoe.edu.in` | `librarian123` | Approve/Reject returns, Min-Heap urgent overdue panel, print receipts |
| **Admin** | `MMADMIN01` | `admin123` | "Under the Hood" DSA visualizer, Time Machine demo control, live benchmarks |
