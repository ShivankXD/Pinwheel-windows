# Proposed mobile parity diagnostic

`P2ParityProbe.kt` is a reviewable diagnostic for the owner to run in the mobile
test source set at commit `2a417fd`. It has not been copied into the mobile repo,
compiled for Android, installed or executed by the Windows agent. It uses
private preview-renderer members by reflection and may require an owner-side
repair if those names change.

The diagnostic produces `windows-refs/diagnostics/mobile-math-probe.json` and
84 neutral SWAY tiles in `windows-refs/diagnostics/neutral/<sample>/<t>.png` under
the app's external files directory. It uses the existing preview GL context,
restores its GL state and does not edit projects. The JSON records the phone
GL vendor, renderer, version and high-float precision, plus the exact reduced
Scene Cut hash values at the four golden times.

These files can be supplied alongside the existing read-only Windows references
to compare against `evidence/p2/diagnostics`. Neutral copies isolate sample
preparation, SWAY sampling and RGB565 readback from catalog effect math.
The Windows task `:pinwheel-render:p2MathProbe` writes separate hardware and
WARP diagnostics; neither replaces the hardware golden frames.

An owner-run diagnostic must preserve the installed app and its project data.
Do not use `connectedAndroidTest` or a connected Gradle task on the owner's
phone: the build brief warns that those tasks can uninstall the app and delete
projects. The existing golden images remain the acceptance inputs.
