package com.coccoc;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


public class SortMain {

    private static final String TEMP_FOLDER = System.getProperty("java.io.tmpdir") + File.separator + "external_sort";
    public static final String TEMP_FILE_PATTERN = "tempfile_%s";

    private SortMain() {
    }

    public static void sort(String inputFilename, String outputFilename, long limitedMemory) throws IOException {
        File file = new File(inputFilename);
        if (!file.exists()) {
            System.err.println("File " + inputFilename + " no exists");
            return;
        }

        if (file.length() == 0) {
            System.err.println("File " + inputFilename + " empty");
            return;
        }

        File parentFolder = new File(TEMP_FOLDER);
        if (!parentFolder.exists())
            parentFolder.delete();


        List<String> filesList = splitAndSort(file, limitedMemory * 1024*1024);

        mergeKSortedFiles(outputFilename, filesList);

    }

    private static List<String> splitAndSort(File file, long limitedMemory) throws IOException {
        long totalSize = file.length();
        long limitedMemoryCanBeHold = limitedMemory - 1024 * 1024 * 50;//50MB for storing remained thing;
        long sizePerFile = totalSize > limitedMemoryCanBeHold ? limitedMemoryCanBeHold : totalSize;

        int fileCount = 0;
        int totalWriteBytes = 0;
        List<String> filesList = new LinkedList<>();
        List<String> buffer = new LinkedList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                buffer.add(line);
                totalWriteBytes += line.getBytes().length;
                if (totalWriteBytes > sizePerFile) {
                    String tempFilename = TEMP_FOLDER + File.separator + String.format(TEMP_FILE_PATTERN, fileCount);
                    writeAndSortData(buffer, tempFilename);
                    fileCount++;
                    totalWriteBytes = 0;
                    buffer.clear();
                    filesList.add(tempFilename);
                }

            }

        }

        if (!buffer.isEmpty()) {
            String tempFilename = TEMP_FOLDER + File.separator + String.format(TEMP_FILE_PATTERN, fileCount);
            writeAndSortData(buffer, tempFilename);
            filesList.add(tempFilename);
        }

        return filesList;
    }

    private static void writeAndSortData(List<String> data, String filename) throws IOException {
        Collections.sort(data);

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
            for (String l : data) {
                writer.write(l);
                writer.write("\r\n");
            }
        }
    }

    private static void mergeKSortedFiles(String outputFilename, List<String> filesList) throws IOException {
        CircularList<BufferedReader> readerCircularList = new CircularList<>();
        PriorityQueue<String> minHeap = new PriorityQueue<>();

        for (String input : filesList)
            readerCircularList.add(new BufferedReader(new FileReader(input)));

        for (int i = 0; i < filesList.size(); i++)
            minHeap.add(readerCircularList.getNextCircularData().readLine());

        try (PrintWriter outputWriter = new PrintWriter(new FileWriter(outputFilename))) {

            BufferedReader currentReader = null;
            while (!minHeap.isEmpty() || currentReader == null) {
                outputWriter.println(minHeap.poll());
                String line;
                if ((currentReader = readerCircularList.getNextCircularData()) == null)
                    continue;

                if ((line = currentReader.readLine()) != null)
                    minHeap.offer(line);
                else {
                    BufferedReader br = readerCircularList.removeElement(currentReader);
                    if (br != null)
                        br.close();
                }
            }

            for(String filename: filesList)
                Files.delete(Paths.get(filename));

        }

    }


    static class Node<T> {
        T data;

        Node(T data) {
            this.data = data;
        }

        Node<T> next;

    }

    static class CircularList<T> {
        Node<T> head;
        Node<T> current;
        Node<T> tail;

        public void add(T data) {
            Node newNode = new Node(data);
            if (head == null) {
                head = tail = newNode;
                newNode.next = head;
            } else {
                tail.next = newNode;
                tail = newNode;

            }
            newNode.next = head;
            //
            if (head == tail)
                head.next = newNode;
        }

        public T getNextCircularData() {
            if (head == null)
                return null;
            if (current == null)
                current = head;
            Node<T> result = current;
            current = current.next;
            return result.data;
        }

        public T removeElement(T element) {

            if (head == null)
                return null;
            if (current == null)
                current = head;
            Node<T> currentPointer = head;
            Node<T> previous = null;


            while (true) {
                if (element == currentPointer.data) {
                    if(currentPointer == head) {
                        head = head.next;
                        tail.next = head;
                    } else
                        previous.next = currentPointer.next;

                    break;
                }
                previous = currentPointer;
                currentPointer = currentPointer.next;
            }
            if(currentPointer != null)
                return currentPointer.data;
            return null;

        }

    }

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Input filename:");
        String inputFilename = scanner.next();

        System.out.println("Output filename:");
        String outputFilename = scanner.next();

        System.out.println("Limited memory:");
        long limitedMemory = scanner.nextLong();
        System.out.println("Running...");
        long start = System.currentTimeMillis();
        sort(inputFilename, outputFilename, limitedMemory);

        System.out.println("Done! Took " + (System.currentTimeMillis() - start)+  " ms to finish ");

    }
}


