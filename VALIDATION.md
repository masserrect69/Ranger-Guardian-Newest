# Ford Guardian v18 validation report

Validation date: 30 July 2026

## Executable core tests

A JVM harness compiled and executed the app's real decoder, payload parser, supported-PID map, DTC parser, vehicle registry and PID registry code against the supplied Torque Pro scan export.

Result:

```text
Core tests passed: 149 PID definitions, [7E8, 7E9] support maps
```

Covered cases:

- compact no-space `7E8` and `7E9` responses;
- Mode 01 coolant, RPM, module-voltage and catalyst-temperature formulas;
- separate ECM and TCM supported-PID bitmaps;
- long compact responses with a leading ISO-TP length byte;
- numbered/spaced multi-frame ISO-TP reconstruction;
- stored DTC decoding (`P0404`, `U0184` fixture);
- the three supplied Ranger custom formulas;
- unique runtime PID keys and names;
- TCM request header `7E1`; and
- anonymised Australian Ford `MPB` VIN-prefix detection.

## Additional checks

- `node --check backend/server.mjs`: passed.
- Production manifest, debug manifest and string-resource XML parsing: passed.
- `git diff --check`: passed.
- Kotlin syntax-focused source parser: no syntax-token errors; unresolved Android/Compose symbols are expected without the Android SDK.
- Privacy scan: no full vehicle VIN, calibration ID, OpenAI secret key or original machine-specific Gradle path remains.

## Build status

A full Android Gradle build was not possible in the repair runtime because it did not contain the Android SDK or Gradle distribution and could not provision those binary dependencies. Therefore no v18 APK is included and no claim is made that an installable APK was produced here.

Before road testing:

1. Build the debug variant in Android Studio.
2. Test connection with Torque Pro and other OBD apps fully closed.
3. Verify Mode 01 values at ignition-on/engine-off, idle and a controlled drive.
4. Enable one experimental Mode 22 favourite at a time and compare its value with Torque Pro.
5. Keep DPF/EGT/TFT alerts disabled until their live byte layout and scaling are confirmed on the vehicle.
