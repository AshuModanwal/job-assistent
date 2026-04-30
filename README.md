🚀 Building an AI-Powered Job Intelligence Assistant

Like many developers, I applied to multiple companies across platforms — LinkedIn, Indeed, company portals…

But the real problem wasn’t applying.

👉 It was **losing visibility** after applying
👉 Not knowing where I applied
👉 Not understanding why I’m not getting responses

Everything was already in my Gmail… just unstructured.

So I started building this 👇

---

### 🧠 What I’m building

An **AI-powered system that transforms raw email data into structured job intelligence**

---

📌 **Here’s how the system looks (current MVP)**
👇
![img.png](img.png)

---

### ⚙️ Current System (MVP)

* 🔐 Google OAuth 2.0 authentication
* 📩 Gmail API integration (email ingestion)
* 🧠 Parsing engine (extract company, role, source, date)
* 🗄️ PostgreSQL for structured storage
* 🔄 Sync-based architecture (idempotent, no duplicates)

👉 Pipeline:
**Gmail → Parsing → Filtering → Structured DB → Dashboard**

---

### 🧠 AI Layer (What I’m building next)

This is where things get interesting 👇

📌 **System architecture & roadmap**
👇
![img_1.png](img_1.png)

---

I’m currently working on:

* 🧩 **Document Chunking (JD + Resume + Emails)**
* 🔗 **Embedding generation**
* 🗃️ **Vector storage (FAISS / PGVector)**
* 🔍 **Top-K semantic retrieval**
* 🤖 **LLM-based reasoning layer**

👉 Building towards a **RAG-based system** that can answer:

* “Why am I not getting callbacks?”
* “Which skills am I missing across applications?”
* “Prepare me for this role based on my history + JD”

---

### 💡 Vision

Move from:

❌ Passive job tracking

To:

🔥 **AI-driven decision system for job search**

---

### 🧱 Tech Stack

* Java + Spring Boot
* Gmail API
* PostgreSQL
* (Upcoming) LLM + Embeddings + Vector DB

---

### 📂 Full Architecture 

![img_2.png](img_2.png)

This is something I wish I had during my own job search.

Still early — but already solving a real problem.

Would love feedback or ideas 🙌

---

