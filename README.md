## Airport Concurrent Simulation

A Java-based airport simulation developed to demonstrate **concurrent programming, thread coordination, resource management, and synchronization** in a realistic airport environment.

The simulation models multiple aircraft arriving, landing, receiving ground services, and departing while sharing limited airport resources such as a single runway, three gates, and one refuelling truck.

## Features

* Multiple aircraft operating concurrently
* Single shared runway for landing and departure
* Maximum of three aircraft on the airport grounds
* Three airport gates with controlled allocation
* ATC-controlled landing and departure clearances
* Emergency landing priority
* Passenger boarding and disembarking performed concurrently
* Aircraft cleaning and supply refilling performed concurrently
* Exclusive access to a single refuelling truck
* Aircraft waiting and holding when airport capacity is unavailable
* Random aircraft arrival intervals of 0–2 seconds
* Random passenger numbers up to 50 per aircraft
* Final airport safety checks and performance statistics

## Concurrency Concepts

The project demonstrates several Java concurrency mechanisms:

* **Threads** — individual pilot and airport operation threads
* **ExecutorService** — manages passenger, ground crew, and refuelling tasks
* **BlockingQueue** — used as the ATC message queue
* **CompletableFuture** — coordinates requests between pilots and ATC
* **CountDownLatch** — coordinates the congestion and emergency landing scenario
* **Synchronization** — protects shared statistics and console output
* **Single-thread executor** — ensures only one refuelling operation occurs at a time

## System Architecture

The simulation is divided into several main components:

| Component              | Responsibility                                       |
| ---------------------- | ---------------------------------------------------- |
| `AirportSimulation`    | Starts and coordinates the overall simulation        |
| `Flight`               | Represents aircraft behaviour and lifecycle          |
| `AirTrafficController` | Manages runway, gate allocation, and flight requests |
| `Gate`                 | Represents airport gate resources                    |
| `AirportStatistics`    | Records waiting times and passenger statistics       |
| `AtcMessage` classes   | Represent messages exchanged with ATC                |

## Airport Workflow

Each aircraft follows a lifecycle similar to:

```text
Arrival
   ↓
Landing Request
   ↓
ATC Clearance
   ↓
Landing
   ↓
Taxi to Gate
   ↓
Passenger Disembarkation
   ↓
Ground Operations
   ├── Supply Refill
   ├── Cleaning
   ├── Refuelling
   └── Passenger Boarding
   ↓
Departure Request
   ↓
ATC Clearance
   ↓
Taxi to Runway
   ↓
Take-off
   ↓
Departure
```

Ground operations are executed concurrently where appropriate while shared resources remain controlled.

## Statistics

After all aircraft have completed their journeys, the simulation reports:

* Minimum waiting time
* Average waiting time
* Maximum waiting time
* Number of aircraft served
* Number of passengers boarded
* Whether all gates are empty
* Whether the runway is clear
* Whether the airport grounds are clear

## Technologies

* **Java**
* Java Concurrency API
* `Thread`
* `ExecutorService`
* `BlockingQueue`
* `CompletableFuture`
* `CountDownLatch`
* Git & GitHub
* Visual Studio Code

## How to Run

### Using VS Code

Open `AirportSimulation.java` and select **Run Java**.

### Using the Terminal

Compile the source files:

```bash
javac -d out src/*.java
```

Run the simulation:

```bash
java -cp out AirportSimulation
```

## Project Structure

```text
Airport-Concurrent-Simulation/
│
├── src/
│   ├── AirportSimulation.java
│   ├── Flight.java
│   ├── AirTrafficController.java
│   ├── AirportStatistics.java
│   ├── Gate.java
│   ├── AtcMessage.java
│   ├── LandingRequest.java
│   ├── LandingRunwayCleared.java
│   ├── DepartureRequest.java
│   ├── GateReleased.java
│   ├── DepartureCompleted.java
│   └── FinishRequest.java
│
├── .gitignore
└── README.md
```

## Purpose

This project was developed as part of a Concurrent Programming module to apply Java concurrency concepts to a realistic resource-sharing problem.

The main focus is on coordinating multiple independent activities safely while preventing conflicts over shared airport resources.
