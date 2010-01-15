package org.multiverse.integrationtests.notification;

import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.transactional.collections.TransactionalLinkedList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.sleepMs;

public class ActorLongTest {

    @Test
    public void test() {
        PrintActor actor = new PrintActor();
        TestThread t = threadActor(actor);

        actor.send(new TextMessage("hello"));
        actor.send(new PoisonMessage());

        joinAll(t);
    }

    public class PrintActor extends Actor {

        public void receive(TextMessage msg) {
            System.out.println(msg.text);
        }
    }

    public class TextMessage implements Message {

        public String text;

        public TextMessage(String text) {
            this.text = text;
        }
    }

    //==========================================================

    @Test
    public void test2() {

        SumActor sumActor = new SumActor();
        threadActor(sumActor);

        StartActor startActor = new StartActor(sumActor);
        threadActor(startActor);

        startActor.send(new StartMessage(1, 2));

        sleepMs(1000);
    }

    public class CalcMessage implements Message {

        int a, b;

        Actor sender;
    }

    public class StartActor extends Actor {

        private SumActor sumActor;

        public StartActor(SumActor sumActor) {
            this.sumActor = sumActor;
        }

        public void receive(StartMessage startMsg) {
            CalcMessage msg = new CalcMessage();
            msg.sender = this;
            msg.a = startMsg.a;
            msg.b = startMsg.b;
            sumActor.send(msg);
        }

        public void receive(ResultMessage msg) {
            System.out.println("result was: " + msg.a);
        }
    }

    public class StartMessage implements Message {

        int a, b;

        public StartMessage(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }

    public class SumActor extends Actor {

        public void receive(CalcMessage message) {
            ResultMessage msg = new ResultMessage();
            msg.a = message.a + message.b;
            message.sender.send(msg);
        }
    }

    public class ResultMessage implements Message {

        int a;
    }

    public interface Message {

    }

    public class PoisonMessage implements Message {

    }

    public TestThread threadActor(final Actor actor) {
        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                while (actor.run()) {
                }
            }
        };
        t.start();
        return t;
    }

    public abstract class Actor {

        private final TransactionalLinkedList<Message> mailbox = new TransactionalLinkedList<Message>();

        public boolean run() {
            Message message = mailbox.takeUninterruptible();
            receive(message);
            return !(message instanceof PoisonMessage);
        }

        private void receive(Message message) {
            try {
                Method m = getClass().getMethod("receive", message.getClass());
                m.invoke(this, message);
            } catch (NoSuchMethodException e) {
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public void send(Message msg) {
            mailbox.add(msg);
        }
    }
}
