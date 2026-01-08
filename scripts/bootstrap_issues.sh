#!/bin/bash
set -euo pipefail

# Unset GITHUB_TOKEN to force using the local gh auth credentials 
# instead of the environment's limited token.
unset GITHUB_TOKEN

# 1. Verify gh is installed and authenticated
if ! command -v gh &> /dev/null; then
    echo "Error: GitHub CLI (gh) is not installed."
    exit 1
fi

if ! gh auth status &> /dev/null; then
    echo "Error: You are not logged into GitHub CLI. Run 'gh auth login'."
    exit 1
fi

# 2. Determine current repo
# Try to get it from git origin first
REMOTE_URL=$(git config --get remote.origin.url || true)

if [[ -n "$REMOTE_URL" ]]; then
    # Extract owner/repo from URL (supports https and ssh)
    # Removes .git suffix if present
    REPO=$(echo "$REMOTE_URL" | sed -E 's/.*github\.com[:/](.+)(\.git)?/\1/' | sed 's/\.git$//')
else
    # Fallback to gh view if no git remote found
    REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
fi

echo "Bootstrapping issues for repository: $REPO"

# Helper functions
ensure_label() {
    local name="$1"
    local color="$2"
    local desc="$3"
    
    echo "Ensuring label '$name'..."
    # Try to create first. If it fails (likely due to existence), try to edit.
    # We use --force on create just in case, though it usually errors if exists.
    if ! gh label create "$name" --repo "$REPO" --color "$color" --description "$desc" --force 2>/dev/null; then
        gh label edit "$name" --repo "$REPO" --color "$color" --description "$desc" >/dev/null
    fi
}

issue_exists() {
    local title="$1"
    # Check if any issue returned by search matches the title exactly
    gh issue list --repo "$REPO" --limit 100 --search "in:title \"$title\"" --json title -q '.[].title' | grep -Fqx "$title"
}

create_issue() {
    local title="$1"
    local labels="$2"
    local body="$3"

    if issue_exists "$title"; then
        echo "Skipping existing issue: $title"
    else
        echo "Creating issue: $title"
        gh issue create --repo "$REPO" --title "$title" --body "$body" --label "$labels" >/dev/null
    fi
}

# 3. Create Labels
echo "--------------------------------------------------------"
echo "Creating labels..."
echo "--------------------------------------------------------"

ensure_label "P1" "b60205" "Priority 1: Urgent / High Value"
ensure_label "P2" "d93f0b" "Priority 2: Important"
ensure_label "P3" "0e8a16" "Priority 3: Normal"
ensure_label "P4" "c5def5" "Priority 4: Low / Nice to have"

ensure_label "size/S" "0075ca" "Small effort"
ensure_label "size/M" "0075ca" "Medium effort"
ensure_label "size/L" "0075ca" "Large effort"

ensure_label "type/bug" "d73a4a" "Something is broken"
ensure_label "type/refactor" "a2eeef" "Code improvement, no feature change"
ensure_label "type/feature" "a2eeef" "New functionality"
ensure_label "type/tests" "c5def5" "Adding or fixing tests"
ensure_label "type/devops" "006b75" "CI/CD, Docker, Scripts"

ensure_label "area/ui" "bfdadc" "Frontend, HTML, CSS, JS"
ensure_label "area/security" "e99695" "Security, Auth, Permissions"
ensure_label "area/data" "c2e0c6" "Database, Models, Migrations"

# 4. Create Issues
echo "--------------------------------------------------------"
echo "Creating issues..."
echo "--------------------------------------------------------"

