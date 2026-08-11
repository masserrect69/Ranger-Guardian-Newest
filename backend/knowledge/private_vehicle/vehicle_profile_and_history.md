# Private vehicle profile — Ford Guardian Ranger Brain

Confidence labels in this file are intentional. The AI must not promote a user report into an OEM fact.

## Configured vehicle — verified/profile
- Ford Ranger PX MkII, 2018.
- 3.2 L five-cylinder Duratorq/Puma common-rail turbo-diesel.
- 6R80 six-speed automatic transmission.
- Australian vehicle profile.
- Diagnostic transport configured for ISO 15765-4 CAN, 11-bit identifiers, 500 kbit/s.
- ECM request/response: 7E0 / 7E8.
- TCM request/response: 7E1 / 7E9.
- vLinker FS CV304 used by Ford Guardian over classic Bluetooth SPP.
- Historical app profile recorded approximately 258,270 km; later user reports describe the vehicle as approximately 260,000 km. Treat odometer as approximate unless the app supplies a current value.

## User-reported modifications/history — not OEM specification
- EGR cooler/exhaust path was blanked at the EGR-valve side and an EGR bypass cable was fitted. The user believed the cable tells the valve to remain closed. Exact control behaviour must be diagnosed rather than assumed.
- Exhaust-manifold-to-turbo gasket was re-used during turbo-related work. Treat an exhaust leak at this joint as a physical-inspection hypothesis if symptoms fit; do not state that it leaks without evidence.
- User has reported an intermittent loud high-pitched turbo/air/exhaust squeal at highway speed, roughly around 100 km/h and 2,000+ rpm, occurring intermittently rather than continuously. Diagnose from current evidence; do not anchor on one cause.
- User has previously been concerned about unusually frequent DPF regeneration intervals, around 50 km at times. Historical logs below did not capture an active regeneration event, so the cause remains unresolved in the knowledge base.

## Data rules for this vehicle
- Do not store or expose the full VIN in AI prompts or knowledge documents.
- Do not infer that EGR, DPF or turbo signals should follow stock behaviour without accounting for modifications.
- A known modification can explain data, but it is not permission to advise defeating emissions controls.
