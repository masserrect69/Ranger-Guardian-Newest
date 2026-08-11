# Observed baselines from this Ranger's historical live-data logs

These are **observations**, not Ford specifications. They summarize user-provided Torque/FORScan-style CSV sessions. Conditions, scan ordering, sensor support and modifications vary. Do not treat percentile ranges as diagnostic pass/fail thresholds.

## 24 Jun 2026 — mixed driving session
37,727 rows over about 1,029.5 s.
- RPM: median 2,031 rpm; 5th–95th percentile 835–2,702; max 4,006.
- Accelerator pedal: median 18.5%; 95th percentile 52.5%; max 99.5%.
- Coolant: median 85 °C; 95th percentile 89; max 90.
- DPF differential pressure: median 14.9 kPa; 5th–95th percentile 1.1–42.9; max 49.3. Interpret only with simultaneous exhaust flow/load.
- DPF soot percentage (open-loop channel): median 5%; 95th percentile/max 11%.
- MAP actual: median 151.2 kPa; 95th percentile 215.2; max 233.2. MAP demand was very similar in the logged stream, but asynchronous sampling means this is not a laboratory tracking-error measurement.
- MAF: median 74.74 g/s; 95th percentile 160.53; max 187.61.
- Fuel rail pressure actual: median 104,816 kPa; 95th percentile 159,342; max 179,056. Commanded median 104,640 kPa.
- VGT actual: median 50.2%; 95th percentile 85.1. VGT command median 50.2%.
- EGT11/12/13 medians: about 500/535/505 °C. EGT14 was fixed at -40 °C and is considered invalid/sentinel in this dataset.
- DPF_REGEN was reported Inactive in the captured samples; DPF_REGN_TYP was Passive.

## 24 Jun 2026 — hard-acceleration session
3,439 rows over about 85.3 s.
- RPM median 2,306; 95th percentile 3,553.5; max 3,809 rpm.
- Accelerator pedal reached 99.5%.
- Coolant median 79 °C; max 83.
- DPF differential pressure median 11.6 kPa; 95th percentile 36.0; max 39.7.
- DPF soot channel stayed at 2%.
- MAP actual median 129.8 kPa; 95th percentile 214.1; max 229.8; MAP demand was closely similar in the stream.
- MAF median 72.76 g/s; 95th percentile 181.07; max 186.97.
- Rail pressure actual 95th percentile about 165,001 kPa; max 174,944; commanded 95th percentile about 164,160.
- VGT actual median 64.31%; 95th percentile 89.8; VGT command median 63.14%.
- DPF_REGEN was reported Inactive.

## 3 Jul 2026 — session before a user-described static DPF regeneration
5,459 rows over about 129.3 s.
- Coolant median 78 °C.
- DPF differential pressure median 6.9 kPa; 95th percentile 8.9; max 9.7.
- DPF soot channel stayed at 3%.
- MAF median 52.875 g/s; CIMAP median 132 kPa.
- EGT11/12/13 medians about 140/138/140 °C.

## 3 Jul 2026 — session after the user-described static DPF regeneration
6,722 rows over about 146.3 s.
- Coolant median 78 °C.
- DPF differential pressure median 1.5 kPa; 95th percentile 2.1; max 2.2.
- DPF soot channel stayed at 3%.
- MAF median 17.9 g/s; CIMAP median 102 kPa.
- EGT11/12/13 medians about 175/202/425 °C.

**Important comparison warning:** the before/after sessions have materially different airflow/MAP/EGT conditions. The lower post-regeneration differential pressure is not, by itself, proof of a cleaned DPF. A valid comparison should normalize for RPM/exhaust flow/load.

## 21 Jul 2026 — later driving session
55,890 rows over about 1,311.2 s.
- Coolant median 81 °C; max 84.
- DPF differential pressure median 7.6 kPa; 95th percentile 26.9; max 35.5.
- DPF soot channel median/95th percentile 5%, range 0–5%.
- Distance-since-last-DPF channel median 34.8 km, max 44 km, with reset/sequence behaviour in the data; do not infer regeneration frequency from a single statistic.
- MAP actual median 136.0 kPa; 95th percentile 230.5; max 235.8; MAP demand median 135.8, 95th percentile 230.5.
- MAF median 39.31 g/s; 95th percentile 143.687; max 182.49.
- Rail pressure actual median 78,192 kPa; 95th percentile 150,528; max 177,104; commanded median 78,340.
- EGT11/12/13 medians about 300/308/300 °C; EGT14 remained fixed at -40 °C (invalid/sentinel).
- Captured DPF_REGEN was Inactive, DPF_REGN_STAT Off, DPF_REGN_TYP Passive.

## Interpretation rule
Use these sessions for pattern matching and “is this different from this vehicle's prior observed behaviour?” questions. Do not call a value normal/abnormal solely because it falls inside/outside these observed ranges. Always condition DPF pressure on flow/load, turbo pressure on barometric pressure and demand, rail pressure on demand and load, and temperatures on operating state.
