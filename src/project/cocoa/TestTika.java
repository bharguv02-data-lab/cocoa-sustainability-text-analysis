package project.cocoa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TestTika {

    public static void main(String[] args) {

        Path companyFolder = Paths.get("pdf_company");
        Path nonCompanyFolder = Paths.get("pdf_noncompany");
        Path tikaFolder = Paths.get("tika");

        try {
            Path tikaJar = findTikaJar(tikaFolder);

            if (tikaJar == null) {
                System.out.println("No Tika .jar file found in tika folder.");
                return;
            }

            System.out.println("Tika jar found:");
            System.out.println(tikaJar.toAbsolutePath());
            System.out.println();

            processFolder(companyFolder, "Company", tikaJar);
            processFolder(nonCompanyFolder, "Non-company", tikaJar);

            System.out.println("Text extraction check finished.");

        } catch (IOException | InterruptedException e) {
            System.out.println("Something went wrong.");
            e.printStackTrace();
        }
    }

    private static Path findTikaJar(Path tikaFolder) throws IOException {

        if (!Files.exists(tikaFolder)) {
            return null;
        }

        try (var files = Files.list(tikaFolder)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static void processFolder(Path folder, String folderName, Path tikaJar)
            throws IOException, InterruptedException {

        System.out.println("Reading " + folderName + " PDFs...");
        System.out.println(folder.toAbsolutePath());

        List<Path> pdfFiles = new ArrayList<>();

        try (var files = Files.list(folder)) {
            files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .forEach(pdfFiles::add);
        }

        System.out.println("PDF count: " + pdfFiles.size());
        System.out.println();

        int documentNumber = 1;

        for (Path pdf : pdfFiles) {

            String text = extractTextWithTika(pdf, tikaJar);

            System.out.println(documentNumber + ". " + pdf.getFileName());
            System.out.println("Characters extracted: " + text.length());
            System.out.println();

            documentNumber++;
        }
    }

    private static String extractTextWithTika(Path pdfFile, Path tikaJar)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                tikaJar.toString(),
                "--text",
                pdfFile.toString()
        );

        pb.redirectError(ProcessBuilder.Redirect.DISCARD);

        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        StringBuilder text = new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            text.append(line).append(" ");
        }

        process.waitFor();

        return text.toString();
    }
}
