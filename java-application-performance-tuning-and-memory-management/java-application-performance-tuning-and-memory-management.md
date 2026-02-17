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


## Section 5: Chapter 5 - Passing objects between methods

### 21. What does "passing by value" mean?
```java
public class Main {

	
	public static void calculate(int calcValue) {
		calcValue = calcValue * 100;
	}
	
	public static void main(String[] args) {
		int localValue = 5;
		calculate(localValue);
		System.out.println(localValue);

	}

}
```


### 22. What does "passing by reference" mean?
- passing by reference is not allowed in java (byref keyword)


### 23. Passing objects into methods
- for objects passsed into methods, the REFERENCE to object is passed BY VALUE



### 24. The final keyword and why it's not the same as a constant
- The real meaning of the final keyword in Java, when applied to variables, is that the variable can only be assigned once. It is the closest feature Java has to a constant
- A `final` variable can be declared without an initial value and assigned one later, whereas a traditional constant is typically assigned at declaration.

```java
final Customer c = new Customer("John");

final Customer c;
c = new Customer("John");
c = new Customer("Susan");
```

### 25. Why the final keyword doesn't stop an object's values from being changed
When the final keyword is applied to a variable holding an object in Java, it prevents the variable (the reference on the stack) from being reassigned to a different object (on the heap), but it does not prevent the object's internal fields from being changed or "mutated". Therefore, although a variable is declared final, its underlying values can potentially be altered, meaning developers should not be fooled into thinking that final objects cannot be changed. This behavior, where the internal state of a referenced object can change, indicates that Java does not provide const correctness, a powerful feature found in other languages that allows the complete and guaranteed fixing of an object's state


```java
// 1. The Customer Class (allowing mutation of its state)
class Customer {
    private String name;

    // Constructor
    public Customer(String initialName) {
        this.name = initialName;
    }

    // Method to allow changing the internal state (mutator)
    public void setName(String newName) {
        this.name = newName;
    }

    public String getName() {
        return name;
    }
}

public class FinalObjectMutationExample {

    public static void main(String[] args) {

        // Declare a variable 'myCustomer' as final
        // It points to a Customer object named "John"
        final Customer myCustomer = new Customer("John");
        System.out.println("1. Initial Customer Name: " + myCustomer.getName());

        // --- Behavior 1: Attempting to Reassign the Reference (FORBIDDEN) ---
        
        // The final keyword means the variable on the stack cannot be changed 
        // to point to a different object in the heap.
        
        /*
        // UNCOMMENTING THIS LINE WILL CAUSE A COMPILER ERROR: 
        // myCustomer = new Customer("Jane"); 
        // This is not allowed because the reference variable 'myCustomer' is final.
        */
        
        System.out.println("2. Reassignment of the variable 'myCustomer' is prohibited.");

        // --- Behavior 2: Mutating the Object's Internal State (ALLOWED) ---
        
        // We can change the parameters within the object in the heap.
        // This is perfectly valid Java code; there will be no compiler error.
        myCustomer.setName("Susan");

        // The object does indeed change its value.
        System.out.println("3. Mutated Customer Name: " + myCustomer.getName()); 
        
        // The output is now "Susan," demonstrating that the internal state changed 
        // even though the variable 'myCustomer' was declared as final.
    }
}
```

## Section 6: Chapter 6 - Memory exercise 1
### 26. Instructions for the exercise
Open up the **MemoryTest project** – before you run the project, work through the code and **predict the outcome**!

```java
package main;

public class Main {

	public static void main(String args[]) {
		Main main = new Main();
		main.start();
	}
	
	public void start() {
		String last = "Z";
		Container container = new Container();
		container.setInitial("C");
		another(container,last);
		System.out.print(container.getInitial());
	}
	
	public void another(Container initialHolder, String newInitial) {
		newInitial.toLowerCase();
		initialHolder.setInitial("B");
		Container initial2 = new Container();
		initialHolder=initial2;
		System.out.print(initialHolder.getInitial());
		System.out.print(newInitial);
	}
}


package main;

public class Container {
	private String initial = "A";
	
	public String getInitial() {
		return initial;
	}
	
	public void setInitial(String initial) {
		this.initial = initial;
	}
}


```
### 27. Walkthrough of the solution


## Section 7: Chapter 7 - Escaping References
### 28. Introduction - what is an escaping reference?
Escaping references are a potential problem in Java code that occurs when a reference to an object is passed out of the class that was supposed to encapsulate it,. This concept is important for Java programmers to understand, especially in the context of how objects are passed by value.

#### Violation of Encapsulation

The existence of an escaping reference fundamentally **violates the rules of encapsulation**. Encapsulation is a core concept of object-oriented programming, defining the idea that classes contain both data and functionality packaged together, and access to the data within the class must be strictly controlled. When a class is well-defined, the private fields (like `title` or `author` in a `book` class example) are only set via the constructor or specific set methods, making it difficult for values to be accidentally changed from outside the class,.

#### How Escaping References Occur

Escaping references often occur when a method returns an internal, mutable data structure, such as a map or collection,.

1.  **Internal Data:** Consider a `CustomerRecords` class that uses a private map of customer objects internally.
2.  **The Escape:** If this class includes a `Get customers` method, and that method returns a direct reference to the internal records collection, the reference has escaped,.
3.  **External Mutation:** The calling code now obtains a reference to the private collection and can perform any modification it wishes. For instance, external code could call the `clear` method on the returned map, removing all records from the customer collection. This modification is typically not intended by the developer of the class, who likely provided the method only for purposes like iterating through the records. By doing this, the class effectively acts as if the internal map were declared as a public variable.

#### Consequences and Impact

