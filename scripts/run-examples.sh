#!/usr/bin/env bash
#
# Runs every example in this repository and reports a pass/fail summary.
#
# Examples come in two flavours:
#   * SDK tutorials      - classes named *SdkTutorial, each with its own main()
#   * Enterprise runners - Spring CommandLineRunners selected by a @Profile
#
# Both are discovered from the sources, so a new example is picked up without
# touching this script.
#
# The network is whatever .env configures (see TutorialClient and
# application.properties). In CI that is the local hiero-solo-action network.
#
# Usage:
#   scripts/run-examples.sh                  # everything
#   scripts/run-examples.sh --sdk            # SDK tutorials only
#   scripts/run-examples.sh --enterprise     # Spring runners only
#   scripts/run-examples.sh --only nft       # only examples matching a pattern
#
# Env:
#   EXAMPLE_TIMEOUT   per-example timeout in seconds (default 600)
#   MVN               maven command (default: mvn)

set -uo pipefail

cd "$(dirname "$0")/.."
PROJECT_DIR="$(pwd)"

MVN="${MVN:-mvn}"
EXAMPLE_TIMEOUT="${EXAMPLE_TIMEOUT:-600}"
LOG_DIR="$PROJECT_DIR/target/example-logs"

run_sdk=true
run_enterprise=true
filter=""

while [ $# -gt 0 ]; do
  case "$1" in
    --sdk) run_enterprise=false ;;
    --enterprise) run_sdk=false ;;
    --only)
      shift
      filter="${1:-}"
      [ -n "$filter" ] || { echo "--only needs a pattern" >&2; exit 2; }
      ;;
    -h | --help)
      sed -n '2,25p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
  shift
done

if [ ! -f .env ]; then
  echo "ERROR: no .env in $PROJECT_DIR — the examples need operator credentials." >&2
  echo "       In CI it is written from the hiero-solo-action outputs." >&2
  exit 2
fi

mkdir -p "$LOG_DIR"

echo "==> Building and resolving the classpath"
"$MVN" -q -B compile || exit 1
"$MVN" -q -B dependency:build-classpath -Dmdep.outputFile=target/classpath.txt || exit 1
CLASSPATH="$PROJECT_DIR/target/classes:$(cat target/classpath.txt)"

# Discover the examples.
sdk_tutorials=()
while IFS= read -r file; do
  class_path="${file#src/main/java/}"
  sdk_tutorials+=("$(echo "${class_path%.java}" | tr '/' '.')")
done < <(cd "$PROJECT_DIR" && find src/main/java -name '*SdkTutorial.java' | sort)

enterprise_profiles=()
while IFS= read -r profile; do
  enterprise_profiles+=("$profile")
done < <(grep -rho '@Profile("[^"]*")' src/main/java | sed 's/@Profile("\(.*\)")/\1/' | sort -u)

passed=()
failed=()

# Runs one example, tees its output to a log file and records the outcome.
# $1 label, $2 log file name, rest: the java command
run_example() {
  local label="$1" log_name="$2"
  shift 2
  local log="$LOG_DIR/$log_name.log"

  echo ""
  echo "=============================================================="
  echo "==> $label"
  echo "=============================================================="

  local -a cmd=("$@")
  if command -v timeout >/dev/null 2>&1; then
    cmd=(timeout --signal=KILL "$EXAMPLE_TIMEOUT" "$@")
  fi

  if "${cmd[@]}" 2>&1 | tee "$log"; then
    echo "--> PASS: $label"
    passed+=("$label")
  else
    echo "--> FAIL: $label (see ${log#"$PROJECT_DIR"/})"
    failed+=("$label")
  fi
}

if [ "$run_sdk" = true ]; then
  for fqcn in "${sdk_tutorials[@]}"; do
    simple="${fqcn##*.}"
    if [ -n "$filter" ] && [[ "$fqcn" != *"$filter"* ]]; then
      continue
    fi
    run_example "SDK $simple" "sdk-$simple" java -cp "$CLASSPATH" "$fqcn"
  done
fi

if [ "$run_enterprise" = true ]; then
  for profile in "${enterprise_profiles[@]}"; do
    if [ -n "$filter" ] && [[ "$profile" != *"$filter"* ]]; then
      continue
    fi
    run_example "Enterprise profile '$profile'" "enterprise-$profile" \
      java -cp "$CLASSPATH" \
      "-Dspring.profiles.active=$profile" \
      com.hedera.tutorial.TutorialApplication
  done
fi

echo ""
echo "=============================================================="
echo "Summary: ${#passed[@]} passed, ${#failed[@]} failed"
echo "=============================================================="
for name in "${passed[@]:-}"; do [ -n "$name" ] && echo "  PASS  $name"; done
for name in "${failed[@]:-}"; do [ -n "$name" ] && echo "  FAIL  $name"; done

if [ ${#failed[@]} -gt 0 ]; then
  exit 1
fi

if [ ${#passed[@]} -eq 0 ]; then
  echo "ERROR: no examples matched" >&2
  exit 2
fi
