# Episode 10 demos

Captured on Java 24.0.2. Every number below came from running the code or from
`git diff --no-index --stat`, not from counting by hand.

One checkout system, written twice. `tangled/` is one class that does everything.
`clean/` is the same behaviour, factored. Both produce identical business output.

```bash
cd tangled && java Main.java
cd clean   && java Main.java
```

## Before any change

| | lines | files |
|---|---|---|
| tangled | 103 | 6 |
| clean | 157 | 13 |

**The clean version is 52 percent more code in 2.2 times the files.** Anyone who
tells you SOLID makes the code smaller has not measured it.

## Change 1: add UPI as a payment method

```bash
git diff --no-index --stat tangled add-upi/tangled
git diff --no-index --stat clean   add-upi/clean
```

```
tangled   Main.java         | 2 +-
          OrderService.java | 9 +++++++++
          2 files changed, 10 insertions(+), 1 deletion(-)

clean     Main.java         |  4 ++--
          UpiPayment.java   | 16 ++++++++++++++++
          2 files changed, 18 insertions(+), 2 deletions(-)
```

**The clean version wrote more lines, 18 against 10.** That is not the point.

| | lines of existing logic modified |
|---|---|
| tangled | 9, spread over three places inside the method that also opens the database and sends the email |
| clean | 0 |

In the clean version, 12 of the 13 existing files came out byte identical.
`CardPayment.java` and `NetBankingPayment.java` were never opened. The only
file touched was `Main.java`, which is the wiring, and that is the job a
framework like Spring does for you.

## Change 2: marketing wants different email wording

```
tangled   OrderService.java  | 4 +++-   3 insertions, 1 deletion
clean     EmailNotifier.java | 5 +++--  3 insertions, 2 deletions
```

Almost the same number of lines. **The difference is which file.** A wording
change from the marketing team edited the payment logic file. That is what
"more than one reason to change" costs in practice.

## Liskov: the subclass that breaks its parent's promise

```bash
cd proofs && java LiskovProof.java
```

One test, written once against the parent's promise:

```
card         contract holds? true
netbanking   contract holds? true
gift card    contract holds? false
```

The damage is not a tidy exception. A refund batch written against the parent
type stops halfway:

```
refunded Rs 500.00 by card
STOPPED: gift cards are not refundable
batch died after 1 of 3. Rs 500.00 already left the account and the rest
never ran. Nobody wrote a bug. The subclass broke a promise.
```

## Interface segregation: counted, not argued

```bash
cd proofs && java FatInterfaceProof.java
```

```
                  charge      refund      schedule    splitAcross
card              works       works       works       works
upi               works       works       throws      throws
cash on delivery  works       throws      throws      throws

12 method implementations had to be written.
7 do something. 5 exist only to throw, which is 41 percent.
```

The compiler allowed all of it, because every class really does implement every
method. Split the interface into `PaymentMethod`, `Refundable`, `Schedulable`
and `Splittable`, and the same mistake stops being a production crash:

```bash
cd proofs/segregated && javac *.java
```

```
Bad.java:5: error: incompatible types: UpiPayment cannot be converted to Splittable
        System.out.println(SplitTheBill.run(new UpiPayment(), 3, 50000));
                                            ^
1 error
```

Methods in the segregated version that exist only to throw: **0**.

## Dependency inversion: can the thing be tested at all

```bash
cd tangled && java TangledTest.java
cd clean   && java CleanTest.java
```

Both timings are taken after a warm up run, so this is the work and not class
loading.

| | tangled | clean |
|---|---|---|
| time for one checkout | 301 ms | 158 microseconds |
| real assertions possible | 1, that it did not throw | 5 |
| needs a database | yes | no |
| sends a real email | yes | no |

**About 1,900 times faster**, and that ratio is the least interesting part. The
tangled test cannot check the fee, because the number went into a database and
was never handed back. The clean test checks the fee, the method, the order id,
the email address and the gateway reference, because the fakes kept them.

## The honest summary

SOLID did not make this code shorter. It made it 52 percent longer. What it
changed is where a change lands: in a new file instead of inside working code,
and in the file that owns the concern instead of the file that owns everything.
