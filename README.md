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
- Download and extract UPPAAL (version 4.1.26-1)
- Edit `uppaalRootPath` in `gradle.properties`
- (Windows only?) Edit server path in `app/src/main/java/tempus/App.java$121` to adjust with your OS
- Run `./gradlew run`

Adjust the topology file in `config/test.json` accordingly (file name is hardcoded for now)

## Notes
public enum LKind {
    name, init, urgent, committed, invariant, exponentialrate, comments
};

public enum EKind {
    select, guard, synchronisation, assignment, comments, probability
};