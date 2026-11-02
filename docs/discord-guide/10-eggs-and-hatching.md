## 10. Eggs and hatching

Use an egg item on a block to place it. Right-click the placed egg for progress
and condition messages. **Crouch + right-click** picks it up; the item retains
its incubation data, and placing it again resumes that data.

Some eggs need an environment to **activate** before incubation:

- **Night Fury:** combined light level **1 or below**. Use an enclosed dark spot.
- **Deadly Nadder:** **block light 10 or above**; daylight alone is not the
  specific light measurement this condition checks.
- **Monstrous Nightmare:** fire or soul fire at the egg's block position.
- **Skrill:** fire or soul fire at the egg's block position in this build.
- **Hideous Zippleback:** water at its position **and** combined light **3 or below**.
- **Gronckle, Whispering Death, Terrible Terror, Speed Stinger:** no special
  activation environment is specified in the shipped type definitions.

Keep the required environment until the egg reports **Activated**. The current
implementation then counts down incubation while the egg is loaded and ticking.
Hatching duration varies; lag, configuration and an unloaded area can affect
the real-world wait. Use the egg's progress message rather than assuming a
fixed number of minutes.

Babies inherit genetic data. A newly hatched dragon still needs care and growth.
Do not assume every hatchling is automatically owned: lineage and taming state
matter. Full natural breeding and incubation playthroughs remain less thoroughly
tested than the automated hatch/persistence checks.
