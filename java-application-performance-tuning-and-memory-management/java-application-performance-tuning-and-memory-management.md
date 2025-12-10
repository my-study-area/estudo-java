# java-application-performance-tuning-and-memory-management

Link: https://www.udemy.com/course/java-application-performance-and-memory-management/?couponCode=MT251110G3

## Introduction
### What do we mean by performance, and what versions of Java does this course cover
- java language (8 and 11)
- JVM

### Example code provided with this course
Source code: ./PracticalsAndCode.zip

### Using different JDK and JVM vendors
- oracle JDK
- openJDK

### The structure of this course
- How the JVM runs your code
- How the JVM manages memory
- Garbage Collection and heap analysis
- Measuring Performance
- How Programming choices impact performance
- The future and other JVM Languages

### How to get support while you're taking this course
- Q & A 

## Just in time compilation and the code cache
### What is bytecode?
- Main.java -> javac -> JVM[Main.class]

### The concept of "Just In Time Compilation"
- JIT compilation
- Native compiled

The provided text explains how the Java Virtual Machine (JVM) initially runs code as an interpreter but utilizes Just-in-Time (JIT) compilation to improve performance. Initially, the JVM is slower than natively compiled languages like C because it interprets code as bytecode, losing the speed advantage of code that runs directly on the operating system. To address this, JIT compilation monitors frequently used code sections, such as methods or loops, and compiles them into native machine code specific to the operating system (e.g., Windows or Mac code), which executes much faster. This entire optimization process is transparent to the user and occurs in a separate thread, meaning the application can continue running interpreted code while the optimized version is being compiled, resulting in applications that generally run faster the longer they are left running. Programmers should be aware of JIT compilation, as code performance assessment may vary depending on whether the code has already been natively compiled.


### Introducing the first example project
java-application-performance-tuning-and-memory-management/PracticalsAndCode/Starting Workspaces/java11/Chapter 02/PerformanceExample1/src/main/Main.java

java-application-performance-tuning-and-memory-management/PracticalsAndCode/Starting Workspaces/java11/Chapter 02/PerformanceExample1/src/main/PrimeNumbers.java
```java
package main;
import java.util.Date;

public class Main {

	public static void main(String[] args) {
		PrimeNumbers primeNumbers = new PrimeNumbers();
		Integer max = Integer.parseInt(args[0]);
		primeNumbers.generateNumbers(max);
	}

}

package main;
import java.util.ArrayList;
import java.util.List;

public class PrimeNumbers {

	private List<Integer> primes;
	
	private Boolean isPrime(Integer testNumber) {
		for (int i = 2; i < testNumber; i++) {
			if (testNumber % i == 0) return false;
		}
		return true;
	}
	
	private Integer getNextPrimeAbove(Integer previous) {
		Integer testNumber = previous + 1;
		while (!isPrime(testNumber)) {
			testNumber++;
		}
		return testNumber;
	}
	
	public void generateNumbers (Integer max) {
		primes = new ArrayList<Integer>();
		primes.add(2);

		Integer next = 2;
		while (primes.size() <= max) {
			next = getNextPrimeAbove(next);
			primes.add(next);
		}
		System.out.println(primes);
	}

}
```

How to execute:
```bash
cd PerformanceExample1
java --module-path bin --module PerformanceExample1/main.Main 8
```

or:
```bash
cd bin
java main.Main 8
```

### Finding out which methods are being compiled in our applications
```bash
java -XX:+PrintCompilation main.Main 10
```

The structure of the JVM flag that is introduced to find out what kind of compilation is occurring is -xx: [+/-]optionName.
1. -xx (Advanced Option): This prefix indicates that the subsequent flag is an advanced option.
2. Colon (:): This separates the advanced option indicator from the control/option name.
3. Plus or Minus (+/-): This is used to indicate whether the option should be switched on (plus) or off (minus).
4. Option Name (e.g., PrintCompilation): This specifies the functionality being controlle


| Coluna/Símbolo | Significado |
| :--- | :--- |
| **First Column** | The number of **milliseconds that have passed since the JVM started**. |
| **Compilation ID** | The **order in which the method or code block was compiled**. Note: These numbers may not appear in sequential order, which could be due to **multithreading issues** or the **complexity or length of the code** being compiled. |
| **n** | Indicates a **native method**. |
| **s** | Indicates a **synchronized method**. |
| **!** | Indicates the method contains **exception handling**. |
| **%** | The code has been **natively compiled** and is now running in a special part of memory called the **code cache**. This means the method is now running in the **most optimal way possible**. |
| **Compilation Level** | A number from **0 to 4**. This number tells what kind of compiling has taken place. **Level 0** means the code was only interpreted, not compiled. **Levels 1 through 4** mean that a **progressively deeper level of compilation** has happened, with Level 4 being the highest possible level of compilation. |
| **Final Column** | The line of code or the **name of the method being compiled**. |


