# Time-Conscious Network Verification

## Network Properties
- Network topology
- Switch
    - Forwarding state
    - Queue architecture
    - Queue delay
- Link
    - Failure probability
    - Link delay

## Bounded Reachability 
Given network properties, compute the probability of a packet from node `X` reaching node `Y` in under `T` time unit

## How To Run
- Download UPPAAL 
- Edit server path in `app/src/main/java/tempus/App.java` to adjust with your OS and UPPAAL directory
- Edit model path in the same file
- Run `./gradlew run`