The most significant consequence of an escaping reference is that it makes debugging extremely difficult.

*   If the records collection becomes corrupted or invalid values appear, it is hard to determine where in the code the data was changed.
*   This difficulty arises because **any code anywhere in the project can now access and mutate this collection**.

While avoiding escaping references can require significant effort, and many projects choose not to eliminate them entirely, understanding their potential impact and being able to identify them is critical. Strategies used to avoid them, such as copying collections, sometimes introduce performance considerations that must also be evaluated,.

#### Examples
Good:
```java
public class Book {

    private String title;
    private String author;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Book(String title, String author) {
        this.title = title;
        this.author = author;
    }
}
```


bad:
```java
public class Customer {
    private String name;
    
    // Construtor
    public Customer(String name) {
        this.name = name;
    }

    // Método que a classe CustomerRecords usa
    public String getName() {
        return name;
    }
    
    // Método toString para fácil visualização
    @Override
    public String toString() {
        return "Customer{" + "name='" + name + '\'' + '}';
    }
}

import java.util.HashMap;
import java.util.Map;

public class CustomerRecords {

    // Documentação: Este campo privado armazena os registos de clientes.
    // Usa um Map onde a chave é o nome do cliente (String) e o valor é o objeto Customer.
    private Map<String, Customer> records;

    // Documentação: Construtor da classe.
    // Inicializa o 'records' como um novo HashMap vazio.
    public CustomerRecords() {
        this.records = new HashMap<String, Customer>();
    }

    // Documentação: Adiciona um novo objeto Customer ao mapa 'records'.
    // Usa o nome do cliente (obtido através de c.getName()) como a chave.
    public void addCustomer(Customer c) {
        this.records.put(c.getName(), c);
    }

    // Documentação: Devolve o mapa completo de registos de clientes.
    // Este é o método destacado na imagem.
    public Map<String, Customer> getCustomers() {
        return this.records;
    }
}

CustomerRecords records = new CustomerRecords();

Map<String, Customer> myCustomers = records.getCustomers();

myCustomers.clear();

```

### 29. Strategy 1 - using an iterator
The Solution isn't perfect, but there isn't performance problem:
```java
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CustomerRecords implements Iterable<Customer> {

    private Map<String, Customer> records;

    public CustomerRecords() {
        this.records = new HashMap<String, Customer>();
    }

    public void addCustomer(Customer c) {
        this.records.put(c.getName(), c);
    }

    /*
    public Map<String, Customer> getCustomers() {
        return this.records;
    }
    */

    @Override
    public Iterator<Customer> iterator() {
        return records.values().iterator();
    }
}
```

```java
Iterator<Customer> it = records.iterator();
it.next();
it.remove();
```


### 30. Strategy 2 - duplicating collections
```java
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CustomerRecords implements Iterable<Customer> {

    private Map<String, Customer> records;

    public CustomerRecords() {
        this.records = new HashMap<String, Customer>();
    }

    public void addCustomer(Customer c) {
        this.records.put(c.getName(), c);
    }

    // Método getCustomers() conforme a imagem image_513ca4.png (Devolvendo uma cópia)
    public Map<String, Customer> getCustomers() {
        return new HashMap<>(this.records);
    }

    @Override
    public Iterator<Customer> iterator() {
        return records.values().iterator();
    }
}

class Book {
    
    private String title;
    private String author;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Book(String title, String author) {
        this.title = title;
        this.author = author;
    }
}
```
 

### 31. Strategy 3 - using immutable collections
- Collections.unmodifiableMap(this.records);
- Map.copyOf(records);
```java
public class CustomerRecords implements Iterable<Customer> {

    private Map<String, Customer> records;

    public CustomerRecords() {
        this.records = new HashMap<String, Customer>();
    }

    public void addCustomer(Customer c) {
        this.records.put(c.getName(), c);
    }

    public Map<String, Customer> getCustomers() {
        return Map.copyOf(records);
        // return Collections.unmodifiableMap(this.records);
    }

    @Override
    public Iterator<Customer> iterator() {
        return records.values().iterator();
    }
}
```


### 32. Strategy 4 - duplicating objects
```java
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CustomerRecords implements Iterable<Customer> {

    private Map<String, Customer> records;

    public CustomerRecords() {
        this.records = new HashMap<String, Customer>();
    }

    public void addCustomer(Customer c) {
        this.records.put(c.getName(), c);
    }

    public Map<String, Customer> getCustomers() {
        return Map.copyOf(records);
    }

    @Override
    public Iterator<Customer> iterator() {
        return records.values().iterator();
    }

    public Customer find(String name) {
        return new Customer(records.get(name));
    }
}

class Book {

    private String title;
    private String author;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Book(String title, String author) {
        this.title = title;
        this.author = author;
    }
}

import java.util.Iterator;

public class Main {
    public static void main(String[] args) {
        CustomerRecords records = new CustomerRecords();
        
        records.addCustomer(new Customer("John"));
        records.addCustomer(new Customer("Simon"));
        
        
        records.find("John").setName("Jane");
        
        for (Customer next : records.getCustomers().values())
        {
            System.out.println(next);
        }
        
        System.out.println(records.find("John"));
    }
}
```


