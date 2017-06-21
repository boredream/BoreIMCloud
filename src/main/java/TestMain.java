import java.util.concurrent.atomic.AtomicInteger;

public class TestMain {

    private static AtomicInteger i = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        for (int m = 0; m < 100000; m++) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    plus();
                }
            };
            thread.start();
        }

        Thread.sleep(1000);

        System.out.println(i);
    }

    private static void plus() {
        i.addAndGet(1);
    }
}
