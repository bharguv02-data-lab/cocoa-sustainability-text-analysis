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

public class SensitivityAnalysis {

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

            System.out.println("Starting sensitivity analysis...\n");

            List<String> companyTexts = loadTexts(companyFolder, "Company", tikaJar);
            List<String> nonCompanyTexts = loadTexts(nonCompanyFolder, "Non-company", tikaJar);

            int[] wordLengths = {3, 5, 7};

            for (int minimumWordLength : wordLengths) {

                System.out.println("\n======================================");
                System.out.println("SENSITIVITY ANALYSIS: MIN WORD LENGTH = " + minimumWordLength);
                System.out.println("======================================");

                analyzeTexts(companyTexts, "Company", minimumWordLength, 30);
                analyzeTexts(nonCompanyTexts, "Non-company", minimumWordLength, 30);
            }

            System.out.println("\nSensitivity analysis finished.");

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

    private static List<String> loadTexts(Path folder, String folderName, Path tikaJar)
            throws IOException, InterruptedException {

        System.out.println("Loading " + folderName + " PDFs...");

        List<Path> pdfFiles = new ArrayList<>();

        try (var files = Files.list(folder)) {
            files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .forEach(pdfFiles::add);
        }

        System.out.println("PDF count: " + pdfFiles.size());

        List<String> texts = new ArrayList<>();

        int fileNumber = 1;

        for (Path pdf : pdfFiles) {

            String text = extractTextWithTika(pdf, tikaJar);
            texts.add(text);

            System.out.println(fileNumber + ". Loaded: " + pdf.getFileName()
                    + " | characters: " + text.length());

            fileNumber++;
        }

        System.out.println();

        return texts;
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

    private static void analyzeTexts(List<String> texts, String side, int minimumWordLength, int limit) {

        Map<String, Integer> wordFrequency = new HashMap<>();
        int totalWords = 0;

        for (String text : texts) {

            List<String> words = cleanAndTokenize(text, minimumWordLength);

            totalWords += words.size();

            for (String word : words) {
                wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
            }
        }

        System.out.println("\nTop " + limit + " words: " + side
                + " | minimum word length = " + minimumWordLength);
        System.out.println("Total cleaned words: " + totalWords);
        System.out.println("Total unique words: " + wordFrequency.size());
        System.out.println();

        List<Map.Entry<String, Integer>> topWords = wordFrequency.entrySet()
                .stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());

        int rank = 1;

        for (Map.Entry<String, Integer> entry : topWords) {

            double percentage = (entry.getValue() * 100.0) / totalWords;

            System.out.println(rank + ". " + entry.getKey()
                    + " = " + entry.getValue()
                    + " (" + String.format(Locale.US, "%.3f", percentage) + "%)");

            rank++;
        }
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
                "some", "many", "most", "where", "when", "what", "who", "how", "why",

                // document / web noise
                "page", "pages", "table", "figure", "fig", "chapter", "section",
                "www", "com", "org", "pdf", "http", "https", "doi", "report", "annual",
                "int", "wir", "files", "default", "copyright", "reserved", "seite",
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
                "kakaoforum", "solidaridad", "wageningen", "bmleh", "bmel",

                // extra source-name / extraction-noise terms from TF-IDF cleaning
                "sprüngli", "spruengli", "storck", "guangala", "lante", "echar",
                "blommer", "sucden", "cargill", "nestle", "alfred", "olam",
                "worldcocoafoundation", "earthworm", "gri", "hiap",
                "filename", "dossier", "sosteci", "travaildesenfants",
                "önem", "read", "invite", "promise", "turbulence",
                "disco", "iscos", "dutch", "institute"
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
}