package org.multiverse.integration.org.multiverse.integration.examples

import org.multiverse.transactional.refs.BooleanRef
import static org.multiverse.integration.MultiverseGroovyLibrary.*
import org.multiverse.transactional.refs.IntRef;

/**
 * Created by IntelliJ IDEA.
 * User: hari
 * Date: 19 Sep, 2010
 * Time: 9:53:12 AM
 * To change this template use File | Settings | File Templates.
 */

public class DiningPhilosphersTest extends GroovyTestCase {
  Philosopher godel, escher, bach, aristotle, digkstra
  Chopstick chopstick1, chopstick2, chopstick3, chopstick4, chopstick5
  IntRef food;

  void setUp()
  {
    food = new IntRef(5);
    chopstick1 = new Chopstick(id: 1, free: new BooleanRef(true))
    chopstick2 = new Chopstick(id: 2, free: new BooleanRef(true))
    chopstick3 = new Chopstick(id: 3, free: new BooleanRef(true))
    chopstick4 = new Chopstick(id: 4, free: new BooleanRef(true))
    chopstick5 = new Chopstick(id: 5, free: new BooleanRef(true))
    godel = new Philosopher(name: "godel", leftChopstick: chopstick5, rightChopstick: chopstick1, food: food);
    escher = new Philosopher(name: "escher", leftChopstick: chopstick1, rightChopstick: chopstick2, food: food);
    bach = new Philosopher(name: "bach", leftChopstick: chopstick2, rightChopstick: chopstick3, food: food);
    aristotle = new Philosopher(name: "aristotle", leftChopstick: chopstick3, rightChopstick: chopstick4, food: food);
    digkstra = new Philosopher(name: "digkstra", leftChopstick: chopstick4, rightChopstick: chopstick5, food: food);
  }

  void testNoPhilosopherShouldStarve() {
    def threads = [];

    for (philosopher in [godel, escher, bach, aristotle, digkstra]) {
      Thread thread = new Thread(philosopher);
      threads.add(thread)
      thread.start();
    }

    for (thread in threads) {
      thread.join();
    }

    //Assertion is non deterministic
/*    for (philosopher in [godel, escher, bach, aristotle, digkstra]) {
      assertFalse("${philosopher.getName()} has starved", philosopher.isStarved());
    }*/
  }
}

class Philosopher implements Runnable {

  String name;
  Chopstick leftChopstick;
  Chopstick rightChopstick;
  IntRef numberOfTimesEaten = new IntRef();
  IntRef food;

  void eat() {
    atomic(trackreads: true, explicitRetryAllowed: true) {
      leftChopstick.free.await(true);
      rightChopstick.free.await(true);
      if (food.get() > 0) {
        leftChopstick.take()
        rightChopstick.take()
        numberOfTimesEaten.inc();
        sleep 10
        food.dec();
      }
    }
  }

  void think() {
    atomic(trackreads: true, explicitRetryAllowed: true) {
      leftChopstick.release()
      rightChopstick.release()
    }
    sleep 10
  }

  void run() {
    live()
  }

  private def live() {
    10.times {
      eat()
      think()
    }
  }

  boolean isStarved() {
    return numberOfTimesEaten.get() == 0;
  }
}

class Chopstick {
  int id;
  BooleanRef free;

  void take() {
    free.set(false);
  }

  void release() {
    free.set(true);
  }

}