### 33. Strategy 5 - using interfaces to create immutable objects
```java
public interface ReadonlyCustomer {
    String getName();
    String toString();
}

public class Customer implements ReadonlyCustomer {
    private String name;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Customer(String name) {
        this.name = name;
    }

    public Customer(ReadonlyCustomer c) {
        this.name = c.getName();
    }

    @Override
    public String toString() {
        return name;
    }
}


import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CustomerRecords implements Iterable<Customer> {
    private Map<String, Customer> records;

    public CustomerRecords() {
        this.records = new HashMap<String, Customer>();
    }

    public void addCustomer(Customer c) {
        this.records.put(c.getName(), c);
    }

    public Map<String, Customer> getCustomers() {
        return Map.copyOf(records);
        //return Collections.unmodifiableMap(records);
    }

    @Override
    public Iterator<Customer> iterator() {
        return records.values().iterator();
    }

    public ReadonlyCustomer find(String name) {
        return new Customer(records.get(name));
    }
}

import java.util.Iterator;

public class Main {

    public static void main(String[] args) {
        CustomerRecords records = new CustomerRecords();

        records.addCustomer(new Customer("John"));
        records.addCustomer(new Customer("Simon"));

        //records.getCustomers().clear();

        ReadonlyCustomer c = records.find("John");
        Customer customer = (Customer)c;
        customer.setName("Jane");

        for (ReadonlyCustomer next : records.getCustomers().values())
        {
            System.out.println(next);
        }

        System.out.println(records.find("John"));

        // for (Customer next : records) {
        //     System.out.println(next);
        // }

        // Iterator<Customer> it = records.iterator();
        // it.next();
        // it.remove();

        // for (Customer next : records) {
        //     System.out.println(next);
        // }
    }
}
```


### 34. Strategy 6 - using modules to hide the implementation
- https://www.udemy.com/course/java-application-performance-and-memory-management/learn/lecture/13918208#overview
- [fonte](./PracticalsAndCode/End%20Of%20Chapter%20Workspaces/Chapter%2007/)


## Section 8: Chapter 8 - Memory Exercise 2
### 35. Instructions for the exercise
Memory Exercise 2

In the BookCatalog project:
- Find and fix the escaping reference in Price
- Find and fix the escaping reference in BookCollection
- Find and fix the escaping reference in Book
- Fix the bug in Price.convert()

[project link](./PracticalsAndCode/Starting%20Workspaces/java11/Chapter%2008/BookCatalog/)


### 36. Walkthrough of the solution
[project link](./PracticalsAndCode/End%20Of%20Chapter%20Workspaces/Chapter%2008/BookCatalog/)



## Section 9: Chapter 9 - The Metaspace and internal JVM memory optimisations
### 37. The role of the Metaspace
Java memory is generally divided into three primary sections: **the stacks, the heap, and the metaspace**. While the internal workings of these areas are highly tuned and complex, understanding their general roles is essential for managing application performance.

#### The Stacks and the Heap
As established in our conversation history, the stacks are thread-specific areas used for local variables and method execution. In contrast, the **heap is "absolutely huge"** compared to the other two sections and serves as the primary storage area for all objects. Our previous discussion noted that even when a variable is declared `final`, the reference remains on the stack while the object's data on the heap remains mutable.

#### The Metaspace: Metadata and Compilation
The metaspace is used primarily to store **metadata**. This includes information about classes and methods, such as whether a method has been compiled into bytecode or native machine code. This area is largely out of reach for programmers, but it plays a vital role in how the JVM manages the "tiered compilation" process discussed previously.

#### Static Variables in Metaspace
The most significant way developers interact with the metaspace is through **static variables**. The metaspace acts similarly to a stack for these variables, but with distinct rules:
*   **Static Primitives:** These are stored **entirely within the metaspace**.
*   **Static Objects:** For objects declared as static (such as a `HashMap`), the **reference lives in the metaspace**, but the actual object and its contents reside on the heap.
*   **Permanence:** Unlike stack variables, which are "popped off" when they go out of scope, variables in the metaspace are **permanent**. They never reach a state where they can no longer be referenced; consequently, any objects on the heap referenced from the metaspace are **never garbage collected**.
*   **Global Access:** Because every thread and class in a Java program has access to the metaspace, static variables can be accessed from any piece of code in the application.

#### Public vs. Static Variables
There is often confusion regarding how the `public` keyword affects memory compared to the `static` keyword. 
*   **Public Variables:** Declaring a variable as public is a matter of **encapsulation and visibility**, not storage. A public variable (that is not static) is still just a reference to an object on the heap, stored in exactly the same way as a private variable. 
*   **Static Variables:** Only the `static` keyword moves the reference (or the primitive value) into the **metaspace**.




### 38. The PermGen
- Permanent generation

This document provides a summary of Java's Permanent Generation (PermGen), a memory space that existed in Java Virtual Machine (JVM) versions 7 and earlier. It has since been replaced by MetaSpace starting with Java 8. The primary issue with PermGen was its fixed size, which could lead to `java.lang.OutOfMemoryError: PermGen space` errors when the space became full. These errors were typically resolved by manually increasing the PermGen size using specific JVM flags (`-XX:PermSize` and `-XX:MaxPermSize`).

These flags are now obsolete. If an application previously configured for Java 7 is migrated to a Java 8 or later environment without updating its startup configuration, the JVM will issue a warning that these flags are invalid and are being ignored. The application will still run without error, and it is considered completely safe to remove these legacy flags.



### 39. Are objects always created on the heap?
Traditionally, Java was designed to simplify development by mandating that all objects be created on the heap, a large memory area designed for objects with variable lifetimes that may need to be shared across different methods. This contrasts with the stack, a highly efficient memory space where data lifetime is tightly coupled to its code block scope.

The central insight is that while programmers are not given a choice in object placement, modern Java Virtual Machines (JVMs) perform a critical internal optimization. If the JVM detects that a newly created object will not be shared and its use is confined entirely within its creation scope, it may allocate that object on the stack instead of the heap. This automatic optimization leverages the stack's efficiency for short-lived, non-shared objects, delivering potential performance benefits transparently and without any change to the developer's code.