# 1
BODY=$(cat <<EOF
Problem: Controller has \`@GetMapping("/dashboard")\` but no \`dashboard.html\` template.

Acceptance criteria
- Visiting \`/dashboard\` while logged in returns a valid page (either create \`dashboard.html\` or remove/redirect route).
- Add at least one MVC test proving the behavior.

Notes
- If you keep it: reuse model attributes already computed in controller.
EOF
)
create_issue "[P1][S] Fix \`/dashboard\` route (missing view)" "P1,size/S,type/bug,area/ui" "$BODY"

# 2
BODY=$(cat <<EOF
Problem: Security config permits \`/css/**\` and \`/js/**\`, but the repo uses \`src/main/resources/static/style.css\` (served at \`/style.css\`).

Acceptance criteria
- Landing + login pages load CSS without authentication.
- Add a test (or document manual verification) showing unauthenticated access to the CSS path.

Hint
- Update \`requestMatchers(...)\` to permit the actual static paths used.
EOF
)
create_issue "[P1][S] Make static assets accessible on landing/login pages" "P1,size/S,type/bug,area/security" "$BODY"

# 3
BODY=$(cat <<EOF
Acceptance criteria
- Home page shows newest-first ordering for both transactions and incomes.
- Use repository methods (e.g., \`findByUserIdOrderByDateDesc\`) instead of sorting in the view.
- Add a unit test or repository test for ordering.
EOF
)
create_issue "[P1][S] Sort transactions and incomes by date (newest first)" "P1,size/S,type/refactor" "$BODY"

# 4
BODY=$(cat <<EOF
Acceptance criteria
- If there are no transactions/incomes, show friendly empty-state cards (“Add your first income/transaction”).
- No broken charts (Chart.js should not throw on empty datasets).
EOF
)
create_issue "[P1][S] Add “empty state” UI for new users" "P1,size/S,type/feature,area/ui" "$BODY"

# 5
BODY=$(cat <<EOF
Acceptance criteria
- Add validation annotations (e.g., required title, amount > 0, date required).
- Update controller methods to use \`@Valid\` + \`BindingResult\`.
- UI displays field-level errors without losing entered form data.
- Add tests for at least one invalid submission.
EOF
)
create_issue "[P1][M] Add bean validation to \`Transaction\` and \`Income\` + show errors in UI" "P1,size/M,type/feature,type/tests" "$BODY"

# 6
BODY=$(cat <<EOF
Acceptance criteria
- Add tests covering:
  - anonymous user → returns \`"landing"\`
  - authenticated user → returns \`"home"\` and includes expected model keys (transactions, incomeList, totals, chart data)
- Use Spring Security test support to simulate auth.
EOF
)
create_issue "[P1][M] Add first meaningful tests for \`TransactionController#home\`" "P1,size/M,type/tests" "$BODY"

# 7
BODY=$(cat <<EOF
Acceptance criteria
- Deleting via \`/transaction/delete/{id}\` and \`/income/delete/{id}\` works reliably with Spring Security defaults.
- Either:
  - include CSRF token in fetch requests, OR
  - explicitly configure CSRF (with justification in docs).
- Add tests (preferred) or clear documentation explaining the choice.
EOF
)
create_issue "[P1][M] Fix/clarify CSRF behavior for AJAX delete endpoints" "P1,size/M,type/bug,area/security,type/tests" "$BODY"

# 8
BODY=$(cat <<EOF
Acceptance criteria
- CSV export correctly handles commas/quotes/newlines in description/title.
- Response header sets a filename like \`transactions-YYYY-MM.csv\`.
- Add at least one test validating output format for “special character” input.
EOF
)
create_issue "[P1][M] Improve CSV export: consistent quoting + file name includes date" "P1,size/M,type/feature" "$BODY"

# 9
BODY=$(cat <<EOF
Acceptance criteria
- Create \`TransactionService\` and \`IncomeService\`.
- Move “ownership checks” and CRUD logic out of controller.
- Controller becomes request/response orchestration only.
- Add unit tests for the services.
EOF
)
create_issue "[P2][M] Introduce service layer for transactions/incomes (thin controllers)" "P2,size/M,type/refactor" "$BODY"

# 10
BODY=$(cat <<EOF
Acceptance criteria
- Create a \`CashflowSummaryService\` that computes:
  - monthly spending total
  - monthly income total
  - net cashflow
  - category totals for charts
- Both \`home()\` and \`dashboard()\` use the service.
- Add tests for edge cases (null dates, empty lists).
EOF
)
create_issue "[P2][M] Deduplicate monthly summary calculations (home + dashboard)" "P2,size/M,type/refactor" "$BODY"

# 11
BODY=$(cat <<EOF
Acceptance criteria
- Add a clean way to compute summaries for different periods:
  - current month (default)
  - selected month via query param (e.g., \`?month=2026-01\`)
- Use Strategy (or similar) rather than adding conditionals everywhere.
- Tests cover at least 2 periods.
EOF
)
create_issue "[P2][M] Replace “Month = now” with a \`PeriodStrategy\`" "P2,size/M,type/refactor,type/feature" "$BODY"

# 12
BODY=$(cat <<EOF
Acceptance criteria
- Centralize common errors (not found, validation, generic exception).
- Return a friendly error view for HTML routes and sensible responses for JSON endpoints.
- Add tests for one HTML error and one JSON error.
EOF
)
create_issue "[P2][M] Add \`@ControllerAdvice\` for consistent error handling" "P2,size/M,type/refactor" "$BODY"

# 13
BODY=$(cat <<EOF
Acceptance criteria
- DataSeeder does not run in tests or production-like runs.
- Use \`@Profile("dev")\` or a config property.
- Add a brief note in README: how to enable dev seeding.
EOF
)
create_issue "[P2][S] Make \`DataSeeder\` run only in dev profile" "P2,size/S,type/refactor" "$BODY"

# 14
BODY=$(cat <<EOF
Acceptance criteria
- Username uniqueness enforced at DB level and in validation (friendly message).
- Registration fails gracefully if username is taken.
- Tests cover duplicate registration attempt.
EOF
)
create_issue "[P2][M] Enforce unique usernames + improve registration validation" "P2,size/M,type/feature,area/security,type/tests" "$BODY"

# 15
BODY=$(cat <<EOF
Acceptance criteria
- Transaction.amount and Income.amount are BigDecimal.
- All calculations updated (monthly totals, category totals, CSV export).
- UI formatting updated (2 decimals).
- Add tests showing rounding issues are gone.
EOF
)
create_issue "[P2][L] Replace \`double\` amounts with \`BigDecimal\` (money correctness)" "P2,size/L,type/refactor,type/tests,area/data" "$BODY"

# 16
BODY=$(cat <<EOF
Acceptance criteria
- Create a @MappedSuperclass (or composition) to reduce duplicated fields (amount/date/description/user).
- Keep JPA mapping clean.
- No behavior changes; tests still pass.
- Add at least one test validating mappings still work.
EOF
)
create_issue "[P2][L] Introduce a shared base entity for \`Income\` and \`Transaction\`" "P2,size/L,type/refactor" "$BODY"

# 17
BODY=$(cat <<EOF
Acceptance criteria
- Filter transactions by:
  - start/end date
  - category
  - title/description contains
- Filters apply to list + totals/charts (document the chosen behavior).
- Tests for filter logic (service/repo level).
EOF
)
create_issue "[P3][M] Add filters: date range + category + text search" "P3,size/M,type/feature,area/ui" "$BODY"

# 18
BODY=$(cat <<EOF
Acceptance criteria
- Use Spring Data Pageable for lists.
- UI shows page controls and page size selection.
- Tests cover page boundaries and sorting.
EOF
)
create_issue "[P3][M] Add pagination for transactions and incomes" "P3,size/M,type/feature" "$BODY"

# 19
BODY=$(cat <<EOF
Acceptance criteria
- User can set a budget amount per category per month.
- Dashboard shows remaining/over-budget per category.
- Include data model changes + migrations approach documented.
- Tests cover calculations and over-budget edge cases.
EOF
)
create_issue "[P3][L] Implement monthly budgets per category" "P3,size/L,type/feature,area/data" "$BODY"

# 20
BODY=$(cat <<EOF
Acceptance criteria
- User can create a recurrence rule (weekly/monthly) for income or transaction.
- System generates instances up to a horizon (e.g., next 3 months) without duplicates.
- Add tests for month-end edge cases (e.g., Jan 31).
EOF
)
create_issue "[P3][L] Recurring income/expense rules" "P3,size/L,type/feature,area/data" "$BODY"

# 21
BODY=$(cat <<EOF
Acceptance criteria
- Upload CSV → show preview table → confirm import.
- Validation errors reported per-row (missing date, bad amount, unknown category).
- Tests cover importer parsing and validation.
EOF
)
create_issue "[P3][L] CSV import with preview + validation report" "P3,size/L,type/feature,type/tests" "$BODY"

# 22
BODY=$(cat <<EOF
Acceptance criteria
- One purchase can be split into line items with categories/amounts.
- Totals and category chart reflect split lines.
- UI supports adding/removing split lines.
- Tests cover invariants (sum of splits == transaction total).
EOF
)
create_issue "[P3][L] Split a transaction across multiple categories" "P3,size/L,type/feature,area/data" "$BODY"

# 23
BODY=$(cat <<EOF
Acceptance criteria
- Add notes to transaction and/or income.
- Display notes in list/detail/edit and CSV export.
- Migration plan documented.
- Tests cover persistence and export.
EOF
)
create_issue "[P3][M] Add “notes” field and display it everywhere relevant" "P3,size/M,type/feature" "$BODY"

# 24
BODY=$(cat <<EOF
Acceptance criteria
- Endpoints:
  - GET /api/transactions
  - GET /api/incomes
- Response includes only the logged-in user’s data.
- Add at least one contract/integration test.
Stretch: OpenAPI docs.
EOF
)
create_issue "[P3][M/L] Add a small REST API (read-only) for transactions + incomes" "P3,size/M,type/feature" "$BODY"

# 25
BODY=$(cat <<EOF
Acceptance criteria
- Add Flyway + baseline migration(s) matching current schema.
- Set ddl-auto=validate (or none) for non-dev profiles.
- App boots clean on an empty DB.
- Document how to reset/recreate DB in dev.
EOF
)
create_issue "[P4][L] Introduce Flyway migrations and stop using \`ddl-auto=update\`" "P4,size/L,type/devops,area/data" "$BODY"

# 26
BODY=$(cat <<EOF
Acceptance criteria
- docker compose up brings up Postgres + app.
- Uses environment variables (no secrets committed).
- README includes one-command run instructions.
EOF
)
create_issue "[P4][M] Add Docker Compose for local run (app + Postgres) + docs" "P4,size/M,type/devops" "$BODY"

# 27
BODY=$(cat <<EOF
Acceptance criteria
- GitHub Actions runs mvn test on PR.
- Add at least one quality gate: Spotless/formatter OR Checkstyle OR SpotBugs.
- README: how to run the same checks locally.
EOF
)
create_issue "[P4][M] Add CI pipeline with required checks (tests + formatting)" "P4,size/M,type/devops,type/tests" "$BODY"

# 28
BODY=$(cat <<EOF
Acceptance criteria
- Add integration tests that use Postgres Testcontainers for repositories and/or service layer.
- CI runs them reliably.
- Document expected runtime and how to run locally.
EOF
)
create_issue "[P4][M/L] Add Testcontainers-based integration tests for repositories" "P4,size/M,type/tests,type/devops" "$BODY"

# 29
BODY=$(cat <<EOF
Acceptance criteria
- Every request has a correlation ID (filter/interceptor).
- Logs include safe user identifier, route, and correlation ID.
- Add TROUBLESHOOTING.md with “how to collect logs and reproduce.”
EOF
)
create_issue "[P4][M] Add structured logging + request correlation IDs + troubleshooting guide" "P4,size/M,type/devops" "$BODY"

# 30
BODY=$(cat <<EOF
Acceptance criteria
- Configure Actuator endpoints (at least health, info) with appropriate exposure.
- Ensure endpoints are protected (or intentionally exposed with reasoning).
- Add a custom health indicator for DB connectivity (or verify built-in behavior) and test it.
EOF
)
create_issue "[P4][M] Harden Actuator usage (health/info + security)" "P4,size/M,type/devops,area/security" "$BODY"

echo "--------------------------------------------------------"
echo "Done! Issues bootstrapped."
