# Bootstrapping GitHub Issues

This repository includes a script to automatically populate the issue tracker with a predefined backlog of work (labels and issues).

## Prerequisites

1. **GitHub CLI (`gh`)**: Ensure you have the GitHub CLI installed.
   - [Installation Guide](https://cli.github.com/manual/installation)
2. **Authentication**: You must be logged in to the GitHub CLI with access to this repository.
   ```bash
   gh auth login
   ```
   Run `gh auth status` to verify.

## How to Run

1. Make the script executable:
   ```bash
   chmod +x scripts/bootstrap_issues.sh
   ```

2. Run the script:
   ```bash
   ./scripts/bootstrap_issues.sh
   ```

## What it does

- **Checks your environment**: Verifies `gh` is installed and logged in.
- **Creates Labels**: Sets up standard labels for priority (`P1` - `P4`), size (`size/S` - `size/L`), types (`type/bug`, etc.), and areas (`area/ui`, etc.) with consistent colors.
- **Creates Issues**: Creates 30 distinct issues covering bugs, refactoring tasks, new features, and DevOps improvements.
  - **Idempotent**: If you run the script multiple times, it will skip creating issues that already exist (matched by exact title) and will simply update label definitions if they exist.

## Troubleshooting

- **"gh not installed"**: Install the GitHub CLI.
- **"Not logged in"**: Run `gh auth login`.
- **403 Errors**: Ensure your token has `repo` scope permissions.
