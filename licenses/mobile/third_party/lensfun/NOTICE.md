# Lensfun attribution and source

Copyright: Lensfun database contributors. The selected database records and their
adaptation in LensfunCalibrations.kt are licensed under Creative Commons
Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0). See COPYING.CC_BY-SA_3.0.

Project: https://lensfun.github.io/
Source repository: https://github.com/lensfun/lensfun
Pinned commit: 2f1be1deab50da7869fc5d23f481939803d845bf
Original files:
- https://github.com/lensfun/lensfun/blob/2f1be1deab50da7869fc5d23f481939803d845bf/data/db/slr-canon.xml
- https://github.com/lensfun/lensfun/blob/2f1be1deab50da7869fc5d23f481939803d845bf/data/db/slr-nikon.xml
- https://github.com/lensfun/lensfun/blob/2f1be1deab50da7869fc5d23f481939803d845bf/data/db/mil-sony.xml
License: https://creativecommons.org/licenses/by-sa/3.0/

Changes by NativeOffice Studio: selected 35 full-frame prime lens records and
53 camera records, added stable application IDs and exact EXIF-name aliases, and
converted distortion/TCA coefficients and camera crop factors into Kotlin constants.
The coefficients are unchanged. profiles.xml retains the selected source records,
including contributor comments. PTLens distortion and, on 28 profiles, poly3
transverse chromatic aberration data are applied through separate opt-in controls.
Vignetting entries retained in this source subset are not implemented.
No Lensfun software/library code is incorporated. No contributor or manufacturer
endorsement is implied. The adapted calibration data remains CC BY-SA 3.0.
The subset and Kotlin data are reproducible with scripts/update_lensfun_subset.py;
the script verifies SHA-256 of all three pinned upstream source XML files.
