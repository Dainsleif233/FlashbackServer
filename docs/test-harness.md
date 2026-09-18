# Test harness

## Phase 1 feasibility spike (2026-06-03)
- Server reached `Done (`: yes
- Plugin loaded (`FlashbackServer enabled.`): yes
- Approx cold-boot time: 8 seconds
- Blockers: none

## Running tests
- Fast unit tests (no network, no server): `./gradlew test`
- Integration tests (boots real Paper servers + a headless bot): `./gradlew integrationTest`
  - Downloads Paper 1.16.1, 1.21.5 and Paper 26.2 jars once into temporary test directories.
  - Requires JDK 21 and JDK 25, network access, and free local ports.
  - Set `JAVA21_HOME` and `JAVA25_HOME` to portable JDK installations when they are not
    auto-detected (the CI workflow demonstrates this).
  - Paper 1.16.1 NMS compile input: run `nms/v1_16_1/libs/paper-1.16.1-138.jar` once so paperclip
    writes `nms/v1_16_1/libs/paperclip-run/cache/patched_1.16.1.jar` (gitignored).
  - Paper 1.16.1 runtime needs Java 8 (Paper rejects Java > 14; worldgen hangs on Java 11 in this
    environment). Core + the 1.16.1 adapter are compiled with `--release 8`.
