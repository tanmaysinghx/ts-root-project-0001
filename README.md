# ts-root-project-0001

## 📋 Requirements

Before you begin, ensure you have:

1. **Node.js** (LTS recommended)  
2. **Git** installed and available in your system PATH  
3. **Docker Desktop** & **Docker Compose**  
4. **GitHub HTTPS access** to all repositories (no SSH keys needed)

---

## 💻 Setup Commands

### 1. Clone All Repositories (First Time)

From your **project root** folder, run:
```bash
node src/setup.js clone
```

---

### 2. Alternative: If the Above Command Doesn't Work

**Windows OS (CMD):**
```bash
cd scripts
clone-all.bat
```


**Linux based OS:**
```bash
cd scripts
bash clone-all.sh
```

**macOS:**
```bash
cd scripts
bash clone-all.sh
```


---

### 3. Pulling latest code from GitHub
> 💡 **Tip:** Please pull code everday morning once
```bash
node src/setup.js pull
```

### 4. Start All Services with Docker

From the **project root**, simply run:
-- To Build:
```bash
docker-compose build --no-cache
```

-- To Run:
```bash
docker-compose up
```