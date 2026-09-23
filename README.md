# ✈️ Airport Concurrent Simulation

A Java-based airport simulation developed to demonstrate **concurrent programming, thread coordination, synchronization, and shared-resource management**.

The simulation models multiple aircraft arriving, landing, receiving ground services, and departing while sharing **one runway, three gates, and one refuelling truck**.

## 🚀 Features

* Multiple aircraft operating concurrently
* Single shared runway
* Three airport gates
* ATC-controlled landing and departure
* Emergency landing priority
* Concurrent passenger operations
* Concurrent cleaning and supply refilling
* Single refuelling truck
* Aircraft waiting when airport capacity is unavailable
* Final safety checks and statistics

## 🧵 Concurrency

The project uses several Java concurrency mechanisms:

* `Thread` — aircraft and airport activities
* `ExecutorService` — manages concurrent tasks
* `BlockingQueue` — handles ATC messages
* `CompletableFuture` — coordinates requests between pilots and ATC
* `CountDownLatch` — coordinates the emergency landing scenario
* `synchronized` — protects shared data and console output

## 🛫 Simulation Flow

```text
Arrival → Landing → Taxi → Gate
                    ↓
          Passenger Operations
                    ↓
          Ground Operations
                    ↓
          Departure → Take-off
```

Ground operations such as passenger handling, cleaning, supply refilling, and refuelling run concurrently where appropriate while shared resources remain controlled.

## 📊 Final Statistics

After all aircraft have completed their journeys, the simulation reports:

* Minimum, average, and maximum waiting time
* Aircraft served
* Passengers boarded
* Gate status
* Runway status
* Airport ground status

## ▶️ How to Run

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

## 📁 Project Structure

```text
Airport-Concurrent-Simulation/
├── src/
│   ├── AirportSimulation.java
│   ├── Flight.java
│   ├── AirTrafficController.java
│   ├── AirportStatistics.java
│   ├── Gate.java
│   └── AtcMessage classes
├── .gitignore
└── README.md
```

## 🎓 Project Context

A university project focused on applying **Java concurrency concepts** to a realistic resource-sharing problem.

The main focus is coordinating multiple independent activities safely while preventing conflicts over shared airport resources.