### The C1 and C2 Compilers and logging the compilation activity
Think of JVM tiered compilation like a restaurant kitchen preparing dishes. Initially, a new order (code) might be handled by an apprentice (C1 compiler) using a quick, standard recipe (Tier 1-3). If that dish becomes extremely popular and high-volume (frequently called code), the head chef (C2 compiler) takes over. They refine the recipe (Tier 4 compilation) to be maximally efficient and then place the pre-prepared ingredients (compiled code) in the most accessible spot on the counter (code cache) to ensure the fastest possible service time. The kitchen only focuses this high level of attention on the most popular, complex dishes, not every single order, because time and resources are limited (the tradeoff)

```bash
java -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation main.Main 5000
```

### Tuning the code cache size
The JVM Code Cache: Is This Hidden Limit Degrading Your Java Application's Performance?

1. Introduction: The Silent Performance Killer

It’s a familiar story for many developers working on large-scale Java applications: over time, the system becomes sluggish. Performance degrades, but there are no obvious errors, no crashes, and no clear exceptions in the logs. You've profiled the usual suspects—memory leaks, database queries, network latency—but the root cause remains elusive. This gradual slowdown can often be traced to a critical but frequently overlooked component of the Java Virtual Machine.

This component is the JVM's "code cache," a specialized memory area dedicated to storing highly optimized native machine code. When this cache is misconfigured or becomes full, it can quietly throttle your application's performance without ever throwing a fatal error. Understanding how it works is not just academic; it's a practical necessity for maintaining the health of any significant Java service.

This article will reveal a few critical things every Java developer should know about this crucial component, from what it does to how you can take control of it.

2. Takeaway 1: Your Highly Optimized Code Lives in a Limited-Size 'Cache'

When the JVM identifies "hot" methods that are executed frequently, it uses its C2 compiler (also known as Tier four compilation) to translate the Java bytecode into highly optimized native machine code. To avoid the cost of recompiling this code repeatedly, the JVM stores the resulting native code in a special memory region called the code cache.

The key problem is that this code cache has a limited size. In a large application with many methods that qualify for this level of optimization, the cache can eventually run out of space.

When this happens, the JVM must make a difficult choice. To compile and cache a new hot method, it must first evict an older, already-optimized method. This can lead to a performance-draining cycle where methods are constantly being compiled, cached, evicted, and then re-compiled later when they become hot again. This constant churn consumes CPU cycles and prevents the application from settling into a stable, high-performance state. Conversely, increasing the size of the code cache to prevent this churn can lead to a significant improvement in your application's performance.

3. Takeaway 2: The Most Alarming Warning Message Isn't an Error

When the code cache fills up completely and all the code within it is actively being used, the JVM has no safe methods to evict. Faced with no room to clean up, it takes a drastic step: it simply stops compiling new methods. At this point, you will see a specific warning message appear in your application's console:

Code cache is full, compiler has been disabled.

This warning is particularly insidious because it doesn't stop your application from running. It's not an error or an exception; it's just a notification. However, its implications are severe. It means that from this point forward, no new methods will be optimized to native code, regardless of how "hot" they become. The application is no longer running in its most optimal state. It has silently shifted into a lower gear, and this performance degradation will persist for the remainder of its runtime.

4. Takeaway 3: Default Cache Sizes Have Grown, But You Might Still Be at Risk

The maximum size of the code cache is not a one-size-fits-all value; it depends on the Java version and JVM architecture you are using. The defaults have increased significantly over the years, but older applications or those running in specific environments may still be constrained by smaller limits.

Here are the maximum default sizes based on the Java version:

* Java 7 (or below) 32-bit JVM: 32MB max
* Java 7 (or below) 64-bit JVM: 48MB max
* Java 8 (or above) 64-bit JVM: Up to 240MB max

While modern 64-bit JVMs offer a much larger default cache, it's crucial to be aware of these limits, especially if you are maintaining legacy applications or migrating a large codebase from an older Java version. An application that ran without issue on Java 6 might immediately hit the code cache limit if its complexity has grown over time.

5. Takeaway 4: You Have Precise Control Over the Code Cache

The good news is that the JVM provides a straightforward set of flags to monitor and configure the code cache. You don't have to guess about its status or accept the defaults.

First, you can check the current configuration and usage by adding the -XX:+PrintCodeCache flag when you launch your application. This will print details about the cache, including its total size and how much is currently used. For example, you might see that your cache is 20MB total with only 1MB used. In a complex application, you should become concerned if the used size begins to approach the total size. This is the primary indicator that you may need to tune the cache to avoid performance issues.

To actively tune the cache, you can use three main flags:

* InitialCodeCacheSize: Sets the initial size of the code cache when the application starts. The default size varies based on available memory, but is often around 160kB.
* ReservedCodeCacheSize: Defines the absolute maximum size the cache is allowed to grow to. This is the most important flag for preventing the "compiler has been disabled" warning.
* CodeCacheExpansionSize: Dictates the increment size. Each time the cache needs to grow, it will expand by this amount of memory.

You can set a value for these flags using the syntax -XX:FlagName=Value. The value can be specified in bytes (a number), kilobytes (k), megabytes (m), or even gigabytes (g). These unit letters are case-insensitive, so M and m are equivalent. For example, to set the maximum code cache size to 28 megabytes, you would use the flag: -XX:ReservedCodeCacheSize=28m.

