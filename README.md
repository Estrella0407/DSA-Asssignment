TARUMT RESORTS MANAGEMENT SYSTEM
BMCS2063 DATA STRUCTURES AND ALGORITHMS ASSIGNMENT
=================================================

1. SYSTEM OVERVIEW
------------------
This is a console-based resort management application developed using Java.
The system follows the Entity-Control-Boundary (ECB) design pattern and uses
team-developed Collection ADTs instead of the Java Collections Framework.

The integrated system contains the following modules:

1. Walk-In Registrations and Standard Booking
2. VIP and Loyalty Tier Priority Room Allocation
3. Housekeeping and Task Log
4. Front-Desk Service


2. SOFTWARE REQUIREMENTS
------------------------
- Apache NetBeans IDE
- Java Development Kit (JDK) 17 or later
- A terminal or console that supports standard keyboard input


3. HOW TO OPEN AND RUN THE PROJECT IN NETBEANS
-----------------------------------------------
1. Extract the submitted ZIP file if it has not been extracted.
2. Open Apache NetBeans.
3. Select File > Open Project.
4. Browse to and select the project folder named "DSA-Asssignment-master".
5. Wait for NetBeans to load the project.
6. If NetBeans asks for a Java platform, select JDK 17 or later.
7. Right-click the project and select Clean and Build.
8. Right-click the project again and select Run.
9. The program will start in the Output/Terminal window and display the main
   menu.

Main class: main.MainApp


4. ALTERNATIVE WAY TO RUN
-------------------------
The application may also be started by opening:

src/main/MainApp.java

Then right-click the file and select Run File.


5. USING THE APPLICATION
------------------------
At the main menu, enter the number of the required module:

1 - Walk-In Registrations and Standard Booking
2 - VIP and Loyalty Tier Priority Room Allocation
3 - Housekeeping and Task Log
4 - Front-Desk Service
0 - Exit the application

Follow the instructions shown on the screen. Enter only the requested data.
Use the Back or Return option provided in each module to return to the main
menu.


6. SAMPLE DATA
--------------
The application automatically creates sample room and guest records when it
starts. Sample room numbers include:

101, 102, 103, 104, 105, 201 and 202

Some functions may update the shared room data. Therefore, a change made in
one module may be visible in another module during the same program session.


7. DATA STORAGE
---------------
The current version stores its records in memory using the custom Collection
ADTs. The sample data is recreated whenever the application is restarted.
Changes made during a session are not saved after the program is closed.


8. CUSTOM COLLECTION ADTs USED
------------------------------
- DoublyLinkedList
- ArrayPriorityQueue
- HashTable

The source files for the ADTs and their interfaces are located in the
src/adt folder.


9. PROJECT STRUCTURE
--------------------
src/adt       - Collection ADT interfaces and implementations
src/boundary  - User interface and input/output classes
src/control   - Business logic and module control classes
src/entity    - Entity and data classes
src/main      - Main application driver
src/test      - System integration test class


10. TROUBLESHOOTING
-------------------
- If "invalid target release" appears, configure the project to use JDK 17 or
  later in Project Properties > Libraries > Java Platform.
- If the main class is not detected, set main.MainApp as the Main Class in
  Project Properties > Run.
- If input is not accepted, click inside the Output/Terminal window before
  typing.
- If the program behaves unexpectedly after several updates, exit and restart
  the application to reload the original sample data.


11. IMPORTANT NOTE
------------------
Do not remove or rename the package folders because the Java package names
depend on the existing project structure.

End of ReadMe.txt
