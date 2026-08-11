# Workshop Manual Applicability Guardrails

Source: `Ford_Ranger_3.2L_Duratorq_6_Speed_Auto_Workshop_Manual_2018-2021.pdf`

## Important compilation issue

This user-provided PDF is a large compilation containing owner-manual material, Ford workshop material,
multiple engines, multiple emissions levels, different build dates and some generic/misfiled text.

### Known contradictory page
PDF page 1512 has a header for **3.2L Duratorq-TDCi Puma - Turbocharger - Overview** but the body says the
assembly has LH/RH turbochargers and discusses **EcoBoost twin turbochargers**. That text conflicts with
the adjacent 3.2L component/removal pages, which show a single turbocharger and an electronic guide-vane
adjuster. Ranger Brain must treat the twin-EcoBoost paragraph on p.1512 as misfiled/non-applicable to
the primary 3.2L Puma vehicle.

## Build-date selection
The primary vehicle is a 2018 Ranger. Prefer **Vehicles Built From: 17-08-2015** content.
Do not substitute a **Built Up To: 16-08-2015** procedure simply because it otherwise looks similar.

## Engine selection
Prefer sections explicitly naming **3.2L Duratorq-TDCi (148kW/200PS) - Puma**.
Shared 2.2L/3.2L procedures may be used only when the page explicitly includes the 3.2L engine.

## Emissions-market selection
The PDF contains Euro 6/SCR/reductant material as well as DPF-only configurations. Do not assume the
primary Australian 2018 vehicle has SCR/AdBlue hardware unless the vehicle profile, VIN/build data,
physical inspection or diagnostic data confirms it.

## Transmission selection
Prefer **Automatic Transmission - 6-Speed Automatic Transmission - 6R80** sections.
Do not mix 10-speed transmission information into the primary vehicle.

## Trust rule
A heading alone is not sufficient when the body contradicts the vehicle architecture. When there is a
conflict, prefer:
1. exact build-date component location/removal/installation pages,
2. exact-engine specifications,
3. exact-transmission specifications,
4. diagnostic pages that explicitly include the 3.2L,
and flag the conflict rather than silently resolving it.
