autoscale: true
footer: Purely Fonctional IO
slidenumbers: true

# [fit] **Purely Fonctional IO**
## in _Scala_ and _ZIO_

---

# Exceptions are Impure

---

# Mutability is Impure

---

# Traditional IOs are Impure

---

# `IO[E, A]`

```scala
IO[E, A] // E = Error, A = Result
```

* An immutable object that **describes** an **action performing side-effects**
* To actually do something, it must be interpreted by a runtime system
* When **run**, it will either
    - fail with an **error** of type **`E`**,
    - or run forever,
    - or eventually produce a **result** of type **`A`**.

---

# `IO` is Pure


* `IO` values are **pure** :innocent:
* They can be **combined** to form complex programs
* They can be **inlined** or **extracted** without changing the meaning of the code
* A **program** can be represented as a **single `IO` value**
* Program `IO` value can eventually be run in the `main` method
* This would be the only **impure** :imp: part of the code  

---

Pure Values — IO.point
Synchronous Effects — IO.sync
Asynchronous Effects — IO.async
Concurrent Effects — io.fork
Resource Effects — io.bracket
The concurrency model is based on fibers, a user-land lightweight thread, which permit cooperative multitasking, fine-grained interruption, and very high performance with large numbers of concurrently executing fibers.

IO values compose with other IO values in a variety of ways to build complex, rich, interactive applications. See the methods on IO for more details about how to compose IO values.

In order to integrate with Scala, IO values must be interpreted into the Scala runtime. This process of interpretation executes the effects described by a given immutable IO value. For more information on interpreting IO values, see the default interpreter in RTS or the safe main function in App.

---

# Minimalistic App

```scala
class HelloWorldApp extends App {
  def run(args: List[String]): IO[Nothing, ExitStatus] = {
    helloWorld.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))
  }

  def helloWorld: IO[IOException, Unit] = {
    for  {
      _ <- putStrLn("What's you name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello $name!")
    } yield ()
  }
}
```

---

# Types in `for` Comprehension

```scala
```

---

# Scopes in `for` Comprehension

```scala
```

---

# Implicit Nesting in `for` Comprehesion

```scala
```
