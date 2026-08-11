# Ranger Brain source manifest

## Source trust order
1. Fresh live OBD/DTC evidence from Ford Guardian.
2. This vehicle's private history and repeated observed logs.
3. Ford OEM documentation.
4. User-licensed Haynes manual exports.
5. General mechanical reasoning.

## Public Ford sources used by the backend's restricted web search
- Ford Australia Owner Manuals: https://www.ford.com.au/owners/vehicle-support/owner-manuals/
- Ford Australia Body Equipment Manuals: https://www.ford.com.au/owners/vehicle-support/body-equipment-manuals/
- Ford Australia Right to Repair: https://www.ford.com.au/owners/vehicle-support/right-to-repair/
- Ford Service Content owner-manual pages: https://www.fordservicecontent.com/
- PX Ranger MkII 2019 Body and Equipment Mounting Manual: https://www.ford.com.au/content/dam/Ford/website-assets/ap/au/owner/vehicle-support/body-equipment-manuals/P375%20MY19%20TKD%20ASEAN%20BEMM-1.pdf
- PX Ranger MkII 2015 Body and Equipment Mounting Manual: https://www.ford.com.au/content/dam/Ford/website-assets/ap/au/owner/vehicle-support/body-equipment-manuals/Body-Equipment-Manuals_au.pdf

## Haynes source supplied by the user
- Manual family: Ford Ranger / Mazda BT-50 Diesel 2011–2018.
- Haynes manual identifier supplied by user: m_902.
- Public product information: https://au.haynes.com/products/ford-ranger-mazda-bt-50-diesel-2011-2018-haynes-repair-manual
- The user's authenticated Haynes session identifier is deliberately NOT stored here.

The backend cannot use a private Haynes login session. Put legitimately exported/printed Haynes pages or PDFs into `knowledge/user_manuals/`; rerun `npm run knowledge:setup` to index them privately.

## Deeper Ford workshop information
Ford Australia's Right-to-Repair page directs Australian aftermarket repairers through AASRA and Ford Service Information / Rotunda. Use that licensed route for workshop procedures, wiring, TSB-style service information and exact repair specifications not available in the public owner/BEMM material.

### Haynes section pointer supplied 12 Aug 2026
- Manual: `m_902` — Ford Ranger / Mazda BT-50 Diesel 2011–2018.
- Chapter pointer: `c_15355`.
- Section pointer: `s_344759`.
- Account page: https://mole.haynes.com/manualOverview?chapterId=c_15355&sectionId=s_344759
- Status: **pointer registered; protected section text not yet indexed**.
- Trust handling: Ranger Brain must not infer the section title, procedure, specification or repair conclusion from the opaque chapter/section IDs. Once the user exports/prints the licensed section to PDF and places it under `knowledge/user_manuals/`, it becomes an indexed Haynes source on the next `npm run knowledge:setup`.

## User-provided Ford Ranger workshop-manual bundle imported 12 Aug 2026
- File: `Ford_Ranger_3.2L_Duratorq_6_Speed_Auto_Workshop_Manual_2018-2021.pdf`
- Stored privately under `knowledge/user_manuals/`.
- Contains Ford owner/workshop content for multiple configurations; Ranger Brain must apply
  `manual_indexes/MANUAL_APPLICABILITY_GUARDRAILS.md`.
- For the primary 2018 3.2L/6R80 vehicle, high-value workshop sections begin around PDF p.1256
  (3.2L engine), p.1510 (turbo), p.1555 (3.2L emissions/EGR), p.1594 (intake/CAC),
  p.1642 (6R80), and p.1931 (3.2L exhaust/DPF).
- This file is user-provided licensed/private material and must not be exposed through a public file endpoint.

