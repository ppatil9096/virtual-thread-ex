Here is a **professionally written, complete README.md** for your **Day‑1 Virtual Threads + JFR Lab**, based on everything you’ve built so far.  
It includes:

*   Overview
*   Requirements
*   Running instructions (IDE‑friendly)
*   Explanation of Virtual Threads
*   How to use JFR
*   How to read thread‑related data
*   What each demo does
*   References (with citations)

***

# 📘 **Java 25 – Virtual Threads & JFR Lab (Programmetic approach)**

This project contains hands‑on exercises for mastering **Virtual Threads (Project Loom)** and **Java Flight Recorder (JFR)** using **Java 25 LTS**.

You will learn:

*   Virtual Threads vs Platform Threads
*   Pinning and how it affects scalability
*   Structured Concurrency
*   How to record & inspect concurrency issues using JFR
*   How to analyze thread behavior inside Java Mission Control

***

# 🚀 **1. Requirements**

*   **JDK 25 (LTS)** installed
    *   Oracle confirms **JDK 25 is the latest LTS** release. 
*   IntelliJ IDEA (Community or Ultimate)
*   Java Mission Control (JMC)
    *   JFR is analyzed via JMC; IntelliJ does *not* natively open .jfr files.
*   Maven 3.9+ (comes bundled in IDE or system)

***

# 📂 **2. Project Structure**

    src/main/java/com/ppp/virtualthreadex/
        JfrStarter.java                 ← Robust JFR launcher (fixes "No chunks")

        pinning/
            PinningDemo.java            ← Causes VT pinning (blocking inside synchronized)
            NonPinningDemo.java         ← Proper non-pinning design

        server/
            VtHttpServer.java           ← HTTP server using Virtual Threads + Structured Concurrency

        load/
            LoadClient.java             ← Virtual-thread load generator (for /work endpoint)

***

# 🧠 **3. Understanding Virtual Threads (Java 25)**

Virtual Threads are **JVM‑managed lightweight threads** that are *not tied* to a specific OS thread.  
Oracle documentation states:

*   Virtual threads **unmount from carrier threads** on blocking I/O.
*   Platform threads stay tied to their OS thread for their whole lifetime.    [\[slideshare.net\]](https://www.slideshare.net/slideshow/the-future-of-java-records-sealed-classes-and-pattern-matching/251331268)

Multiple independent analyses confirm:

*   Virtual Threads drastically improve scalability for **I/O‑bound** workloads.
*   They don’t make CPU‑bound tasks faster.    [\[lucanerlich.com\]](https://lucanerlich.com/java/modern-java-features/)

### ✔ Virtual Threads give:

*   Scale to **millions** of concurrent tasks
*   Clean thread‑per‑request model
*   Simpler concurrency than async/reactive

### ⚠ Virtual Threads DO NOT help when:

*   Heavy CPU workloads
*   Heavy native calls (JNI/FFM)
*   Long blocking sections under monitors

***

# 🔥 **4. Pinning in Virtual Threads**

A Virtual Thread becomes **pinned** when it cannot unmount from the OS carrier thread.

Common causes include:

*   `synchronized` blocks/methods (mostly fixed in Java 24+ per **JEP 491**)
*   Native calls
*   Blocking inside long critical sections  
    

JFR helps detect pinning events (`jdk.VirtualThreadPinned`).

***

# 🎥 **5. JFR (Java Flight Recorder)**

JFR is a low‑overhead JVM profiler built into the JDK.  
It records JVM events such as:

*   Thread scheduling
*   Monitor enter/exit
*   Blocking I/O
*   Virtual Thread start/end
*   Virtual Thread pinning    [\[javarevisi...ogspot.com\]](https://javarevisited.blogspot.com/2026/01/i-tested-30-websites-to-learn-java-here.html)

## How JFR Works

JFR records **events**, not JVM-level thread dumps.  
To analyze concurrency, use JMC’s:

*   **Threads view**
*   **Event Browser → JavaMonitorEnter**
*   **VirtualThreadPinned events**
*   **Thread statistics & stack samples**    [\[javarevisi...ogspot.com\]](https://javarevisited.blogspot.com/2026/01/i-tested-30-websites-to-learn-java-here.html)

***

# 🛠️ **6. Running Each Demo in IntelliJ (No Scripts Needed)**

### ✔ A) Run Pinning Demo (expected pinning)

    Right-click → Run PinningDemo

### ✔ B) Run Non‑Pinning Demo (expected clean behavior)

    Right-click → Run NonPinningDemo

### ✔ C) Start the Virtual Thread HTTP Server

    Right-click → Run VtHttpServer

Server starts at:

    http://localhost:8080/health
    http://localhost:8080/work?userId=123

### ✔ D) Run the Load Client

    Right-click → Run LoadClient

This sends N concurrent VT requests.

***

# 📦 **7. Opening .jfr Files in IntelliJ (Correct Method)**

IntelliJ **cannot** open `.jfr` files directly.  
Use **Java Mission Control (JMC)**.

### Steps:

1.  Find your `.jfr` in project `jfr/` folder.
2.  Right-click → **Open in File Manager**
3.  Double-click the `.jfr` file → JMC opens
4.  Now explore:
    *   **Threads**
    *   **Java Monitor Enter**
    *   **VirtualThreadPinned**
    *   **Stack Traces**
    *   **Timeline**

This matches Oracle’s guidance that JMC is the correct tool to analyze JFR data.    [\[javarevisi...ogspot.com\]](https://javarevisited.blogspot.com/2026/01/i-tested-30-websites-to-learn-java-here.html)

***

# 📊 **8. What to Inspect in JMC for Day‑1 Exercises**

### For **PinningDemo**

*   Look for **jdk.VirtualThreadPinned**
*   See long monitor holds via **Java Monitor Enter**
*   Check carrier thread utilization

### For **NonPinningDemo**

*   Pinning events should be minimal or none
*   Blocking I/O shows proper unmounting of VTs

### For **VtHttpServer + LoadClient**

*   Observe:
    *   VirtualThreadStart / End
    *   Mount / Unmount cycles
    *   SocketRead / SocketWrite latencies
    *   Task parallelism (Structured Concurrency)

***

# 🧪 **9. Verification Checklist**

| Task                | Expected Outcome                                    |
| ------------------- | --------------------------------------------------- |
| Run PinningDemo     | Pinning events visible                              |
| Run NonPinningDemo  | No (or few) pinning events                          |
| Start server + load | High VT operations, healthy carrier behavior        |
| Inspect `.jfr`      | Thread samples, monitor events, pinning, I/O events |

***

# 📚 **10. References**

*   **Virtual Threads unload OS carriers on blocking I/O** (Oracle Java 25 docs) [\[slideshare.net\]](https://www.slideshare.net/slideshow/the-future-of-java-records-sealed-classes-and-pattern-matching/251331268)
*   **Virtual Threads scale I/O workloads massively**; pitfalls documented in concurrency guides [\[lucanerlich.com\]](https://lucanerlich.com/java/modern-java-features/)
*   **JEP 491**: synchronized no longer pins VTs in most cases (Java 24+) 
*   **JFR records timestamps and thread events** (Oracle Flight Recorder docs) [\[javarevisi...ogspot.com\]](https://javarevisited.blogspot.com/2026/01/i-tested-30-websites-to-learn-java-here.html)
*   **JFR analysis via JMC recommended** (Baeldung JFR view guide) [\[javaking.com\]](https://javaking.com/free-java-resources/)

***
