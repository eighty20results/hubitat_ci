# Getting started
1. Install JDK 11.
2. Add `me.biocomp.hubitat_ci:hubitat_ci:<version, eg: 0.25.2>` to your test dependencies (see `hubitat_ci_setup.md`).
3. Use `HubitatAppSandbox` or `HubitatDeviceSandbox` to load your script in tests (see `hubitat_ci_testing.md`).
4. Run your tests locally via Gradle/Maven or groovy + @Grab.
5. Optional: wire into CI (see `github_actions_snippet.yml`).

Existing in-depth docs:
- `docs/hubitat_ci_overview.md`
- `docs/hubitat_ci_setup.md`
- `docs/hubitat_ci_testing.md`
- `docs/hubitat_ci_example_homeconnect.md`