6. Conclusion: Stop Guessing, Start Measuring

The JVM code cache is a powerful performance feature, but its limited size can create a hidden bottleneck in large applications. Ignoring the "Code cache is full, compiler has been disabled" warning is equivalent to knowingly accepting sub-optimal performance.

By understanding the purpose of the code cache and utilizing the JVM flags provided, you can move from guessing to measuring. You have the tools to diagnose potential issues and configure the cache to meet the specific needs of your application, ensuring it runs as efficiently as the JVM intended.

When was the last time you checked the code cache health of your mission-critical applications?

```bash
java -XX:+PrintCodeCache main.Main 5000
java -XX:ReservedCodeCacheSize=28m -XX:+PrintCodeCache main.Main 5000
```

### Remotely monitoring the code cache with JConsole
```bash
jconsole
java -XX:+PrintCompilation main.Main 5000
```

## Section 3: Chapter 3 - Selecting the JVM

### The differences between the 32 bit and 64 bit JVM
**1. Choice and Prerequisites**

When installing a Java virtual machine (at least on Windows and Linux), there is a choice between a 32-bit or a 64-bit version.

*   If you are running on a 32-bit operating system, you **must** choose the 32-bit version.
*   If you are running on a 64-bit operating system, you have the option to choose either version.


**2. Performance and Memory Implications**

If you have the choice between JVM versions, the decision often depends on the memory requirements of the application:

*   **32-bit Speed Advantage:** If an application requires a **heap size less than three gigabytes**, the 32-bit JVM will likely be faster than the 64-bit JVM.
*   **Technical Reason:** This speed difference is based on the fact that each pointer to an object in memory will be 32 bits rather than 64 bits, which makes the manipulation of these pointers quicker.
*   **32-bit Limitations:** To use a 32-bit JVM, the total memory required for the application **must not exceed four gigabytes**. Additionally, if the application is a heavy user of larger numeric types, such as `longs` and `doubles`, the 32-bit JVM might potentially be slower than the 64-bit version.
*   **64-bit Heap Size:** The maximum heap size on the 64-bit JVM is determined by the operating system. On Windows, this is around 1.2GB.
*   **Recommendation:** For smaller applications, developers should not simply default to the 64-bit version; they should **test the performance on both 32-bit and 64-bit** to find the better option.


**3. Compilers and Application Types**

The choice of JVM bit-version significantly impacts which built-in compilers are available:

| JVM Version | Available Compilers | Compiler Names |
| :--- | :--- | :--- |
| **32-bit** | Only C1 | **Client compiler** |
| **64-bit** | Both C1 and C2 | Client compiler (C1) and **Server compiler (C2)** |

The terms *client* and *server* should be understood in the context of how the application is going to work, not the role of the computer itself.

*   **Client Application:** This is defined as an application that is **short-lived** (it runs a process and then finishes). For client applications, **start-up time is critical**. Because they are short-lived, it is unlikely that many methods will run enough times to justify Tier 4 Native code compilation (C2 compilation).
*   **Server Application:** This is defined as an application that is **long-running**, such as a web server. For server applications, start-up time is less important, and what becomes more critical is that the application's **performance is optimized over time**.

In summary, for short-running applications that do not have huge memory requirements, the 32-bit JVM (which only uses the client compiler) may well perform better.


### Specifying which compiler to use at runtime

```java
import java.util.Date;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		Date start = new Date();
		PrimeNumbers primeNumbers = new PrimeNumbers();
		Integer max = Integer.parseInt(args[0]);
		primeNumbers.generateNumbers(max);
		Date end = new Date();
		System.out.println("Elapsed time was " + (end.getTime() - start.getTime()) +" ms.");
	}

}
```

```bash
java -XX:+PrintCompilation main.Main 15000
java -client -XX:+PrintCompilation main.Main 15000
```

### Turning off tiered compilation
```bash
# run using interpreted mode only
java -XX:-TieredCompilation -XX:+PrintCompilation main.Main 15000
```

### Tuning native compilation within the Virtual Machine
```bash
jinfo -flag CICompilerCount

jps

# 10192 was the id process for eclipse
jinfo -flag CICompilerCount 10192

java -XX:+PrintCompilation main.Main 5000

java -XX:+PrintCompilation main.Main 15000

java -XX:CICompilerCount=6 -XX:+PrintCompilation main.Main 15000

jinfo -flag CompileThreshold 10192

java -XX:CICompilerCount=6 -XX:CompileThreshold=1000 -XX:+PrintCompilation main.Main 15000

```


## 📚 Chapter 4 - How memory works - the stack and the heap
### Introduction - the structure of Java's memory
Types of memory in JVM: Stack and Heap


### How the stack works
- stack: 
  - the last item in is the first item out (LIFO)
  - primitive values like int, float and double 
  - references variables and methods frames


### How the heap works
- heap:
  - objects are stored. Like String, INteger and Float
  - can be accessed by multiple threads

### The heap and the stack together - an example



