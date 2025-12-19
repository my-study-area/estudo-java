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

