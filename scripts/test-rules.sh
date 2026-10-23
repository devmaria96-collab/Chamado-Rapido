#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac --release 17 -encoding UTF-8 -d "$OUT" backend/src/main/java/br/com/chamadorapido/TicketRules.java backend/src/main/java/br/com/chamadorapido/TriageEngine.java backend/src/test/java/br/com/chamadorapido/PureRulesCheck.java
java -cp "$OUT" br.com.chamadorapido.PureRulesCheck
