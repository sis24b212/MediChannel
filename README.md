# MediChannel — Hospital Channelling and Appointment System

**Client:** Lanka Care Hospital, Colombo  
**Course:** IT404050 — Full Stack Development (4th Semester Group Project)  
**Technology Stack:** Next.js (JavaScript + Tailwind CSS) · Spring Boot (Java 17) · PostgreSQL

---

## Group Members

| Student ID | Name | Role & Primary Responsibility |
| :--- | :--- | :--- |
| **SIS/24/B2/12** | **Udeshika** *(Group Leader)* | System Architecture, Security, AES-256-GCM Encryption, Auth, Admin Portal & Deployment |
| **SIS/24/B2/01** | **Amanda** | Public Search, Patient Portal, Booking Engine & Business Rules (BR-03 to BR-08) |
| **SIS/24/B2/37** | **Chapa** | Doctor Console, Live Queue Control & Online Consultation Room (BR-09) |
| **SIS/24/B2/28** | **Anjana** | Manager Operations, Catalog, Session Scheduling (BR-01/02), Reception & SQL Reports |
| **SIS/24/B2/16** | **Sathya** | Real-Time STOMP WebSockets (RT-01 to RT-04), Notifications, Testing & Documentation |

*(See `FILEbelongs.md` for the complete file-by-file ownership breakdown.)*

---

## Key Features Implemented

- **4 Strict Roles (`CUSTOMER`, `DOCTOR`, `MANAGER`, `ADMIN`) + Guest Search** with server-side RBAC on REST & WebSocket frames.
- **Medical Privacy Rule:** `ADMIN` and `MANAGER` can never read consultation notes, prescriptions, reasons for visit, or chat messages.
- **AES-256-GCM Encryption at Rest:** Patient personal details, reason for visit, diagnosis, advice, prescription, and chat messages are encrypted via a JPA `AttributeConverter` with a unique random 12-byte IV per value, plus **HMAC-SHA256** blind indexes for email and NIC lookup.
- **HTTP-Only Secure JWT Cookies:** Managed by Next.js Node.js runtime Route Handlers (`/api/auth/login`, `/api/proxy/[...path]`).
- **Concurrency-Safe Atomic Numbering (BR-03):** PostgreSQL row-level pessimistic write lock (`PESSIMISTIC_WRITE`) ensures the same appointment number is never issued twice and never reused.
- **Real-Time STOMP WebSockets:** Live "Now Serving" queue (RT-01), live numbers remaining (RT-02), private encrypted doctor–patient chat with typing indicator (RT-03), and targeted user notifications (RT-04).
- **Performance:** 5-minute TTL Caffeine caching for public doctor & specialization lists with automatic cache eviction on updates, `Cache-Control: no-store` for personal data, and GZIP response compression.

---

## Repository Structure

```text
medichannel/
├── database/
│   ├── schema.sql               # 13 PostgreSQL tables, constraints, indexes
│   └── seed.sql                 # Non-encrypted catalog seed data
├── backend/                     # Spring Boot 3.4 (Java 17, Maven, Dockerfile)
├── frontend/                    # Next.js 15 App Router (JavaScript + Tailwind CSS)
├── docs/
│   ├── ERD.md                   # Entity-Relationship Diagram (Mermaid)
│   ├── GROUP_REPORT_DRAFT.md    # Complete 4,500+ word university report draft
│   └── PRESENTATION_GUIDE.md    # Slide outline & 8–10 minute demo script
├── LOCALsetup.md                # Beginner guide for running locally on PC
├── SETUPproject.md              # Step-by-step Git, Neon, Render & Vercel deployment guide
├── INSTRUCTIONtoGIT.md          # Branching, PRs & 65-commit team Git plan
├── FILEbelongs.md               # Exact file ownership per group member
└── README.md
```

---

## Required Environment Variables

### Backend (`backend/.env` / Render Environment Variables)
- `PORT` (default `8080`)
- `DB_URL` (PostgreSQL JDBC URL, e.g., Neon URL with `?sslmode=require`)
- `DB_USERNAME`
- `DB_PASSWORD`
- `FRONTEND_URL` (CORS allowlist — e.g., `http://localhost:3000` or your Vercel URL)
- `JWT_SECRET` (at least 32 random characters)
- `JWT_EXPIRATION_MS` (default `86400000`)
- `ENCRYPTION_KEY_BASE64` (Base64-encoded 32-byte AES-256 key)
- `HMAC_SECRET` (random secret string for blind index hashing)
- `INITIAL_ADMIN_EMAIL`
- `INITIAL_ADMIN_PASSWORD`
- `INITIAL_ADMIN_NAME`
- `SEED_DEMO_DATA` (`true` for first startup, then `false`)

### Frontend (`frontend/.env.local` / Vercel Environment Variables)
- `NEXT_PUBLIC_API_URL` (e.g., `http://localhost:8080` or `https://your-api.onrender.com`)
- `NEXT_PUBLIC_WS_URL` (e.g., `ws://localhost:8080/ws-native` or `wss://your-api.onrender.com/ws-native`)
- `API_BASE_URL` (same as `NEXT_PUBLIC_API_URL`)

---

## Live Deployment URLs *(Fill in after following `SETUPproject.md`)*

- **Frontend (Vercel):** `https://<your-vercel-app>.vercel.app`
- **Backend API (Render):** `https://<your-render-service>.onrender.com`
- **Health Check:** `https://<your-render-service>.onrender.com/api/health`

*(Note: Never commit test account passwords to GitHub. Submit test credentials only in the university submission form.)*
