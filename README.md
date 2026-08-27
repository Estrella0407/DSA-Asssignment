# TAR UMT Resorts Management System

**BMCS2063 Data Structures and Algorithms Assignment**

A console-based resort management application developed in Java. The system follows the Entity-Control-Boundary (ECB) design pattern and uses team-developed collection Abstract Data Types (ADTs) instead of the Java Collections Framework.

## Features

The integrated system contains four modules:

1. **Walk-In Registration and Standard Booking**
2. **VIP and Loyalty Tier Priority Room Allocation**
3. **Housekeeping and Task Log**
4. **Front-Desk Service**

All modules operate on shared room and guest records during the same program session. The `GuestDirectory` control component provides a central location for registering, searching, and retrieving guest records across the relevant modules.

## Custom Collection ADTs

The custom ADTs and their interfaces are located in `src/adt`.

| ADT | Primary usage |
| --- | --- |
| `DoublyLinkedList` | Stores rooms and guests and supports sequential, FIFO, and LIFO operations |
| `ArrayPriorityQueue` | Prioritizes VIP guests for room allocation |
| `HashTable` through `Dictionary` | Provides efficient guest and room lookup |

### ADT Usage by Module

| Module or component | Primary collection ADT |
| --- | --- |
| Walk-In Registration | `DoublyLinkedList` |
| Priority Room Allocation | `ArrayPriorityQueue` and the shared room list |
| Housekeeping | `DoublyLinkedList` |
| Front-Desk Service | `HashTable` through `Dictionary` |
| Shared `GuestDirectory` | `HashTable` and `DoublyLinkedList` |

## Software Requirements

- Apache NetBeans IDE
- Java Development Kit (JDK) 17 or later
- A terminal or console that supports standard keyboard input

This repository is configured as a NetBeans Ant project. Its main class is `main.MainApp`.

## Opening and Running the Project in NetBeans

1. Clone the repository or download and extract its ZIP file.
2. Open Apache NetBeans.
3. Select **File > Open Project**.
4. Select the extracted or cloned project folder, such as `DSA-Asssignment` or `DSA-Asssignment-master`.
5. Wait for NetBeans to load the project.
6. If NetBeans requests a Java platform, select JDK 17 or later.
7. Right-click the project and select **Clean and Build**.
8. Right-click the project again and select **Run**.
9. The main menu will appear in the NetBeans Output or Terminal window.

### Alternative Method

Open `src/main/MainApp.java`, right-click the file, and select **Run File**.

## Using the Application

Enter the number of the required module at the main menu:

| Option | Module |
| ---: | --- |
| 1 | Walk-In Registration and Standard Booking |
| 2 | VIP and Loyalty Tier Priority Room Allocation |
| 3 | Housekeeping and Task Log |
| 4 | Front-Desk Service |
| 0 | Exit the application |

Follow the instructions displayed by each module. Use the provided Back or Return option to return to the main menu.

## Sample Data

The application creates sample room and guest records when it starts. The sample room numbers are:

`101`, `102`, `103`, `104`, `105`, `201`, and `202`

Because the modules share room and guest data, an update made in one module may be visible in another module during the same program session.

## Data Storage

The current version stores records in memory using the custom collection ADTs. Sample data is recreated whenever the application starts, and changes made during a session are not saved after the program closes.

## Project Structure

```text
DSA-Asssignment/
├── nbproject/       NetBeans project configuration
├── src/
│   ├── adt/         Collection ADT interfaces and implementations
│   ├── boundary/    User interface and input/output classes
│   ├── control/     Business logic and module control classes
│   ├── entity/      Entity and data classes
│   ├── main/        Main application driver
│   └── test/        System integration test class
├── build.xml        Ant build configuration
└── manifest.mf      Application manifest
```

## Integration Test

The integration test is located at `src/test/TestSystemIntegration.java`.

In NetBeans, open the file, right-click it, and select **Run File**. Enable Java assertions with the `-ea` JVM option so that assertion failures are reported.

Alternatively, compile and run it from the project root with Ant and Java:

```bash
ant clean compile
java -ea -cp build/classes test.TestSystemIntegration
```

## Troubleshooting

- **Invalid target release:** Configure the project to use JDK 17 or later under **Project Properties > Libraries > Java Platform**.
- **Main class not detected:** Set `main.MainApp` as the main class under **Project Properties > Run**.
- **Input not accepted:** Click inside the NetBeans Output or Terminal window before typing.
- **Unexpected state after several updates:** Exit and restart the application to reload the original sample data.

## Important Note

Do not remove or rename the package folders. The Java package declarations depend on the existing project structure.
