# DOCS_SIMILARITY

**Document Similarity Analyzer Using Suffix Arrays and Kasai's LCP Algorithm**

DOCS_SIMILARITY is a DSA-3 project that analyzes multiple documents to identify duplicate or highly similar textual content using a custom implementation of Suffix Arrays and the Kasai Algorithm for Longest Common Prefix (LCP) construction.

---

## 1. Abstract

DOCS_SIMILARITY is a document similarity analyzer designed to compare multiple text documents and identify duplicate or highly similar content. The system extracts text from supported document formats, processes it at the word level, identifies common word sequences, calculates similarity scores, and ranks document pairs according to their textual similarity.

Unlike approaches that rely on prebuilt similarity libraries, TF-IDF, or semantic embeddings, the core engine implements Suffix Array construction using the prefix-doubling technique and LCP computation using the Kasai Algorithm. By representing normalized words as integer token IDs, the system identifies meaningful shared word sequences rather than arbitrary character fragments.

The application supports TXT, PDF, and DOCX files. It combines algorithmic processing in a Java Spring Boot backend with a React and Vite frontend to provide document uploading, similarity ranking, common-sequence analysis, and algorithm-related information.

The project demonstrates the application of advanced string-processing algorithms to practical problems such as duplicate document identification, textual overlap analysis, and preliminary plagiarism screening.

## 2. Objectives

The main objective of DOCS_SIMILARITY is to develop an efficient, suffix-based system for analyzing similarity among multiple documents.

### Specific Objectives

- Accept multiple documents and extract their textual content.
- Normalize document text and tokenize it into words.
- Represent words using integer token IDs for algorithmic processing.
- Construct a Suffix Array using the prefix-doubling technique.
- Construct an LCP Array using the Kasai Algorithm.
- Identify common word sequences shared by different documents.
- Merge overlapping matches to reduce duplicate counting.
- Calculate document similarity scores based on shared textual content.
- Rank document pairs from highest to lowest similarity.
- Display matching textual segments for further inspection.
- Demonstrate the practical application of DSA-3 string algorithms.

## 3. Problem Statement

Comparing large documents manually is time-consuming and makes it difficult to identify repeated passages across multiple files. Basic text matching can also produce fragmented results that are less useful for understanding document-level similarity.

The project addresses this problem by implementing suffix-based string-processing algorithms that organize document content, identify common word sequences, and quantify textual overlap. The resulting similarity scores and rankings help users locate document pairs that share substantial amounts of text.

## 4. Proposed Solution

DOCS_SIMILARITY provides a web-based interface for uploading and comparing documents.

The backend extracts and normalizes the text, converts words into integer token IDs, and constructs a combined sequence with unique document boundaries. A Suffix Array organizes the suffixes of this sequence, while the Kasai Algorithm computes the corresponding LCP values.

The system then identifies shared word sequences across document pairs, merges overlapping matches, calculates similarity scores, and ranks the results. The frontend displays the rankings, matching passages, and details of the algorithmic pipeline.

## 5. Features

- **Multiple document uploads:** Compare several documents in a single analysis.
- **Multiple file formats:** Support for TXT, PDF, and DOCX files.
- **Word-level comparison:** Identify shared sequences of words rather than arbitrary character fragments.
- **Custom Suffix Array:** Constructed using the prefix-doubling technique.
- **Kasai LCP construction:** Computes longest common prefixes efficiently.
- **Common-sequence analysis:** Identifies qualifying shared word sequences across documents.
- **Overlap merging:** Combines overlapping matches to reduce duplicate counting.
- **Similarity scoring:** Calculates a percentage score for each document pair.
- **Similarity ranking:** Ranks document pairs by their calculated scores.
- **Matching-content inspection:** Displays common textual segments for selected comparisons.
- **Algorithm visualization:** Presents relevant pipeline and suffix-structure information.
- **Web interface:** Provides an accessible frontend for uploading files and reviewing results.

## 6. Literature Grounding

The following research works provide the theoretical and application-level background for the project's suffix-based approach to document similarity.

