# Ford Guardian PID trust registry notes

## Trusted tier: SAE Mode 01
Ford Guardian performs ECU capability discovery and only polls SAE Mode 01 PIDs advertised as supported. Decoding also has plausibility and freshness guards. These readings can be used as primary live evidence when marked fresh.

## Experimental tier: Ford Mode 22
Ford-specific Mode 22 definitions are not automatically Ford-verified merely because they decode to a plausible number. They remain experimental unless independently validated for the exact vehicle/module/calibration.

The app retains a broader library of Ford Mode 22 labels/decoders behind the Experimental Ford PIDs control. Those entries are not treated as verified just because they came from a scan export or produce plausible values.

Three Ranger-specific definitions were singled out in the v18 validation suite because their supplied formulas had explicit test fixtures:
- 6R80 transmission fluid temperature: request 221E1C, TCM header 7E1, unsigned 16-bit raw / 16 °C.
- Pre-turbo exhaust gas temperature: request 222425, ECM header 7E0, unsigned 16-bit raw °C.
- DPF soot load: request 22242C, ECM header 7E0, unsigned 16-bit raw / 100 %.

Even these three remain experimental until compared against a trusted scan tool on the actual Ranger. Other Mode 22 entries require equal or stronger validation before alerts or repair decisions use them.

## Historical sentinel/unreliable signals
Some Torque logs returned EGT14 = -40 °C continuously and EOT = 0 °C continuously. Treat those historical channels as unsupported/sentinel/unreliable until proven otherwise, not as real temperatures.