### 40. The String Pool
```java
public class Main {

    public static void main(String[] args) {
        String one = "hello";
        String two = "hello";

        System.out.println(one.equals(two)); //true
        System.out.println(one == two); //true

        Integer i = 76;
        String three = i.toString();
        String four = "76";

        System.out.println(three.equals(four)); //true
        System.out.println(three == four); //false

    }

}
```

The comparison `three` == `four` returns **false**. This is because the string `three` was calculated and created as a new object on the heap, while `four` references the object from the String Pool.

Conclusion: "String three has not been placed in the pool because it's been calculated, so it won't be available for reuse." This demonstrates that while the string values are identical, the object references are distinct.



### 41. Interning Strings
The primary function of `intern()` is to manually place a calculated string into the JVM's string pool, enabling its reuse and minimizing the creation of duplicate string objects. While Java automatically interns literal strings, the `intern()` method is specifically for strings generated at runtime. The key benefits of this practice are reduced memory consumption and a lower number of objects for the garbage collector to manage. However, this comes at the cost of the performance overhead of executing the `intern()` method itself, making its application most suitable for situations where a calculated string is reused frequently, such as within a loop. A critical evolution in this area occurred with Java 7, which moved the string pool from the Permanent Generation (PermGen) space to the main heap. This change means that interned strings can now be garbage collected if they are no longer referenced, allowing for more dynamic memory management.



## Section 10: Chapter 10 - Tuning the JVM's Memory Settings
### 42. How the string pool is implemented


### 43. Understanding the size and density of the string pool
- -XX: +PrintStringTableStatistics
- [project link](./PracticalsAndCode/End Of Chapter Workspaces/Chapter 09/ExploringStrings)


### 44. Tuning the size of the string pool
- -XX: +PrintStringTableStatistics -XX: +PrintStringTableSize=120121


### 45. Tuning the size of the heap
- -XX:MaxHeapSize=600m -XX: +PrintStringTableStatistics -XX: +PrintStringTableSize=120121
- -XX:InitialHeapSize=1b -XX: +PrintStringTableStatistics -XX: +PrintStringTableSize=120121
- 

### 46. Shortcut syntax for heap tuning flags
- -XX: +PrintStringTableStatistics 
- -XX:PrintStringTableSize=n

- -XX:MaxHeapSize=n (-Xmx)
- -XX:InitialHeapSize=n (-Xms)

- -XX:UnlockDiagnosticVMOptions
- -XX: +PrintFlagsFinal 


## Section 11: Chapter 11 - Introducing Garbage Collection
### 47. What it means when we say Java is a managed language
- Java works out when objects are no longer needed
- Memory leaks shouldn’t be possible in Java
- Garbage Collection is the process to free up memory


### 48. How Java knows which objects can be removed from the Heap
- Any object on the heap which cannot be reached through a reference from the stack is "eligible for garbage collection".
- circular references aren't eligible for garbage collection


### 49. The System.gc() method
- https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/System.html#gc()
- [project link](./PracticalsAndCode/Starting%20Workspaces/java11/GarbageCollection)
- Remember, the GC method is a hint or a suggestion to the virtual machine and there's no guarantee it would have done. In fact, you might have found on your machine that a garbage collection didn't take place.


### 50. Java 11's garbage collector can give unused memory back to the operating system
- Xms300m (VM args) using java 11.


### 51. Why it's not a good idea to run the System.gc() method



### 52. The finalize() method
- Deprecated since java 9



### 53. The danger of using finalize()
- it is a bad practice



## Section 12: Chapter 12 - Monitoring the Heap
### 54. What is a soft leak?
- soft leaks: when anm object remaind regerenced when no longer needed
- [project link](./PracticalsAndCode/Starting%20Workspaces/java11/Chapter%2012/SoftLeaks/)



### 55. Introducing (J)VisualVM
- https://visualvm.github.io/download.html



### 56. Monitoring the size of the heap over time



### 57. Fixing the problem and checking the heap size
- [project link](./PracticalsAndCode/End%20Of%20Chapter%20Workspaces/Chapter%2012/SoftLeaks/)


## Section 13: Chapter 13 - Analysing a heap dump
### 58. Generating a heap dump
- -XX:+HeapDumpOnOutOfMemoryError
- -XX:+HeapDumpPath=someFilePath


### 59. Viewing a heap dump
- Eclipse Memory Analizer (MAT)


## Section 14: Chapter 14 - Generational Garbage Collection
### 60. How the garbage collector works out what is garbage


### 61. Why the heap is divided into generations
- generation garbage collection
- young and old generation


### 62. The Internals of the Young Generation
- eden
- s0
- s1

### 63. Viewing the generations in VisualVM
- Tools > PLugins > Available Plugins > visual gc



### 64. Viewing the heap when there's a soft leak


## Section 15: Chapter 15 - Garbage Collector tuning & selection
### 65. Monitoring garbage collections
- -verbose=gc
```java
package main;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<Customer> customers = new ArrayList<Customer>();
        while(true) {
            Customer c = new Customer("Matt");
            customers.add(c);
            if (customers.size() > 100)
                for (int i = 0; i < 10; i++)
                    customers.remove(0);
        }
    }

}
```
- -Xmx10m -verbose:gc
- [project link](./PracticalsAndCode/End%20Of%20Chapter%20Workspaces/Chapter%2015/SoftLeaks/src/main/Main.java)


### 66. Turning off automated heap allocation sizing
- -Xmx20m
- -XX:_UseAdaptiveSizePolicy

```bash
jps
jinfo -flag UseAdaptiveSizePolicy 10236
```


### 67. Tuning garbage collection - old and young allocation
- -XX:NewRatio=n

