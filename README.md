# 🚀 TS Root Project 0001

Your unified developer environment for all **TS microservices and frontends**, designed to automate cloning, setup, environment updates, and orchestration of all local services via a single **global command interface**.

---

## ⚙️ Global Command Overview

The system supports **two execution environments** — run all apps either **via Docker** or **directly using local CLI commands**.

### 🐳 Run Using Docker
Ideal for complete containerized workflows.
```bash
node src/setup.js go --docker
```
Or directly via Docker Compose:
```bash
docker-compose up --build
```

### 💻 Run Using Command Line (Local Mode)
Runs Node.js, Angular, and Spring Boot services directly on your local system.
```bash
node src/setup.js go --local
```
Or start specific workflows manually:
```bash
node src/setup.js run
```

> 💡 Before starting either mode, ensure your `.env` file is properly configured and synced.

Each application inherits configuration from the root `.env`.  
This `.env` must be updated for **all backend and frontend services** before launching.

---

## 📦 Prerequisites

Before you begin, make sure the following tools are installed on your system:

| Tool | Description | Recommended Version |
|------|--------------|----------------------|
| 🟢 **Node.js** | Runtime environment | LTS (≥ 18.x) |
| 🧰 **Git** | Source control | Latest |
| 🐳 **Docker Desktop** | Container runtime | Latest stable |
| ⚙️ **Docker Compose** | Service orchestration | v2.x or newer |

> 💡 Ensure **GitHub HTTPS access** is available (SSH keys are optional).

---

## 🧭 Quick Start Guide

### 1️⃣ Clone All Repositories (First Time Setup)

From your **project root**, run:
```bash
node src/setup.js clone
```

This command automatically clones all repositories defined in `scripts/repos.json`  
(or uses `scripts/clone-all.sh` / `.bat` if available).

---

### 2️⃣ Alternative Manual Clone (if above fails)

#### 🪟 **Windows (CMD)**
```bash
cd scripts
clone-all.bat
```

#### 🐧 **Linux**
```bash
cd scripts
bash clone-all.sh
```

#### 🍎 **macOS**
```bash
cd scripts
bash clone-all.sh
```

---

### 3️⃣ Pull Latest Code Daily

Keep your local repositories updated:
```bash
node src/setup.js pull
```
> 💡 **Tip:** Run this once every morning before starting work.

---

### 4️⃣ Update Environment Variables

Each service uses the root `.env` file for environment configuration.
To propagate the latest variables across all apps:
```bash
node src/setup.js env-update
```
This ensures every microservice and frontend has an up-to-date `.env`.

---

### 5️⃣ Install Node Dependencies

Temporary step (will be automated soon):
```bash
node src/setup.js install
```

This installs dependencies across all Node.js and Angular services.

---

### 6️⃣ Start All Services

Choose your preferred environment:

#### ▶️ **Run via Docker Compose**
```bash
docker-compose up
```

#### ⚙️ **Run Locally (CLI)**
```bash
node src/setup.js run
```

Your full environment will boot up with all backend and frontend services.

---

## ⚡ Pro CLI (Interactive Console)

You can manage everything via the **interactive console**:
```bash
node src/setup.js go
```

This provides a guided menu to:
- Choose between **Docker or Local** runtime modes
- Clone / Pull / Install / Run / Stop / Restart services
- Automatically sync `.env` across all applications
- Run individual or all apps
- Stop or restart multiple services interactively
- View detailed service status reports

---

## 🧰 Common Commands

| Action | Command |
|--------|----------|
| 🧬 Clone all or specific repos | `node src/setup.js clone` |
| 🔄 Pull latest changes | `node src/setup.js pull` |
| ⚙️ Update all .env files | `node src/setup.js env-update` |
| 📦 Install dependencies | `node src/setup.js install` |
| ▶️ Run selected or all services | `node src/setup.js run` |
| ⏹ Stop one or more services | `node src/setup.js stop` |
| 🔁 Restart services | `node src/setup.js restart` |
| ❓ Help menu | `node src/setup.js help` |

---

## 🧑‍💻 Author

**Tanmay Singh**  
*Full Stack Developer*  
📧 [tanmaysinghx@gmail.com](mailto:tanmaysinghx@gmail.com)

---