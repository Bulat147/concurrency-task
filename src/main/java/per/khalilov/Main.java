package per.khalilov;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {
        CrptApi api = new CrptApi(5_000, 3);

        CrptApi.Document document = api.new Document(null,
                "id",
                "id",
                "id",
                true,
                "id",
                "id",
                "id",
                LocalDate.now(),
                "id",
                null,
                LocalDate.now(),
                "id");

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        Callable<String> task = () -> api.createDocument(document);

        List<Future<String>> futures = new ArrayList<>();
        int tasksCount = 10;
        for (int i=0; i<tasksCount; i++) {
            futures.add(executorService.submit(task));
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        int doneCount;
        do {
            doneCount = 0;
            for (Future<String> future: futures) {
                if (future.isDone()) {
                    doneCount++;
                    try {
                        System.out.println(future.get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            System.out.println("\n\n\n");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } while (doneCount != tasksCount);

    }
}