```bash
jinfo -flag NewRatio 10236
```
- -Xms20m -XX:NewRatio=1


### 68. Tuning garbage collection - survivor space allocation
```bash
jinfo -flag SurvivorRatio 9004 
```
- -XX:SurvivorRatio=8
- -Xms20m -XX:NewRatio=1 -XX:SurvivorRatio=5


### 69. Tuning garbage collection - generations needed to become old
```bash
jinfo -flag MaxTenuringThreshold 10940
- -XX:MaxTenuringThreshold=15
```


### 70. Selecting a garbage collector
Types of collectors:
- serial: `-XX:+UseSerialGC`
- parallel: `-XX:+UseParallelGC`
- most concurrent: `-XX:+UseConcMarkSweepGC` `-XX:+UseG1GC`



### 71. The G1 garbage collector
🏗️ The G1 Collector: Java’s Smart Cleaner

In Java, "Garbage Collection" (GC) is the process of automatically clearing memory by removing data the program no longer needs. The **G1 Collector** is the modern standard because it manages memory like a smart organizer rather than a brute-force cleaner.

#### 1. The Neighborhood Strategy (Regions)
Imagine the computer’s memory (the **Heap**) as a massive warehouse. Older systems treated it as one giant room. If you needed to clean it, you had to stop everything and sweep the whole floor.
G1 divides this warehouse into exactly **2,048 small rooms (Regions)**. This allows Java to clean one room at a time without closing the entire warehouse.

#### 2. "Garbage First" Triage
The name "G1" comes from its main rule: **Clean the dirtiest rooms first.**
The collector scans the regions and identifies which ones are mostly "garbage" (unused data). By targeting these first, it reclaims a lot of space very quickly. It focuses on high-yield areas to avoid the dreaded "Stop-the-World" pauses where your app freezes.

#### 3. Fluidity and Flexibility
In G1, rooms are not permanent. A room might hold "new" objects today and "old" objects tomorrow.

* **Eden/Survivor:** Where new data starts.
* **Old Generation:** Where long-lasting data lives.
G1 dynamically reassigns these labels to the 2,048 regions based on what your program needs at that exact moment.

#### 4. The "Just Enough" Approach
Instead of trying to achieve 100% cleanliness and taking a long time, G1 does **Partial Collections**. It cleans just enough regions to keep the app running smoothly. It prioritizes **predictable timing** over total cleanup.

---
🛠️ How to use it

If you use **Java 11 or newer**, G1 is already active! You can optimize it by telling Java your "time budget" for cleaning:

```bash
# Tells Java: "Try not to pause my program for more than 200ms"
java -XX:MaxGCPauseMillis=200 -jar MyProgram.jar

```


### 72. Tuning the G1 garbage collector
- `-XX:ConcGCThreads=n`. Number of the concurrent threads
- `-XX:InitiatingHeapOccupancyPercent=n`. Start with some percent of the memory


### 73. String de-duplication
- -XX:UseStringDeduplication (only available if using GI)
- 
When two strings variables are using the same value, like "One", JVM change the things to the variable to point to the same space allocation in the memory.


## Section 16: Chapter 16 - Using a profiler to analyse application performance
### 74. Introducing Java Mission Control (JMC)
- https://jdk.java.net/jmc/8/


### 75. Building the JMC binaries
- https://github.com/openjdk/jmc


### 76. Running JMC and connecting to a VM
- open the JMC and click in the process on the left side


### 77. Customising the overview tab
- click in green plus and search by *eden
- click in green plus in the top and search by  *sur and go to G1 Survivor Space > PeakUsage > used 


### 78. The MBean Browser tab


### 79. The System, Memory and Diagnostic Commands tabs
- [project link](./PracticalsAndCode/Starting%20Workspaces/java11/Chapter%2016/FibonnaciPrimes/)


### 80. Introducing our problem project
- [project link](./PracticalsAndCode/Starting%20Workspaces/java11/Chapter%2016/FibonnaciPrimes/)
- -XX:+UnlockCommercialFeatures -XX:+FlightRecorder
- -XX:+StartingFlightRecording=delay=2min, duration=60s, name=Test,filename=recording.jfr,settings=profile


### 81. Using the flight recorder
- click right button on My Recording and dump last part


### 82. Analyzing a flight recording
- click in the result to understand


### 83. Improving our application
- [project link](./PracticalsAndCode/Starting%20Workspaces/java11/Chapter%2016/FibonnaciPrimesImproved/)


## Section 17: Chapter 17 - Assessing Performance
### 84. Why benchmarking isn't straight forward.
- microbenchmarking: measuring the performance of a small piece of code


### 85. Setting up the code for benchmarking
- [project link](./PracticalsAndCode/End%20Of%20Chapter%20Workspaces/Chapter%2017/Benchmarking/src/main/NumberChecker.java)
```java
package main;

public class NumberChecker {

	public Boolean isPrime1(Integer testNumber) {
		for (Integer i = 2; i < testNumber; i++) {
			if (testNumber % i == 0) return false;
		}
		return true;
	}
	
	public Boolean isPrime2(int testNumber) {
		int maxToCheck = (int)Math.sqrt(testNumber);
		for (int i = 2; i <= maxToCheck; i++) {
			if (testNumber % i == 0) return false;
		}
		return true;
	}
}
```


### 86. A simple approach to micro-benchmarking
```java
package main;

public class Main {

    public static void main(String[] args) {
        NumberChecker nc = new NumberChecker();

        long start = System.currentTimeMillis();

        for (int i = 1001; i < 2000; i++) {
            System.out.println(nc.isPrime1(22));
        }

        long end = System.currentTimeMillis();
        System.out.println("Time taken " + (end - start) + " ms");
    }
}
```


