/* eslint-disable no-console */
const { spawn } = require('child_process');
const os = require('os');
const path = require('path');
const fs = require('fs');
const { Select, MultiSelect } = require('enquirer');
const { bold, cyan, green, yellow, red, magenta, gray, white } = require('kleur');
const ora = require('ora');

/* ──────────────────────────────────────────────────────────────
   Constants / Paths
────────────────────────────────────────────────────────────── */
const platform = os.platform();
const rootDir = path.join(__dirname, '..');
const scriptsDir = path.join(rootDir, 'scripts');
const runtimeDir = path.join(rootDir, '_runtime');
const pidsDir = path.join(runtimeDir, 'pids');
const statePath = path.join(runtimeDir, 'state.json');
const statusLogPath = path.join(rootDir, 'run-status.json');

ensureDir(runtimeDir);
ensureDir(pidsDir);

/* ──────────────────────────────────────────────────────────────
   Compact banner (no animation)
────────────────────────────────────────────────────────────── */
function bannerCompact() {
    const w = Math.max(56, Math.min((process.stdout.columns || 80) - 2, 100));
    const line = '─'.repeat(w);
    const title = bold().white('TS Portal');
    const tag = gray(`(${platform})`);
    const left = `${title} ${tag}`;
    const right = gray(new Date().toISOString().replace('T', ' ').slice(0, 19));
    const pad = Math.max(0, w - stripAnsi(left).length - stripAnsi(right).length);
    console.log('\n' + line);
    console.log(left + ' '.repeat(pad) + right);
    console.log(gray('Author: ') + white('Tanmay Singh ') + gray('<') + white('tanmaysinghx@gmail.com') + gray('>'));
    console.log(gray('Root:   ') + white(rootDir));
    console.log(line + '\n');
}
function stripAnsi(s) { return s.replace(/\x1B\[[0-?]*[ -/]*[@-~]/g, ''); }

/* ──────────────────────────────────────────────────────────────
   Tiny utils
────────────────────────────────────────────────────────────── */
function ensureDir(d) { if (!fs.existsSync(d)) fs.mkdirSync(d, { recursive: true }); }
function isDir(p) { try { return fs.existsSync(p) && fs.statSync(p).isDirectory(); } catch { return false; } }
function exists(p) { try { return fs.existsSync(p); } catch { return false; } }
function safeReadJSON(p) { try { return JSON.parse(fs.readFileSync(p, 'utf-8')); } catch { return null; } }
function pad(t, n) { return (t + ' '.repeat(n)).slice(0, Math.max(n, t.length)); }
function todayISO() { return new Date().toISOString().slice(0, 10); }
function saveState(patch) {
    const prev = safeReadJSON(statePath) || {};
    const next = { ...prev, ...patch };
    fs.writeFileSync(statePath, JSON.stringify(next, null, 2));
    return next;
}
function getState() { return safeReadJSON(statePath) || {}; }

/* ──────────────────────────────────────────────────────────────
   Status tracking (upsert) + Summary
────────────────────────────────────────────────────────────── */
const statusMap = new Map(); // service -> { service, type, status, reason }
function setStatus(service, type, status, reason = '') { statusMap.set(service, { service, type, status, reason }); }
function rows() { return Array.from(statusMap.values()); }

function printSummary(title = 'SERVICE SUMMARY') {
    const data = rows();
    if (data.length === 0) return;

    console.log('\n' + bold().underline(title));
    console.log(pad(bold('Service'), 28), pad(bold('Type'), 14), pad(bold('Status'), 12), bold('Info'));
    console.log(gray('-'.repeat(80)));

    for (const s of data) {
        const sym =
            s.status === 'OK' ? green('✅ OK') :
                s.status === 'SKIP' ? yellow('⏭ SKIP') :
                    s.status === 'LAUNCHED' ? cyan('🚀 LAUNCHED') :
                        s.status === 'PENDING' ? cyan('… PENDING') :
                            red('❌ FAIL');
        console.log(pad(s.service, 28), pad(s.type, 14), pad(sym, 12), s.reason || '');
    }

    const ok = data.filter(x => x.status === 'OK').length;
    const fail = data.filter(x => x.status === 'FAIL').length;
    const skip = data.filter(x => x.status === 'SKIP').length;
    const launched = data.filter(x => x.status === 'LAUNCHED').length;

    console.log(`\n${green(`${ok} succeeded`)}, ${red(`${fail} failed`)}, ${yellow(`${skip} skipped`)}, ${cyan(`${launched} launched`)}.`);
    fs.writeFileSync(statusLogPath, JSON.stringify(data, null, 2));
    console.log(gray(`Saved: ${statusLogPath}`));
    console.log(`\nBuilt with ❤️ by Tanmay Singh <tanmaysinghx@gmail.com>\n`);
}

/* ──────────────────────────────────────────────────────────────
   Exec helpers (spinners + readiness)
────────────────────────────────────────────────────────────── */
function execWait(command, args, cwd) {
    return new Promise((resolve) => {
        const child = spawn(command, args, { cwd, shell: true, stdio: 'inherit' });
        child.on('close', (code) => resolve(code));
        child.on('error', () => resolve(1));
    });
}

async function runWrapper(step, label) {
    const isWin = platform === 'win32';
    const cmd = isWin ? path.join(scriptsDir, `${step}-all.bat`) : 'bash';
    const args = isWin ? [] : [path.join(scriptsDir, `${step}-all.sh`)];
    const spinner = ora({ text: label, color: 'cyan' }).start();

    if (!exists(isWin ? cmd : args[0])) {
        spinner.stop();
        console.log(yellow(`⏭ ${label} skipped (no ${step}-all script found)`));
        return 0;
    }

    const code = await execWait(cmd, args, rootDir);
    if (code === 0) spinner.succeed(`${label} ${green('OK')}`);
    else spinner.fail(`${label} ${red('FAIL')} (code ${code})`);
    return code;
}

/* Readiness-aware long-runner */
const procRegistry = new Map(); // service -> { child, pidPath }
function writePid(service, pid, meta) {
    const pidPath = path.join(pidsDir, `${service}.pid.json`);
    fs.writeFileSync(pidPath, JSON.stringify({ pid, platform, startedAt: new Date().toISOString(), ...meta }, null, 2));
    return pidPath;
}
function readPid(service) {
    const pidPath = path.join(pidsDir, `${service}.pid.json`);
    if (!exists(pidPath)) return null;
    try { return { pidPath, data: JSON.parse(fs.readFileSync(pidPath, 'utf-8')) }; }
    catch { return { pidPath, data: null }; }
}
function deletePid(service) {
    const pidPath = path.join(pidsDir, `${service}.pid.json`);
    if (exists(pidPath)) fs.unlinkSync(pidPath);
}
function listPidFiles() {
    if (!exists(pidsDir)) return [];
    return fs.readdirSync(pidsDir).filter(f => f.endsWith('.pid.json')).map(f => f.replace('.pid.json', ''));
}

function runCommand(service, command, args, cwd, type) {
    setStatus(service, type, 'PENDING');

    const child = spawn(command, args, { cwd, shell: true, stdio: ['inherit', 'pipe', 'pipe'] });
    const pidPath = writePid(service, child.pid, { command, args, cwd, type });
    procRegistry.set(service, { child, pidPath });

    const readyMatchers = [
        /listening/i, /server is running/i, /started on/i, /ready in/i, /http server started/i,
        /compiled successfully/i, /local:\s*http:\/\//i, /Started .* in .* seconds/i, /Tomcat started on port/i
    ];

    let ready = false;
    const mark = (reason) => { if (!ready) { setStatus(service, type, 'OK', reason || ''); ready = true; } };
    const onData = (buf) => {
        const t = buf.toString(); process.stdout.write(t);
        if (!ready) for (const rx of readyMatchers) if (rx.test(t)) { mark(rx.source); break; }
    };

    child.stdout && child.stdout.on('data', onData);
    child.stderr && child.stderr.on('data', onData);

    const timer = setTimeout(() => {
        if (!ready) setStatus(service, type, 'LAUNCHED', 'no readiness signal; process alive');
    }, 7000);

    child.on('error', (e) => { clearTimeout(timer); setStatus(service, type, 'FAIL', e.message); deletePid(service); });
    child.on('close', (code) => {
        clearTimeout(timer);
        if (!ready && code === 0) setStatus(service, type, 'FAIL', 'exited before readiness');
        else if (code !== 0) setStatus(service, type, 'FAIL', `exit ${code}`);
        deletePid(service); procRegistry.delete(service);
    });
}

function enumerateRunning() {
    const names = new Set(procRegistry.keys());
    const list = [];
    for (const [service, { child }] of procRegistry.entries()) {
        const meta = readPid(service)?.data || {};
        list.push({ service, pid: child.pid, type: meta.type || '' });
    }
    for (const svc of listPidFiles()) {
        if (names.has(svc)) continue;
        const meta = readPid(svc)?.data || {};
        if (meta?.pid) list.push({ service: svc, pid: meta.pid, type: meta.type || '' });
    }
    return list;
}
function killPid(pid) {
    return new Promise((resolve) => {
        if (platform === 'win32') {
            const p = spawn('taskkill', ['/PID', String(pid), '/T', '/F'], { stdio: 'ignore' });
            p.on('close', (c) => resolve(c === 0)); p.on('error', () => resolve(false));
        } else {
            try {
                process.kill(pid, 'SIGTERM');
                setTimeout(() => {
                    try { process.kill(pid, 0); process.kill(pid, 'SIGKILL'); } catch { }
                    resolve(true);
                }, 500);
            } catch {
                resolve(false);
            }
        }
    });
}
async function stopService(service) {
    const running = enumerateRunning().find(x => x.service === service);
    if (!running) return false;
    const ok = await killPid(running.pid);
    if (ok) { deletePid(service); setStatus(service, running.type || 'ANY', 'SKIP', 'stopped'); }
    else setStatus(service, running.type || 'ANY', 'FAIL', 'stop failed');
    return ok;
}

/* ──────────────────────────────────────────────────────────────
   Discovery + Apps list
────────────────────────────────────────────────────────────── */
function detectServices() {
    const out = [];
    const scan = (base, scope) => {
        if (!exists(base)) return;
        for (const name of fs.readdirSync(base)) {
            const svcPath = path.join(base, name);
            if (!isDir(svcPath)) continue;
            const pkgPath = path.join(svcPath, 'package.json');
            const isNg = exists(path.join(svcPath, 'angular.json'));
            if (exists(pkgPath)) {
                out.push({
                    id: `${scope}/${name}`, label: `[${scope}] ${name}`,
                    name, scope, path: svcPath, kind: isNg ? 'Angular' : 'Node',
                    start: () => {
                        const pkg = safeReadJSON(pkgPath) || {};
                        const script = isNg ? 'start' : (pkg.scripts?.dev ? 'run dev' : 'start');
                        runCommand(name, 'npm', [script], svcPath, isNg ? 'Angular' : 'Node');
                    },
                    install: () => execWait('npm', ['install'], svcPath)
                });
                continue;
            }
            // Spring Boot markers
            const mvnw = path.join(svcPath, platform === 'win32' ? 'mvnw.cmd' : 'mvnw');
            const gradlew = path.join(svcPath, platform === 'win32' ? 'gradlew.bat' : 'gradlew');
            const pom = path.join(svcPath, 'pom.xml');
            if (exists(mvnw) || exists(gradlew) || exists(pom)) {
                out.push({
                    id: `${scope}/${name}`, label: `[${scope}] ${name}`,
                    name, scope, path: svcPath, kind: 'SpringBoot',
                    start: () => {
                        if (exists(mvnw)) runCommand(name, mvnw, ['spring-boot:run'], svcPath, 'SpringBoot');
                        else if (exists(gradlew)) runCommand(name, gradlew, ['bootRun'], svcPath, 'SpringBoot');
                        else runCommand(name, 'mvn', ['spring-boot:run'], svcPath, 'SpringBoot');
                    },
                    install: null
                });
            }
        }
    };
    scan(path.join(rootDir, 'backend'), 'backend');
    scan(path.join(rootDir, 'frontend'), 'frontend');
    return out.sort((a, b) => a.scope.localeCompare(b.scope) || a.name.localeCompare(b.name));
}

function printAppsList(catalog) {
    console.log('\n' + bold().underline('Applications available'));
    if (!catalog.length) { console.log(yellow('No apps detected under ./backend or ./frontend')); return; }
    catalog.forEach((s, i) => {
        const tag = s.kind === 'Angular' ? yellow('Angular') : s.kind === 'Node' ? green('Node') : cyan('SpringBoot');
        console.log(`${String(i + 1).padStart(2, '0')}. ${bold(s.label)}  ${gray('—')} ${tag}`);
    });
}

/* ──────────────────────────────────────────────────────────────
   Repo map for "Clone Specific" (optional)
────────────────────────────────────────────────────────────── */
function readRepoMap() {
    const p = path.join(scriptsDir, 'repos.json');
    return safeReadJSON(p) || {};
}
async function cloneSpecificInteractive() {
    const map = readRepoMap();
    const keys = Object.keys(map);
    if (keys.length === 0) {
        console.log(yellow('⏭ Clone Specific unavailable (scripts/repos.json not found or empty).'));
        return;
    }
    const choices = keys.map((k, i) => ({ name: k, message: `${String(i + 1).padStart(2, '0')}  ${k}` }));
    const pick = await new MultiSelect({
        name: 'repos',
        message: bold('Select repositories to CLONE') + gray('  (space to toggle, enter to confirm)'),
        limit: 12,
        choices,
        indicator(state, choice) { return choice.enabled ? green('●') : gray('○'); }
    }).run();
    if (!pick.length) { console.log(gray('No selection.')); return; }

    for (const key of pick) {
        const url = map[key];
        const target = path.join(rootDir, key);
        if (exists(target) && isDir(target) && exists(path.join(target, '.git'))) {
            console.log(yellow(`⏭ Skipping ${key} (already cloned)`));
            continue;
        }
        ensureDir(path.dirname(target));
        const spinner = ora({ text: `Cloning ${key}`, color: 'cyan' }).start();
        const code = await execWait('git', ['clone', url, target], rootDir);
        code === 0 ? spinner.succeed(`Cloned ${key}`) : spinner.fail(`Clone failed for ${key}`);
    }
}

/* ──────────────────────────────────────────────────────────────
   Pull All / Specific with "today" reminder
────────────────────────────────────────────────────────────── */
async function pullAllDetected() {
    const targets = [];
    for (const scope of ['backend', 'frontend']) {
        const base = path.join(rootDir, scope);
        if (!exists(base)) continue;
        for (const name of fs.readdirSync(base)) {
            const p = path.join(base, name);
            if (isDir(p) && exists(path.join(p, '.git'))) targets.push(p);
        }
    }
    if (!targets.length) { console.log(yellow('⏭ No git repos detected to pull.')); return; }

    const spinner = ora({ text: `Pulling ${targets.length} repos…`, color: 'cyan' }).start();
    for (const dir of targets) {
        spinner.text = `Pulling ${path.relative(rootDir, dir)}`;
        await execWait('git', ['pull'], dir);
    }
    spinner.succeed(`Pull complete for ${targets.length} repos`);
    saveState({ lastPullISO: todayISO() });
}
async function pullSpecificInteractive() {
    const targets = [];
    for (const scope of ['backend', 'frontend']) {
        theBase = path.join(rootDir, scope);
        var theBase = path.join(rootDir, scope); // guard for older shells
    }
    // re-scan
    for (const scope of ['backend', 'frontend']) {
        const base = path.join(rootDir, scope);
        if (!exists(base)) continue;
        for (const name of fs.readdirSync(base)) {
            const p = path.join(base, name);
            if (isDir(p) && exists(path.join(p, '.git'))) targets.push({ key: `${scope}/${name}`, dir: p });
        }
    }
    if (!targets.length) { console.log(yellow('⏭ No git repos detected.')); return; }

    const choices = targets.map((t, i) => ({ name: t.key, message: `${String(i + 1).padStart(2, '0')}  ${t.key}` }));
    const pick = await new MultiSelect({
        name: 'pull',
        message: bold('Select repositories to PULL') + gray('  (space to toggle, enter to confirm)'),
        limit: 12,
        choices,
        indicator(state, choice) { return choice.enabled ? green('●') : gray('○'); }
    }).run();
    if (!pick.length) { console.log(gray('No selection.')); return; }

    const spinner = ora({ text: `Pulling ${pick.length} repos…`, color: 'cyan' }).start();
    for (const key of pick) {
        const dir = targets.find(t => t.key === key).dir;
        spinner.text = `Pulling ${key}`;
        await execWait('git', ['pull'], dir);
    }
    spinner.succeed(`Pull complete for ${pick.length} repos`);
    saveState({ lastPullISO: todayISO() });
}

/* ──────────────────────────────────────────────────────────────
   Install All / Specific
────────────────────────────────────────────────────────────── */
async function installAll(catalog) {
    const installables = catalog.filter(c => c.install);
    if (!installables.length) { console.log(yellow('⏭ No Node/Angular services to install.')); return; }

    const spinner = ora({ text: `Installing ${installables.length} services…`, color: 'cyan' }).start();
    for (const s of installables) {
        spinner.text = `Installing ${s.scope}/${s.name}`;
        await s.install();
    }
    spinner.succeed(`Install complete for ${installables.length} services`);
}
async function installSpecific(catalog) {
    const installables = catalog.filter(c => c.install);
    if (!installables.length) { console.log(yellow('⏭ No Node/Angular services to install.')); return; }

    const choices = installables.map((s, i) => ({
        name: s.id, message: `${String(i + 1).padStart(2, '0')}  ${bold(`[${s.scope}] ${s.name}`)}`
    }));
    const pick = await new MultiSelect({
        name: 'inst',
        message: bold('Select services to INSTALL (npm i)') + gray('  (space to toggle, enter to confirm)'),
        limit: 12,
        choices,
        indicator(state, choice) { return choice.enabled ? green('●') : gray('○'); }
    }).run();
    if (!pick.length) { console.log(gray('No selection.')); return; }

    const spinner = ora({ text: `Installing ${pick.length} services…`, color: 'cyan' }).start();
    for (const id of pick) {
        const s = installables.find(x => x.id === id);
        spinner.text = `Installing ${s.scope}/${s.name}`;
        await s.install();
    }
    spinner.succeed(`Install complete for ${pick.length} services`);
}

/* ──────────────────────────────────────────────────────────────
   Run All / Specific with "pull today?" reminder
────────────────────────────────────────────────────────────── */
async function runServicesInteractive(catalog) {
    const st = getState();
    if (st.lastPullISO !== todayISO()) {
        const doPull = await new Select({
            name: 'rem',
            message: bold('It looks like you have not pulled today. Pull latest now?'),
            choices: [{ name: 'yes', message: 'Yes, pull all and continue' }, { name: 'no', message: 'No, continue without pull' }]
        }).run();
        if (doPull === 'yes') await pullAllDetected();
    }

    const mode = await new Select({
        name: 'runMode',
        message: bold('Run mode'),
        choices: [{ name: 'all', message: 'Run ALL services' }, { name: 'pick', message: 'Pick specific services' }, { name: 'back', message: 'Back' }]
    }).run();
    if (mode === 'back') return;

    let selected = catalog;
    if (mode === 'pick') {
        const picks = await new MultiSelect({
            name: 'services',
            message: bold('Select services to RUN') + gray('  (space to toggle, enter to confirm)'),
            limit: 12,
            choices: catalog.map((s, i) => ({
                name: s.id,
                message: `${String(i + 1).padStart(2, '0')}  ${bold(`[${s.scope}] ${s.name}`)}  ${gray('—')} ${s.kind === 'Angular' ? yellow('Angular') : s.kind === 'Node' ? green('Node') : cyan('SpringBoot')
                    }`
            })),
            indicator(state, choice) { return choice.enabled ? green('●') : gray('○'); }
        }).run();

        const set = new Set(picks);
        selected = catalog.filter(s => set.has(s.id));
        for (const s of catalog) if (!set.has(s.id)) setStatus(s.name, s.kind, 'SKIP', 'Not selected');

        if (!selected.length) { console.log(yellow('No services selected.')); return; }
    }

    console.log('\n' + bold('Launching selected services…') + '\n');
    for (const s of selected) s.start();
    setTimeout(() => printSummary(), 7000);
}

/* ──────────────────────────────────────────────────────────────
   Main Interactive "GO" Flow
────────────────────────────────────────────────────────────── */
async function goFlow() {
    bannerCompact();

    const catalog = detectServices();
    printAppsList(catalog);

    const top = await new Select({
        name: 'menu',
        message: bold('Choose an action'),
        choices: [
            { name: 'wizard', message: '🚀 GO (guided): Clone / Pull / Install / Run' },
            { name: 'clone', message: '⬇️  Clone (All / Specific)' },
            { name: 'pull', message: '🔄 Pull (All / Specific)' },
            { name: 'install', message: '📦 Install (All / Specific)' },
            { name: 'run', message: '▶️  Run services (All / Specific)' },
            { name: 'stop', message: '⏹  Stop running services' },
            { name: 'restart', message: '🔁 Restart services' },
            { name: 'help', message: '❓ Help' },
            { name: 'exit', message: 'Exit' }
        ]
    }).run();

    if (top === 'exit') return;
    if (top === 'help') { printHelp(); return; }

    if (top === 'wizard') {
        const steps = await new MultiSelect({
            name: 'steps',
            message: bold('Select steps to execute') + gray('  (space to toggle, enter to confirm)'),
            limit: 8,
            initial: ['clone', 'pull', 'install', 'run'],
            choices: [
                { name: 'clone', message: 'Clone repositories' },
                { name: 'pull', message: 'Pull latest changes' },
                { name: 'install', message: 'Install node modules' },
                { name: 'run', message: 'Run services' }
            ],
            indicator(state, choice) { return choice.enabled ? green('●') : gray('○'); }
        }).run();

        if (steps.includes('clone')) {
            const mode = await new Select({
                name: 'cMode', message: bold('Clone mode'), choices: [
                    { name: 'all', message: 'Clone ALL (via scripts/clone-all.* if present)' },
                    { name: 'pick', message: 'Clone Specific (via scripts/repos.json)' }
                ]
            }).run();
            if (mode === 'all') await runWrapper('clone', 'Cloning repositories');
            else await cloneSpecificInteractive();
        }

        if (steps.includes('pull')) {
            const mode = await new Select({
                name: 'pMode', message: bold('Pull mode'), choices: [
                    { name: 'all', message: 'Pull ALL detected repos' },
                    { name: 'pick', message: 'Pull Specific repos' }
                ]
            }).run();
            if (mode === 'all') await pullAllDetected();
            else await pullSpecificInteractive();
        }

        if (steps.includes('install')) {
            const mode = await new Select({
                name: 'iMode', message: bold('Install mode'), choices: [
                    { name: 'all', message: 'Install ALL Node/Angular services' },
                    { name: 'pick', message: 'Install Specific services' }
                ]
            }).run();
            if (mode === 'all') await installAll(catalog);
            else await installSpecific(catalog);
        }

        if (steps.includes('run')) await runServicesInteractive(catalog);
        return;
    }

    if (top === 'clone') {
        const mode = await new Select({
            name: 'cMode', message: bold('Clone mode'), choices: [
                { name: 'all', message: 'Clone ALL (via scripts/clone-all.* if present)' },
                { name: 'pick', message: 'Clone Specific (via scripts/repos.json)' }
            ]
        }).run();
        if (mode === 'all') await runWrapper('clone', 'Cloning repositories');
        else await cloneSpecificInteractive();
        return;
    }

    if (top === 'pull') {
        const mode = await new Select({
            name: 'pMode', message: bold('Pull mode'), choices: [
                { name: 'all', message: 'Pull ALL detected repos' },
                { name: 'pick', message: 'Pull Specific repos' }
            ]
        }).run();
        if (mode === 'all') await pullAllDetected();
        else await pullSpecificInteractive();
        return;
    }

    if (top === 'install') {
        const mode = await new Select({
            name: 'iMode', message: bold('Install mode'), choices: [
                { name: 'all', message: 'Install ALL Node/Angular services' },
                { name: 'pick', message: 'Install Specific services' }
            ]
        }).run();
        if (mode === 'all') await installAll(catalog);
        else await installSpecific(catalog);
        return;
    }

    if (top === 'run') { await runServicesInteractive(catalog); return; }

    if (top === 'stop') {
        const running = enumerateRunning();
        if (!running.length) { console.log(yellow('No tracked PIDs found.')); return; }
        const pick = await new MultiSelect({
            name: 'toStop',
            message: bold('Select services to STOP') + gray('  (space to toggle, enter to confirm)'),
            limit: 12,
            choices: running.map((r, i) => ({
                name: r.service,
                message: `${String(i + 1).padStart(2, '0')}  ${bold(r.service)}  ${gray(`PID:${r.pid}`)}  ${gray(r.type)}`
            }))
        }).run();
        if (!pick.length) { console.log(gray('No selection.')); return; }
        for (const s of pick) console.log((await stopService(s)) ? green(`Stopped ${s}`) : red(`Failed to stop ${s}`));
        setTimeout(() => printSummary('STOP SUMMARY'), 500);
        return;
    }

    if (top === 'restart') {
        const running = enumerateRunning();
        const byName = new Map(catalog.map(c => [c.name, c]));
        const all = Array.from(new Set([...catalog.map(c => c.name), ...running.map(r => r.service)]));
        const pick = await new MultiSelect({
            name: 'toRestart',
            message: bold('Select services to RESTART') + gray('  (space to toggle, enter to confirm)'),
            limit: 12,
            choices: all.map((name, i) => {
                const r = running.find(x => x.service === name);
                const tag = r ? green('RUNNING') : gray('OFF');
                const pidTxt = r ? gray(`PID:${r.pid}`) : '';
                return { name, message: `${String(i + 1).padStart(2, '0')}  ${bold(name)}  ${tag} ${pidTxt}` };
            })
        }).run();
        if (!pick.length) { console.log(gray('No selection.')); return; }
        for (const s of pick) await stopService(s);
        for (const s of pick) { const e = byName.get(s); if (e) { console.log(cyan(`Restarting ${s}…`)); e.start(); } }
        setTimeout(() => printSummary('RESTART SUMMARY'), 7000);
        return;
    }
}

/* ──────────────────────────────────────────────────────────────
   Direct commands + Help
────────────────────────────────────────────────────────────── */
function printHelp() {
    console.log(
        '\n' + bold().underline('TS Portal – Help') + '\n' +
        `  ${cyan('node src/setup.js go')}        → Welcome + App list + menu (Clone / Pull / Install / Run / Stop / Restart)\n` +
        `  ${cyan('node src/setup.js help')}      → This help\n\n` +
        bold('Direct steps:') + '\n' +
        `  ${cyan('node src/setup.js clone all')}         ${gray('→ scripts/clone-all.* if present')}\n` +
        `  ${cyan('node src/setup.js clone pick')}        ${gray('→ uses scripts/repos.json')}\n` +
        `  ${cyan('node src/setup.js pull all')}          ${gray('→ pull all detected git repos')}\n` +
        `  ${cyan('node src/setup.js pull pick')}         ${gray('→ select repos to pull')}\n` +
        `  ${cyan('node src/setup.js install all')}       ${gray('→ npm i for all Node/Angular')}\n` +
        `  ${cyan('node src/setup.js install pick')}      ${gray('→ select services to install')}\n` +
        `  ${cyan('node src/setup.js run')}               ${gray('→ run services (all/pick, with pull-today reminder)')}\n` +
        `  ${cyan('node src/setup.js stop')}              ${gray('→ interactive stop')}\n` +
        `  ${cyan('node src/setup.js restart')}           ${gray('→ interactive restart')}\n`
    );
}

/* Entry */
(async function main() {
    const [cmd = 'go', sub = ''] = (process.argv.slice(2));
    if (cmd === 'help' || cmd === '--help' || cmd === '-h') { printHelp(); return; }

    if (cmd === 'go') { await goFlow(); return; }

    // direct commands
    if (cmd === 'clone') {
        bannerCompact();
        if (sub === 'all') await runWrapper('clone', 'Cloning repositories');
        else await cloneSpecificInteractive();
        return;
    }
    if (cmd === 'pull') {
        bannerCompact();
        if (sub === 'all') await pullAllDetected();
        else await pullSpecificInteractive();
        return;
    }
    if (cmd === 'install') {
        bannerCompact();
        const catalog = detectServices();
        if (sub === 'all') await installAll(catalog);
        else await installSpecific(catalog);
        return;
    }
    if (cmd === 'run') {
        bannerCompact();
        const catalog = detectServices();
        await runServicesInteractive(catalog);
        return;
    }
    if (cmd === 'stop') {
        bannerCompact();
        const running = enumerateRunning();
        if (!running.length) { console.log(yellow('No tracked PIDs found.')); return; }
        const pick = await new MultiSelect({
            name: 'toStop',
            message: bold('Select services to STOP') + gray('  (space to toggle, enter to confirm)'),
            limit: 12,
            choices: running.map((r, i) => ({
                name: r.service,
                message: `${String(i + 1).padStart(2, '0')}  ${bold(r.service)}  ${gray(`PID:${r.pid}`)}  ${gray(r.type)}`
            }))
        }).run();
        if (!pick.length) return;
        for (const s of pick) console.log((await stopService(s)) ? green(`Stopped ${s}`) : red(`Failed to stop ${s}`));
        setTimeout(() => printSummary('STOP SUMMARY'), 500);
        return;
    }
    if (cmd === 'restart') {
        bannerCompact();
        const catalog = detectServices();
        const running = enumerateRunning();
        const all = Array.from(new Set([...catalog.map(c => c.name), ...running.map(r => r.service)]));
        const byName = new Map(catalog.map(c => [c.name, c]));
        const pick = await new MultiSelect({
            name: 'toRestart',
            message: bold('Select services to RESTART') + gray('  (space to toggle, enter to confirm)'),
            limit: 12,
            choices: all.map((name, i) => {
                const r = running.find(x => x.service === name);
                const tag = r ? green('RUNNING') : gray('OFF');
                const pidTxt = r ? gray(`PID:${r.pid}`) : '';
                return { name, message: `${String(i + 1).padStart(2, '0')}  ${bold(name)}  ${tag} ${pidTxt}` };
            })
        }).run();
        if (!pick.length) return;
        for (const s of pick) await stopService(s);
        for (const s of pick) { const e = byName.get(s); if (e) { console.log(cyan(`Restarting ${s}…`)); e.start(); } }
        setTimeout(() => printSummary('RESTART SUMMARY'), 7000);
        return;
    }

    // fallback
    printHelp();
})();