CS 305: Operating Systems - Process Scheduling Simulator
Assignment: Process Scheduling Simulator
Author: Gokberk Ceviker
Date: December 19, 2025

------------------------------------------------------------------
1. OVERVIEW
------------------------------------------------------------------
This Java application simulates CPU scheduling algorithms:
- First-Come, First-Served (FCFS)
- Shortest Job First (SJF) - Non-preemptive
- Priority Scheduling - Non-preemptive
- Round Robin (RR) - Preemptive

It provides a Graphical User Interface (GUI) to load process data, 
edit it in real-time, select algorithms, and visualize the execution 
via a Gantt Chart and a Metrics Table.

------------------------------------------------------------------
2. PREREQUISITES
------------------------------------------------------------------
- Java Development Kit (JDK) 8 or higher installed.
- A terminal or command prompt.

------------------------------------------------------------------
3. COMPILATION & EXECUTION INSTRUCTIONS
------------------------------------------------------------------
Step 1: Save the source code
   Ensure the file "ProcessScheduler.java" is in your working directory.

Step 2: Run the Simulator
   I precompiled the code so just run the compiled class file:
   > java ProcessScheduler.java

Step 3: Using the Simulator
   - The program attempts to auto-load "src/processes.txt" or "processes.txt".
   - You can click "Load File" to select a text file manually.
   - You can edit the process data directly in the text area (ID, Arrival, Burst, Priority).
   - Select an algorithm from the dropdown.
   - If "Round Robin" is selected, enter a Time Quantum (must be > 0).
   - Click "Run Simulation" to generate the Gantt chart and metrics.

------------------------------------------------------------------
4. INPUT FILE FORMAT
------------------------------------------------------------------
Each line represents a process:
Process_ID, Arrival_Time, Burst_Time, Priority

Example:
P1, 0, 8, 3
P2, 1, 4, 1
P3, 2, 9, 4
P4, 3, 5, 2