### 87. Adding in a warm-up period
- `-XX:-TieredCompilation`: diable tired compilation. Normaly JVM uses compilation in level like c1 and c2.
```java
package main;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		NumberChecker nc = new NumberChecker();
		
		//warm up period
		for (int i = 1; i < 10000; i++)
			nc.isPrime1(i);
		
		System.out.println("warmup finished, now measuring");
		
		long start = System.currentTimeMillis();
		
		for (int i = 1; i < 50000; i++)
			nc.isPrime1(i);
		
		long end = System.currentTimeMillis();
		System.out.println("Time taken " + (end - start) + " ms");

	}

}

```

### 88. Comparing two code alternatives
- [project link](./PracticalsAndCode/End%20Of%20Chapter%20Workspaces/Chapter%2017/Benchmarking/src/main/Main.java)
```java
package main;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		NumberChecker nc = new NumberChecker();
		
		//warm up period
		for (int i = 1; i < 10000; i++)
			nc.isPrime2(i);
		
		System.out.println("warmup finished, now measuring");
		
		long start = System.currentTimeMillis();
		
		for (int i = 1; i < 50000; i++)
			nc.isPrime2(i);
		
		long end = System.currentTimeMillis();
		System.out.println("Time taken " + (end - start) + " ms");

	}

}

```
- `-XX:+PrintCompilation -XX:CompileThreshold=1000`


### 89. Using Macro-benchmarking
**Study Note: The Optimization Paradox**

#### Core Concept
The **Optimization Paradox** occurs when improving the execution speed of a specific function (micro-optimization) leads to a decline in the overall application performance (macro-performance). Faster code is not always "better" code if it disrupts the harmony of the entire system.


#### Micro vs. Macro Benchmarking
* **Micro-benchmarking:** Testing a small piece of logic (like a single method) in a controlled, isolated environment. While it provides clean data, it creates a "false sense of security" by ignoring real-world system noise.
* **Macro-benchmarking:** Measuring performance within the context of the full application. This is the **ultimate arbiter of success** because it accounts for how different parts of the code interact.


#### Why "Faster" Code Can Slow Systems Down
1. **Pressure on Downstream Resources:** When Method A is optimized to run faster, it hits Method B or shared resources (like databases or memory) with much higher frequency. This often shifts the bottleneck or creates a new, more expensive one elsewhere—a "Butterfly Effect" in software.
2. **JVM Dynamics:** In environments like the Java Virtual Machine (JVM), the system performs runtime optimizations based on actual usage patterns. Micro-benchmarks are often too "sterile" to trigger these realistic optimizations.


#### Key Takeaway for Beginners
True optimization is about **global efficiency**, not local speed. Always validate your "wins" by testing the entire system's throughput. As an engineer, your goal is the harmony of the stack, ensuring that speeding up one component doesn't overwhelm another.


## Section 18: Chapter 18 - Benchmarking with JMH
### 90. Installing the JMH benchmarking tool
- https://github.com/openjdk/jmh


### 91. Creating and running benchmarks
- java -jar benchmarks.jar


### 92. Using different benchmark modes
- java -jar benchmarks.jar -bm avgt 


## Section 19: Chapter 19 - Performance and Benchmarking Exercise
### 93. Instructions for exercise 1 (creating a flight recording)
- [project link](./PracticalsAndCode/End%20Of%20Chapter%20Workspaces/Chapter%2017/FibonnaciPrimesImproved/src/main/Main.java)


### 94. Walkthrough of the solution & setting up ready for the next challenge
- zmc -vm ~/.asdf/shims/java


### 95. Instructions for exercise 2 (use JMH to macrobenchmark the project)
- Create a second version of generateNextPrime using primatvies 
- Reinstate the original isPrime method, so that there are 2 variants of this method 
- Create 2 variants of the run-methods so that we can compare isPrime1 + generateNextPrime1 against isPrime2 + generateNextPrime2 
- Build a jar file containing the code Use JMH to evaluate the macrobenchmark


### 96. Walkthrough of the solution - part 1 setting up the code



### 97. Walkthrough of the solution - part 2 - integrating into JMH
- [project link](./PracticalsAndCode/End%20Of%20Chapter%20Workspaces/Chapter%2018/jmhBenchmarking/)



## Section 20: Chapter 20 - How Lists Work
### 98. Why it's important to understand how the different List implementations work



### 99. The 8 different list implementations
- ArrayList: 
- CopyOnWriteArrayList
- LinkedList
- AttributeList: a child of ArrayList
- RoleLIst
- RoleUnresolvedList
- Stack
- Vector



### 100. The CopyOnWriteArrayList
- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CopyOnWriteArrayList.html
- Multi-threaded application
- Multiple threads accessing the same list
- Lots of iterations / reads
- Few writes / additions / deletions


### 101. The ArrayList
Types of List:
- ArrayList
- LinkedList
- Stack
- Vector

openjdk-11.0.2/lib/java/src.zip/java.base/java/util/ArrayList.java

In Java, an ArrayList is a dynamic data structure that functions as a resizable container for objects, initially allocating space for ten elements in memory. When this capacity is exceeded, the system automatically handles growth by instantiating a larger array and transferring existing data to the new space. Through an examination of the Java source code, specifically the "grow" method, we can see that the internal storage increases by half of its current size using a bitwise right-shift operator. This process ensures the list can expand indefinitely, while the discarded, smaller arrays are efficiently managed by garbage collection. Ultimately, this mechanism provides a flexible way to manage collections of data without the rigid size constraints of a standard array.