| Research Article | Method / Contribution | Key Finding / Conclusion | Relevance to DOCS_SIMILARITY |
|---|---|---|---|
| **Manber & Myers (1993) — "Suffix Arrays: A New Method for On-Line String Searches"** | Introduced Suffix Arrays as a compact alternative to suffix trees for efficient string searching. | Suffix Arrays support efficient string searches while offering practical space advantages over more complex suffix structures. | Provides the foundational data structure used to organize suffixes and support common-substring analysis. [Paper](https://epubs.siam.org/doi/10.1137/0222058) |
| **Kasai et al. (2001) — "Linear-Time Longest-Common-Prefix Computation in Suffix Arrays and Its Applications"** | Presented an algorithm for computing LCP values from a Suffix Array. | LCP construction can be performed in linear time with respect to the sequence length. | Provides the theoretical basis for the project's Kasai LCP implementation. [Paper](https://doi.org/10.1007/3-540-48194-2_17) |
| **Gagie et al. (2017) — "Document Retrieval on Repetitive String Collections"** | Investigated document retrieval over repetitive collections of strings using compressed and suffix-based structures. | Suffix-oriented structures can support document retrieval over collections containing repeated text. | Provides background for applying string-processing structures to collections of documents. [Paper](https://link.springer.com/article/10.1007/s10791-017-9297-7) |
| **Louza et al. (2020) — "gsufsort: Constructing Suffix Arrays, LCP Arrays and BWTs for String Collections"** | Studied practical construction of Suffix Arrays, LCP Arrays, and Burrows–Wheeler Transforms for string collections. | Specialized construction methods make suffix-based processing practical for large collections. | Supports the use of suffix structures for collection-level textual analysis. [Paper](https://doi.org/10.1186/s13015-020-00177-y) |
| **Baba et al. (2017) — "Plagiarism Detection Using Document Similarity Based on Distributed Representation"** | Explored document similarity for plagiarism detection using sequence-based and distributed-representation techniques. | Document similarity methods can help identify potentially plagiarized content. | Provides application-level background for using textual similarity to identify overlapping documents. [Paper](https://doi.org/10.1016/j.procs.2017.06.038) |
| **Hussain & Suryani (2015) — "On Retrieving Intelligently Plagiarized Documents Using Semantic Similarity"** | Investigated semantic similarity for retrieving documents whose wording may differ while retaining similar meaning. | Semantic techniques can help identify cases that exact textual matching may miss. | Highlights a limitation of DOCS_SIMILARITY: it focuses on textual overlap rather than semantic equivalence. [Paper](https://doi.org/10.1016/j.engappai.2015.06.007) |
| **Amirzhanov et al. (2025) — "Plagiarism Types and Detection Methods: A Systematic Survey of Algorithms in Text Analysis"** | Surveyed plagiarism types and text-analysis approaches, including string matching and semantic methods. | Different forms of plagiarism may require different detection techniques. | Provides broader context for the project's focus on duplicate and highly textually similar documents. [Paper](https://doi.org/10.3389/fcomp.2025.1504725) |

### Overall Literature Conclusion

The reviewed literature establishes the importance of Suffix Arrays and LCP structures for efficient string processing and highlights the application of textual similarity techniques to duplicate detection and plagiarism analysis.

Semantic methods address a different problem: detecting similarity when the wording changes but the meaning remains similar. DOCS_SIMILARITY concentrates on exact normalized word-sequence overlap, using Suffix Arrays, the Kasai Algorithm, common-sequence analysis, and similarity ranking.

## 7. Design Methodology

The system follows a sequential processing pipeline.

### Step 1: Input Documents

Users upload multiple supported documents through the frontend. Each document is associated with its own identifier so that the source of a matching sequence can be determined.

### Step 2: Text Extraction

The backend extracts plain text from the uploaded files:

- **TXT:** Reads the text content directly.
- **PDF:** Uses Apache PDFBox to extract text.
- **DOCX:** Uses Apache POI to extract document text.

### Step 3: Text Preprocessing

The extracted text is normalized and split into words. The resulting word tokens are represented using integer IDs, allowing the suffix-based algorithms to operate on a sequence of tokens.

### Step 4: Combined Sequence Construction

The token sequences from the documents are combined into a single sequence. Unique boundary sentinels are used to distinguish documents and prevent matches from incorrectly crossing document boundaries.

### Step 5: Suffix Array Construction

The system constructs a Suffix Array using the prefix-doubling technique. It repeatedly sorts suffixes using progressively larger prefixes until their relative ordering is established.

### Step 6: LCP Array Construction

The Kasai Algorithm computes the LCP Array using the constructed Suffix Array. The LCP values represent the lengths of common prefixes between adjacent suffixes in suffix-array order.

### Step 7: Common Word Sequence Analysis

The system examines suffix and LCP information to identify qualifying shared word sequences belonging to different documents. The minimum qualifying sequence length is three words.

### Step 8: Overlap Merging

Overlapping matching intervals are merged to reduce repeated counting of the same matched regions.

### Step 9: Similarity Calculation

The system calculates a similarity percentage for each document pair using the amount of shared textual content relative to the combined word counts of the two documents.

### Step 10: Ranking and Results

The document pairs are sorted by similarity score. Users can inspect the ranking and review the common word sequences identified for selected pairs.

## 8. Algorithms and Data Structures

### 8.1 Word Tokenization and Integer Encoding

Document text is normalized and divided into word tokens. Each normalized word is represented by an integer token ID.

This representation allows the Suffix Array and LCP algorithms to process words as sequence elements instead of comparing raw character strings.

### 8.2 Suffix Array

A Suffix Array stores the starting positions of all suffixes of a sequence in lexicographical order.

For a sequence of length \(n\), the project's prefix-doubling approach has a target time complexity of \(O(n\log n)\) under the implementation's sorting assumptions.

The Suffix Array organizes suffixes so that common prefixes can be identified efficiently.

### 8.3 Kasai Algorithm and LCP Array

The Longest Common Prefix Array stores the common-prefix length between adjacent suffixes in Suffix Array order.

The Kasai Algorithm constructs the LCP Array in \(O(n)\) time after the Suffix Array has been constructed.

In DOCS_SIMILARITY, the LCP values are interpreted at the word level because the underlying sequence contains integer word IDs.

### 8.4 Common Word Sequence Analysis

The analyzer uses the Suffix Array, LCP values, and document-boundary information to identify shared sequences across different documents.

A minimum qualifying length of three words helps exclude very short matches that are less informative.

### 8.5 Overlap Merging

Multiple detected sequences can overlap. The merging stage combines overlapping intervals so that the same matched region is not repeatedly counted as independent shared content.

### 8.6 Similarity Score

For a document pair \(A\) and \(B\), the similarity score is calculated using:

\[
\text{Similarity}(A,B)=
\frac{2\times\text{SharedWords}}
{\text{Words}_A+\text{Words}_B}\times100
\]

Where:

- \(\text{Words}_A\) is the number of normalized word tokens in document \(A\).
- \(\text{Words}_B\) is the number of normalized word tokens in document \(B\).
- \(\text{SharedWords}\) represents the shared word count derived from the merged matching intervals, with the pairwise shared count bounded by the matched counts in the two documents.

The result is expressed as a percentage. A higher score indicates greater measured textual overlap under the project's scoring method.

### 8.7 Document Boundaries

Unique boundary sentinels distinguish the individual documents within the combined sequence. This prevents a common sequence from being treated as a valid match when it exists only because the end of one document is adjacent to the beginning of another.

## 9. System Architecture

The project follows a frontend–backend architecture.

**Frontend**

- React
- Vite
- JavaScript
- CSS

The frontend handles document uploads, result presentation, similarity rankings, matching-sequence inspection, and algorithm-related information.

**Backend**

- Java
- Spring Boot
- Maven
- Apache PDFBox
- Apache POI

The backend handles document extraction, tokenization, suffix-based processing, LCP construction, common-sequence analysis, similarity calculation, and result generation.

**Processing flow**

`Document Upload → Text Extraction → Normalization → Word Tokenization → Integer Encoding → Combined Sequence → Suffix Array → Kasai LCP → Common Sequence Analysis → Overlap Merging → Similarity Scoring → Ranking`

## 10. Technology Stack

| Component | Technology |
|---|---|
| Programming language — backend | Java |
| Backend framework | Spring Boot |
| Build and dependency management | Maven |
| Frontend library | React |
| Frontend development server and build tool | Vite |
| Document extraction — PDF | Apache PDFBox |
| Document extraction — DOCX | Apache POI |
| Version control | Git |
| Repository hosting | GitHub |
| Development environment | Visual Studio Code or another compatible IDE |

## 11. Project Structure

The repository is organized into backend, frontend, and sample-document directories.

```text
DOCS_SIMILARITY/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── pom.xml
│   └── mvnw
├── frontend/
│   ├── src/
│   ├── package.json
│   └── index.html
├── sample-documents/
├── .gitignore
└── README.md
```

The exact files and directories may vary slightly as the project evolves.

The backend's principal algorithmic responsibilities are separated into components such as:

- `WordTokenizer.java` — word normalization and tokenization.
- `TokenCombiner.java` — combining token sequences with document boundaries.
- `SuffixArray.java` — Suffix Array construction.
- `KasaiLCP.java` — LCP Array construction.
- `CommonWordSequenceAnalyzer.java` — identification of shared word sequences.
- `CommonSequenceMerger.java` — merging overlapping matches.
- `DocumentSimilarityService.java` — coordinating the analysis and similarity calculation.

## 12. Setup and Execution Instructions

### Prerequisites

Install the following:

- Java Development Kit (JDK) compatible with the backend; Java 17 or later is the intended baseline.
- Node.js and npm.
- Git.
- A terminal and a code editor.

### Clone the Repository

```bash
git clone https://github.com/siribillakanti/KLH-CSE-2026-27-TEAM-07-DOCS_SIMILARITY.git

cd KLH-CSE-2026-27-TEAM-07-DOCS_SIMILARITY
```

### Start the Backend

Open a terminal and execute:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows, use the Maven wrapper's Windows command if available:

```powershell
mvnw.cmd spring-boot:run
```

The Spring Boot backend is configured to run on port `8080` in the local development setup.

If port `8080` is already occupied, stop the existing process or configure a different port before starting the application.

### Start the Frontend

Open a second terminal from the repository root:

```bash
cd frontend
npm install
npm run dev
```

Open the local URL printed by Vite in the terminal. The default development URL is:

`http://localhost:5173`

Keep both development servers running while testing the locally hosted application.

### Running the Application

1. Start the backend.
2. Start the frontend.
3. Open the frontend in a browser.
4. Upload two or more supported documents.
5. Run the comparison.
6. Review the ranked document pairs and their similarity scores.
7. Select a pair to inspect common textual sequences and related analysis details.

## 13. Input and Output

### Input

The application accepts:

- TXT documents.
- PDF documents containing extractable text.
- DOCX documents.

The configured upload limits are 25 MB per file and 100 MB per request.

Scanned or image-only PDFs that do not contain extractable text are not supported by the current text-extraction pipeline. Optical Character Recognition (OCR) is outside the current scope.

### Output

The application presents:

- Document-pair similarity scores.
- Ranked similarity results.
- Common word sequences identified between documents.
- Details of selected document comparisons.
- Information about the processing pipeline and suffix-based algorithms.

## 14. Testing and Evaluation

The system should be evaluated using documents with known relationships and expected textual overlap.

Recommended test cases include:

- Two documents containing identical text.
- Two documents containing a shared passage.
- Documents containing only short common phrases.
- Documents with different content.
- Multiple documents with different levels of overlap.
- Documents in different supported file formats.
- Documents containing punctuation, repeated words, and different whitespace patterns.
- Inputs at or near the configured upload limits.
- Empty, malformed, or unsupported files.

Evaluation should examine the correctness of suffix ordering, LCP values, common-sequence detection, overlap merging, similarity-score calculation, ranking, and document-boundary handling.

Actual test results should be recorded from the test runs performed on the final project version.

## 15. Technical Soundness and Limitations

### Technical Strengths

- Implements Suffix Array construction and Kasai LCP computation as core algorithms.
- Applies the algorithms to word-level document comparison.
- Separates document extraction from the suffix-based processing engine.
- Preserves document boundaries in the combined sequence.
- Merges overlapping matches before similarity scoring.
- Provides a frontend for inspecting results rather than returning only a numerical score.

### Limitations

- The analyzer focuses on normalized textual overlap, not semantic similarity.
- Paraphrased text with substantially different wording may not be detected as similar.
- Scanned PDFs without extractable text are not supported because OCR is not included.
- Similarity scores depend on the implementation's tokenization, matching, and scoring rules.
- A high textual-similarity score indicates substantial overlap; it does not by itself establish plagiarism or prove that copying occurred.

## 16. Applications

Potential applications include:

- Duplicate document identification.
- Preliminary plagiarism screening.
- Comparison of academic documents.
- Identification of repeated passages across reports.
- Analysis of overlapping text in document collections.
- Preliminary document grouping based on textual overlap.

These are potential applications of the implemented textual-comparison approach, not a claim that the system provides a complete plagiarism-detection solution.

## 17. Course Alignment

DOCS_SIMILARITY aligns with the DSA-3 TextHack framework by applying string-processing algorithms to a practical document-analysis problem.

The project demonstrates:

- Suffix Array construction.
- Prefix-doubling techniques.
- LCP Array construction.
- Kasai Algorithm.
- Common-substring and word-sequence analysis.
- Algorithmic complexity analysis.
- Application of data structures to a real-world problem.
- Integration of algorithmic processing with a web application.

## 18. Team Details

| Team Member | Role |
|---|---|
| Billakanti Siri | Team Lead Project development and documentation |
| Bhavana Kondakrindi | Frontend developer |
| Aniketh Pani | sample datasets, testing |

**Project Supervisor:** Dr.Ch.Anuradha

## 19. Conclusion

DOCS_SIMILARITY demonstrates how Suffix Arrays and the Kasai Algorithm can be applied to document-level textual comparison. By processing documents at the word level, identifying common sequences, merging overlapping matches, and calculating similarity scores, the system provides an interpretable approach to identifying duplicate and highly similar textual content.

The project combines core DSA concepts with a Java Spring Boot backend and a React-based frontend. It illustrates the practical use of string-processing algorithms while clearly distinguishing exact textual overlap from semantic similarity and definitive plagiarism detection.

## 20. References

1. Manber, U., & Myers, G. (1993). *Suffix Arrays: A New Method for On-Line String Searches*. SIAM Journal on Computing. https://epubs.siam.org/doi/10.1137/0222058
2. Kasai, T., Lee, G., Arimura, H., Arikawa, S., & Park, K. (2001). *Linear-Time Longest-Common-Prefix Computation in Suffix Arrays and Its Applications*. https://doi.org/10.1007/3-540-48194-2_17
3. Gagie et al. (2017). *Document Retrieval on Repetitive String Collections*. https://link.springer.com/article/10.1007/s10791-017-9297-7
4. Louza et al. (2020). *gsufsort: Constructing Suffix Arrays, LCP Arrays and BWTs for String Collections*. https://doi.org/10.1186/s13015-020-00177-y
5. Baba et al. (2017). *Plagiarism Detection Using Document Similarity Based on Distributed Representation*. https://doi.org/10.1016/j.procs.2017.06.038
6. Hussain & Suryani (2015). *On Retrieving Intelligently Plagiarized Documents Using Semantic Similarity*. https://doi.org/10.1016/j.engappai.2015.06.007
7. Amirzhanov et al. (2025). *Plagiarism Types and Detection Methods: A Systematic Survey of Algorithms in Text Analysis*. https://doi.org/10.3389/fcomp.2025.1504725
