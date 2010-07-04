package org.multiverse.integration.org.multiverse.integration.examples

import org.multiverse.transactional.refs.LongRef
import static org.multiverse.integration.MultiverseGroovyLibrary.*

public class BankAccountTransferTest extends GroovyTestCase {

  def a, b, c

  void setUp()
  {
    a = new Account(balance: 5, name: "A")
    b = new Account(balance: 0, name: "B")
    c = new Account(balance: 0, name: "C")
  }

  void testInitialize(){
    assertEquals 5, a.balance
    assertEquals 0, b.balance
    assertEquals 0, c.balance
  }

  void testShouldPerformAccountTransferAtomically() {
    def transfer_from_A_to_B = Thread.start {
      transfer(a, b, 5, 50)
    }

    def transfer_from_A_to_C = Thread.start {
      transfer(a, c, 5, 1)
    }

    transfer_from_A_to_B.join();
    transfer_from_A_to_C.join();
    
    assertEquals 0, a.balance
    assertEquals 0, b.balance
    assertEquals 5, c.balance
  }

  private static def transfer(from, to, amount, delay) {
    atomic() {
      if (from.balance >= amount){
        sleep delay
        from.balance -= amount
        to.balance += amount
      }
    }
  }

}

class Account {
  LongRef balance = new LongRef();
  String name = ""

  long getBalance() {balance.get()}

  void setBalance(long newBalance) {
    this.balance.set(newBalance);
  }

}