**Conclusion: Thinking Beyond the Default**    
The ArrayList is more than a simple container; it is a highly engineered data structure defined by specific mathematical and architectural trade-offs. By understanding the default capacity of ten, the O(n) cost of reference-array copying, and the 1.5x bitwise growth strategy, you can make much more informed decisions.
The next time you initialize a list where you expect thousands of entries, ask yourself: Should I let Java resize this 20 times, or should I specify an initial capacity to save the CPU from those expensive copy operations? Understanding the mechanics "under the hood" is what separates a coder who just uses the API from an engineer who masters the platform.


### 102. Specifying the initial size of an ArrayList
- [project link](./PracticalsAndCode/End%20Of%20Chapter%20Workspaces/Chapter%2020/ListBenchmarking/src/main/Main.java)

When working with Java's ArrayList, the system must frequently reallocate memory and copy data as the list expands, which can significantly hinder application speed. You can prevent this overhead by providing an initial capacity argument to the constructor, ensuring the internal array is large enough to hold your expected data from the start. This optimization minimizes expensive resize operations and reduces the burden on garbage collection, potentially cutting execution time in half for large datasets. While an ArrayList can technically grow to over two billion items, being proactive about its starting size creates a more performant and memory-efficient program.


### 103. The Vector
 Vector class in Java, comparing its architecture and historical significance to the more common ArrayList. While both utilize a resizable internal array for storage, the Vector is a legacy collection maintained primarily for backwards compatibility with the earliest versions of the language. Its primary technical distinction is that it is thread-safe, providing a built-in mechanism for handling concurrent access that the standard ArrayList lacks. However, because this protection introduces a performance cost, the author suggests that developers should only consider the Vector for specific multi-threaded scenarios where it might outperform modern alternatives.
 

### 104. The Stack
- It is a child of Vector


### 105. The LinkedList
A linked list functions as a dynamic sequence of data where items are organized through a web of connections rather than being stored in a rigid array. Each element is housed within a node object, which acts as a container holding the actual data alongside pointers that reference the neighboring nodes. By maintaining links to both the preceding and succeeding items, the structure allows for seamless navigation in either direction and effortless updates. Because the list grows by simply remapping these pointers when new nodes are added, it bypasses the need for the memory-heavy resizing required by traditional array-based systems.


### 106. Choosing the optimal list type
- ArrayList
  - get ans item in the middle is good. The LinkedList is bad because is necessary to loop every item.
- LinkedList
  - adds a item in the end is good. Only adds a item and updates the last node to points to the new item.
  - adds a item in the beginning is good. Only updates the nodes. In the ArrayList we need mode each element in a new position and after adds the new element.

Summary:    
Based on the sources, here is a comparison table to help you remember the strengths and weaknesses of **ArrayList** and **LinkedList**:

| Feature/Scenario | **ArrayList** (Good / Bad) | **LinkedList** (Good / Bad) |
| :--- | :--- | :--- |
| **Adding to the End** | **Good**: Normally very quick as it just places a pointer in a vacant slot. **Bad**: Can be slow if the array is full and requires a **resizing operation** (creating a new array and copying data). | **Good**: Always quick. Java maintains a reference to the last item, allowing it to go straight there without a performance impact. |
| **Adding to the Start** | **Bad**: Very expensive for large lists; **every existing item** must be shifted down one space to make room. | **Good**: Excellent performance; it is a simple operation that only requires **updating a pointer**. |
| **Retrieval (Get Method)** | **Good**: Superior performance for random access. It can jump directly to any position. In a test of 10 million items, this took only **1 millisecond**. | **Bad**: Very slow for items in the middle or end. Java must **manually navigate** through every preceding node. In the same 10-million-item test, this took **125 milliseconds**. |
| **Removing Items** | **Bad**: Requires **copying and shifting** all subsequent data down one position, creating overhead. | **Good/Bad**: Excellent if removing the **first item**. However, removing an item in the middle can be slow because you must **iterate through the list** to find the item first. |
| **General Use Case** | **The List of Choice**: Best for scenarios prioritizing **data lookup** and retrieval by position. | **Specialized Use**: Best for scenarios involving frequent **structural changes**, especially at the start or middle of the list. |


**Key Takeaway for Memory**
*   **ArrayList** is like an **organized bookshelf**: you can reach any book immediately if you know its number, but adding a new book at the beginning requires sliding every other book over.
*   **LinkedList** is like a **treasure hunt**: adding a new clue at the start is easy, but to find the 50th clue, you must follow the trail through the first 49.



### 107. Sorting lists
- LinkedList don't have sort methos, so uses sort method from List interface that uses Arrays.sort. It's necessary convert to Array before sort
- ArrayList has a sort method and uses Arrays.sort

Certainly! Here is a concise summary of the performance differences between **ArrayList** and **LinkedList** when it comes to sorting in Java.

#### Comparison Summary: Sorting Performance

| Feature | ArrayList | LinkedList |
| --- | --- | --- |
| **Internal Storage** | Uses a dynamic **array** structure. | Uses a chain of **nodes** (pointers). |
| **Sorting Strategy** | Sorts the internal array **directly**. | Must be **converted to an array** first, sorted, then converted back. |
| **Extra Overhead** | **Zero.** No conversion necessary. | **Low.** The time taken to convert is tiny compared to the sort. |
| **Final Efficiency** | High. | **Equivalent** to ArrayList (the difference is usually negligible). |
| **Source of Truth** | Directly utilizes the JDK's optimized `Arrays.sort()` logic. | Relies on the same logic after the conversion step. |


**Key Takeaway**

While the **LinkedList** technically does more "work" by switching data formats, the actual mathematical process of **sorting** is so much more demanding that the time spent converting the list is essentially invisible in most real-world applications.



