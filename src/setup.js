const { spawn } = require('child_process');
const os = require('os');
const path = require('path');

// Detect OS
const platform = os.platform();
console.log(`Detected OS: ${platform}`);

// Parse action arg: "clone", "pull", or "install"
const action = process.argv[2] || 'clone';

// Script folder path (relative to src/)
const scriptsDir = path.join(__dirname, '..', 'scripts');

// Choose command and args
let cmd = '';
let args = [];

if (platform === 'win32') {
    if (action === 'clone') {
        cmd = path.join(scriptsDir, 'clone-all.bat');
    } else if (action === 'pull') {
        cmd = path.join(scriptsDir, 'pull-all.bat');
    } else if (action === 'install') {
        cmd = path.join(scriptsDir, 'install-node-modules.bat');
    }
} else {
    cmd = 'bash';
    if (action === 'clone') {
        args = [path.join(scriptsDir, 'clone-all.sh')];
    } else if (action === 'pull') {
        args = [path.join(scriptsDir, 'pull-all.sh')];
    } else if (action === 'install') {
        args = [path.join(scriptsDir, 'install-node-modules.sh')];
    }
}

if (cmd) {
    console.log(`Running: ${cmd} ${args.join(' ')}`);
    const child = platform === 'win32'
        ? spawn(cmd, { cwd: path.join(__dirname, '..'), stdio: 'inherit', shell: true })
        : spawn(cmd, args, { cwd: path.join(__dirname, '..'), stdio: 'inherit' });

    child.on('close', (code) => {
        console.log(`Script exited with code ${code}`);
    });

    child.on('error', (err) => {
        console.error('Failed to start subprocess:', err);
    });
} else {
    console.error(`Unsupported OS: ${platform}`);
}
