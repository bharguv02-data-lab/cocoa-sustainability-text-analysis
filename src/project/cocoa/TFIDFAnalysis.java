package project.cocoa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TFIDFAnalysis {

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

            System.out.println("Starting TF-IDF analysis...\n");

            List<DocumentData> allDocuments = new ArrayList<>();

            allDocuments.addAll(loadDocuments(companyFolder, "Company", tikaJar));
            allDocuments.addAll(loadDocuments(nonCompanyFolder, "Non-company", tikaJar));

            System.out.println("Total documents loaded: " + allDocuments.size());
            System.out.println();

            Map<String, Integer> documentFrequency = computeDocumentFrequency(allDocuments);

            Map<String, Double> companyTfidf = computeAverageTfidfForSide(
                    allDocuments, documentFrequency, "Company"
            );

            Map<String, Double> nonCompanyTfidf = computeAverageTfidfForSide(
                    allDocuments, documentFrequency, "Non-company"
            );

            printTopTfidf(companyTfidf, "Company", 50);
            printTopTfidf(nonCompanyTfidf, "Non-company", 50);

            System.out.println("\nTF-IDF analysis finished.");

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

    private static List<DocumentData> loadDocuments(Path folder, String side, Path tikaJar)
            throws IOException, InterruptedException {

        System.out.println("Loading " + side + " PDFs...");

        List<Path> pdfFiles = new ArrayList<>();

        try (var files = Files.list(folder)) {
            files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .forEach(pdfFiles::add);
        }

        System.out.println("PDF count: " + pdfFiles.size());

        List<DocumentData> documents = new ArrayList<>();

        int fileNumber = 1;

        for (Path pdf : pdfFiles) {

            String text = extractTextWithTika(pdf, tikaJar);
            List<String> words = cleanAndTokenize(text, 3);

            Map<String, Integer> wordCounts = new HashMap<>();

            for (String word : words) {
                wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
            }

            DocumentData document = new DocumentData(
                    pdf.getFileName().toString(),
                    side,
                    wordCounts,
                    words.size()
            );

            documents.add(document);

            System.out.println(fileNumber + ". Done: " + pdf.getFileName()
                    + " | cleaned words: " + words.size());

            fileNumber++;
        }

        System.out.println();

        return documents;
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

    private static List<String> cleanAndTokenize(String text, int minimumWordLength) {

        Set<String> stopWords = new HashSet<>(Arrays.asList(
                // basic English stopwords
                "the", "and", "for", "that", "with", "this", "from", "are", "was", "were",
                "has", "have", "had", "not", "but", "all", "can", "will", "its", "into",
                "also", "more", "than", "which", "each", "such", "been", "being", "they",
                "them", "their", "there", "these", "those", "our", "you", "your", "about",
                "between", "through", "within", "without", "over", "under", "per", "only",
                "other", "based", "however", "should", "could", "would", "may", "might",
                "some", "many", "most", "where", "when", "what", "who", "how", "why", "sprüngli", "spruengli", "storck", "guangala", "lante", "echar",
                "blommer", "sucden", "cargill", "nestle", "alfred", "olam",
                "worldcocoafoundation", "earthworm", "gri", "hiap",
                "filename", "dossier", "sosteci", "travaildesenfants",
                "önem", "read", "invite", "promise", "turbulence",
                "disco", "iscos", "dutch", "institute",

                // document / web noise
                "page", "pages", "table", "figure", "fig", "chapter", "section",
                "www", "com", "org", "pdf", "http", "https", "doi", "report", "annual", "int", "wir", "files", "default", "copyright", "reserved", "seite",
                "downloads", "slide", "panel", "producfon", "certi",
                "ferrerosustainability", "cocoasector", "cocoainitiative",
                "nestlecocoaplan", "zwolsoliv",

                // German basic words appearing from some reports
                "und", "der", "die", "das", "den", "dem", "des", "ein", "eine", "einer",
                "eines", "mit", "für", "von", "zu", "zur", "zum", "auf", "im", "ist",
                "sind", "werden", "wird",

                // organization / company name noise
                "ferrero", "fazer", "barry", "callebaut", "hershey", "lindt", "sprngli",
                "sprungli", "ritter", "valrhona", "mars", "mondelz", "mondelez", "meiji",
                "natra", "pladis", "maestrani", "tradin", "nestl", "nestlé", "fairtrade",
                "icco", "voice", "idh", "oecd", "swissco", "gisco", "isco", "jrc",
                "kakaoforum", "solidaridad", "wageningen", "bmleh", "bmel"
        ));

        String lowerText = text.toLowerCase(Locale.ROOT);

        String cleanedText = lowerText.replaceAll("[^\\p{L}\\s]", " ");

        String[] words = cleanedText.split("\\s+");

        List<String> finalWords = new ArrayList<>();

        for (String word : words) {
            if (word.length() >= minimumWordLength && !stopWords.contains(word)) {
                finalWords.add(word);
            }
        }

        return finalWords;
    }

    private static Map<String, Integer> computeDocumentFrequency(List<DocumentData> documents) {

        Map<String, Integer> documentFrequency = new HashMap<>();

        for (DocumentData document : documents) {

            Set<String> uniqueWordsInDocument = document.wordCounts.keySet();

            for (String word : uniqueWordsInDocument) {
                documentFrequency.put(word, documentFrequency.getOrDefault(word, 0) + 1);
            }
        }

        return documentFrequency;
    }

    private static Map<String, Double> computeAverageTfidfForSide(
            List<DocumentData> allDocuments,
            Map<String, Integer> documentFrequency,
            String side
    ) {

        Map<String, Double> totalTfidfScores = new HashMap<>();

        int totalDocuments = allDocuments.size();
        int sideDocumentCount = 0;

        for (DocumentData document : allDocuments) {

            if (!document.side.equals(side)) {
                continue;
            }

            sideDocumentCount++;

            for (Map.Entry<String, Integer> entry : document.wordCounts.entrySet()) {

                String word = entry.getKey();
                int wordCount = entry.getValue();

                double termFrequency = wordCount / (double) document.totalWords;

                int docsContainingWord = documentFrequency.getOrDefault(word, 1);

                double inverseDocumentFrequency = Math.log(totalDocuments / (double) docsContainingWord);

                double tfidf = termFrequency * inverseDocumentFrequency;

                totalTfidfScores.put(
                        word,
                        totalTfidfScores.getOrDefault(word, 0.0) + tfidf
                );
            }
        }

        Map<String, Double> averageTfidfScores = new HashMap<>();

        for (Map.Entry<String, Double> entry : totalTfidfScores.entrySet()) {
            averageTfidfScores.put(entry.getKey(), entry.getValue() / sideDocumentCount);
        }

        return averageTfidfScores;
    }

    private static void printTopTfidf(Map<String, Double> tfidfScores, String side, int limit) {

        String title = "Top " + limit + " average TF-IDF terms: " + side;

        System.out.println("\n==============================");
        System.out.println(title);
        System.out.println("==============================");

        List<Map.Entry<String, Double>> topTerms = tfidfScores.entrySet()
                .stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());

        int rank = 1;

        for (Map.Entry<String, Double> entry : topTerms) {

            String line = rank + ". " + entry.getKey()
                    + " = " + String.format(Locale.US, "%.6f", entry.getValue());

            System.out.println(line);
            rank++;
        }
    }

    private static class DocumentData {

        String fileName;
        String side;
        Map<String, Integer> wordCounts;
        int totalWords;

        DocumentData(String fileName, String side, Map<String, Integer> wordCounts, int totalWords) {
            this.fileName = fileName;
            this.side = side;
            this.wordCounts = wordCounts;
            this.totalWords = totalWords;
        }
    }
}