## Section 21: Chapter 21 - How Maps Work
### 108. How Hashmaps Work - part 1
- HashMap (16 elements on create)
- LinkedHashMap
- TreeMap

**HashMap Essentials**    
*   **Structure:** Stores data using **Unique Keys** and **Values**.
*   **Unique Keys:** Each key points to exactly one value; no duplicates allowed.
*   **Constant Speed:** Retrieval time stays the **same regardless of size**.
*   **Performance:** It is just as fast with 10 items as it is with a billion.
*   **Initial Setup:** Starts in memory as an array of **16 buckets**.
*   **Key Conversion:** Java turns your key into an **integer**.
*   **Modulus Math:** Uses the remainder of a division to find the right bucket.
*   **Bucket Choice:** For 16 buckets, the index will always be between 0 and 15.
*   **Efficiency:** Good **hash codes** are critical for optimal map performance.
*   **Variations:** Other types include **LinkedHashMap**, **Hashtable**, and **TreeMap**.


### 109. The role of the Hashcode
- when the key is a string, we need convert to a integer ans after uses a module (the rest of division) by 16. 
- Use"text".hashCode() to convert


### 110. How Hashmaps Work - part 2
**HashMap Advanced Mechanics**
- **Bucket Structure:** Each bucket is not a single slot but actually a **linked list** of objects.
- **Collisions:** Multiple objects with different hash codes can end up in the **same bucket**.
- **Unique Keys:** While multiple items can share a bucket, a map **never allows duplicate keys**.
- **Initial Size:** A HashMap always starts with an **initial capacity of 16 buckets**.
- **The "Load Factor":** By default, a HashMap is considered full when **75% (3/4)** of buckets are used.
- **Growth Pattern:** When it gets full, a HashMap **doubles its size** (whereas an ArrayList grows by only 50%).
- **Re-hashing:** During resizing, the JVM must **re-evaluate every single item** to determine its new bucket.
- **Performance Cost:** Resizing has a **significant overhead** because items must be moved to different buckets.
- **Linked Storage:** Every entry in the internal array is a **reference to the first item** in that bucket's list.


### 111. Specifying the initial size and factor of a HashMap
- first parameter is the size
- the second parameter is the factor
- Bigger initial size and a larger initial fill factor will speed up the adding of items
```java
package main;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        
        Date start = new Date();
        Map<Integer, Book> books = new HashMap<Integer, Book>(500000, 0.9f);
        for (int i = 0; i < 10000000; i++) {
            books.put(i, new Book(i, "Jane Eyre", "Charlotte Bronte", 14.99));
        }
        
        Date end = new Date();
        System.out.println("Elapsed time was " + (end.getTime() - start.getTime()) + " ms.");
    }
}
```


### 112. HashMap Performance
- We've seen that a bigger initial size and a larger initial fill factor will speed up the adding of items
- But if we want to retrieve items from a HashMap, we actually want the size to be as big as possible because we want the number of items in a linked list to be as small as possible. So actually to optimize this for the purposes of both, we probably want to put in a bigger size.


The performance of a **HashMap** depends on the efficiency of its underlying array compared to the linked lists stored within each "bucket."

**Retrieval Mechanics**    
To find an item, Java calculates the key's **hashCode**, uses a **modulus** calculation based on the array size to find the correct bucket, and then navigates the **linked list** in that bucket to find the matching key.

**Array vs. Linked List Efficiency**    
*   **Arrays:** These are very fast because they occupy **contiguous memory** on the heap, allowing for efficient navigation to a specific place.
*   **Linked Lists:** These are slower because the JVM must **follow each pointer** sequentially (from the first or last entry) to reach a specific item.

**Optimization Strategies**    
To maximize retrieval speed, you want the **underlying array to be as large as possible** to keep the number of items in each linked list as small as possible.
*   **Bigger Initial Size:** Starting with a large size (e.g., 10 million) reduces the need for resizing and keeps buckets shallow.
*   **Smaller Fill (Load) Factor:** Using a smaller factor, such as **0.6**, ensures the map grows before buckets become too crowded.
*   **Result:** In a test with these optimized settings, adding items to a HashMap took only **307 milliseconds**, which was the fastest recorded time.



### 113. The rules for Hashcodes



### 114. Generating and optimising the Hashcode method


### 115. Optimising Hashmap Performance
- https://stackoverflow.com/questions/7115445/what-is-the-optimal-capacity-and-load-factor-for-a-fixed-size-hashmap



### 116. How The LinkedHashMap Works
- HashMap: items iterate in a "random" order
- LinkedHAshMap: items iterate in defined order
- In the LinkedHashMap there are 16 buckets

A LinkedHashMap functions as an extension of a standard hash map, offering the unique advantage of preserving the insertion order of elements during iteration. While a typical hash map returns data in a seemingly random sequence, this version maintains a doubly linked list that runs through all entries to track their chronological addition. Despite this added structural complexity, there is no performance penalty for data retrieval because it utilizes the same bucket-based lookup logic as its parent class. The only minor trade-off for this organized behavior is a slightly higher memory footprint required to store the additional pointers that connect the elements.


### 117. The HashTable and TreeMap
This text evaluates two specialized Java map types by highlighting their specific use cases and inherent trade-offs. The HashTable is described as a legacy, thread-safe alternative to the HashMap, though it generally offers slower performance due to its synchronization. In contrast, the TreeMap is utilized when data must be organized according to the natural ordering of its keys, rather than the order of insertion. While useful for specific organizational needs, the author warns that TreeMaps are computationally expensive and should primarily be reserved for small datasets where sorting is a strict requirement.



