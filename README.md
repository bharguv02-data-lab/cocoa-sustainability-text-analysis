# Cocoa Sustainability: Farmer Living Income Analysis

A text-mining study comparing how chocolate manufacturers and external stakeholders (NGOs, policy bodies and sector initiatives) talk about farmer living income in cocoa supply chains.

*Big Data Analysis Project, FOM University of Applied Sciences (June 2026). Paper: "Farmer Living Income Priorities in Cocoa Sustainability".*

## The question

How do manufacturers frame farmer living income, how do external stakeholders frame it, and where do the two align or differ?

## Corpus

51 publicly available PDF documents:

- **26 company documents:** sustainability and cocoa progress reports from manufacturers such as Ferrero, Lindt & Sprüngli, Nestlé, Mars Wrigley, Mondelēz, Barry Callebaut, Hershey, Ritter Sport and Cargill
- **25 external documents:** from organisations such as Fairtrade International, OECD, GISCO, ICCO, the European Commission (JRC), the VOICE Network Cocoa Barometer, IDH and Solidaridad

The PDFs are not included in this repository for copyright reasons. The full list is in [corpus.md](corpus.md).

## Pipeline (Java + Apache Tika)

| Step | Class | What it does |
|---|---|---|
| 1 | `TestTika` | Checks that Apache Tika extracts text from every PDF in both groups |
| 2 | `WordFrequencyAnalysis` | Raw word frequencies for company and external documents |
| 3 | `CleanWordFrequencyAnalysis` | Word frequencies after removing stopwords and noise |
| 4 | `TFIDFAnalysis` | TF-IDF to find the terms that characterise each group |
| 5 | `SensitivityAnalysis` | Re-runs the analysis with different minimum word lengths to check the results hold |

The results were then interpreted with a thematic analysis.

## Key findings

- **Broad alignment:** both groups strongly discuss cocoa, farmers, supply chains, sustainability, human rights, child labour, deforestation and responsible sourcing.
- **Different framing of living income:** manufacturers focus on programs and supply chain activities such as sourcing, traceability, agroforestry, emissions and farmer support. External stakeholders focus directly on living income, price, costs, household welfare, due diligence, benchmarks and accountability.
- **Implication:** manufacturers should link their sustainability programs more clearly to farmer income outcomes and pricing transparency.

## Run it

1. Download `tika-app-3.3.0.jar` from [Apache Tika](https://tika.apache.org/download.html) into `tika/`.
2. Put the company PDFs into `pdf_company/` and the external PDFs into `pdf_noncompany/`.
3. Open the folder as a Java project in Eclipse (or compile with `javac`) and run the classes in the order above. Results are printed to the console.
