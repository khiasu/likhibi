# 🎓 LIKHIBI — MINI PROJECT PRESENTATION & DEFENSE MASTER GUIDE
## B.Tech CSE 7th Sem Major Project Review – I (Mini Project Proposal Defense)

**Project Title**: Likhibi: Computational Resource Curation, Contextual Language Modeling, and Prototype Neural Translation for Nagamese Creole  
**Domain**: Natural Language Processing (NLP) / Machine Learning  
**Target Language**: Nagamese Creole (Lingua Franca of Nagaland, India)  
**Presenter**: Khiasuthong T (Reg No: 2306107010), 7th Sem B.Tech CSE  
**Project Coordinators**: Mr. Nzanthung Odyuo & Mr. Nokshangthemba  

---

## 📖 PLAIN-ENGLISH NLP & LINGUISTICS GLOSSARY

Before presenting, master these terms so you can answer any question with total authority:

* **Nagamese Creole**: A natural language formed when different languages mixed—combining Assamese root words + Naga tribal grammar structure + English/Hindi loanwords. Spoken by over 2–3 million people across Nagaland as an inter-tribal lingua franca.
* **Roman Script**: Writing Nagamese using standard English alphabets (`A-Z`) instead of Assamese/Bengali script. Over 99% of mobile text messaging in Nagaland happens in Roman script.
* **Low-Resource Language (LRL)**: A language with very little digital text available on the internet (unlike English or Spanish). Nagamese had zero digital datasets before this project.
* **Monolingual Corpus**: A single-language text dataset (6,965 Nagamese sentences, 185,945 running tokens) used to calculate word frequencies and train language models.
* **Parallel Corpus**: A bilingual dataset with line-by-line aligned English ↔ Nagamese sentence pairs (6,965 pairs) used for machine translation.
* **Lexicon / Lexical Database**: A structured computer dictionary (`nagamese_lexicon.json`) containing **21,000 validated entries** with lemmas, IPA phonetics, POS categories, definitions, frequencies, and etymology origin tags.
* **Lemma**: The canonical base dictionary form of a word (e.g., `ja` [go] is the lemma for `jabo` [will go] and `jaise` [went]).
* **Code-Switching**: Mixing two languages in one sentence (e.g., *"Moi school jabo"* where *"school"* is English and *"moi/jabo"* is Nagamese).
* **Agglutinative Morphology**: A grammar system where suffix "building blocks" are glued onto a root word (e.g., `manu` + `-khan` = `manukhan` [people], `manu` + `-laga` = `manulaga` [person's]).
* **Anti-Synthetic Purge Filter**: A rule-based filter written in Python to discard non-sensical, artificially generated compound words (e.g., blocking invalid concatenations like `homolaga`).
* **N-Gram Language Model**: A statistical model predicting the next word based on previous $N-1$ words (Unigram = 1 word count, Bigram = 2-word sequence, Trigram = 3-word sequence).
* **Trie Prefix Tree Index**: A character-by-character tree data structure where typing `'j'` $\rightarrow$ `'a'` instantly branches to matching words (`jabo`, `jai`, `jani`) in sub-milliseconds ($O(L)$ time).
* **Add-$k$ Smoothing ($k=0.01$)**: A math formula adjustment. If a user types a word pair never seen in training text, Add-$k$ smoothing adds a tiny fraction ($0.01$) to all counts so unseen words get a tiny non-zero probability instead of crashing the model.
* **Backoff Smoothing**: If a 3-word Trigram context is unseen, the model "backs off" to check the 2-word Bigram probability. If unseen, it backs off to single word Unigram frequencies.
* **Perplexity ($PP = 45.59$)**: The standard NLP metric measuring how "surprised" or confused the model is when guessing the next word. **Lower is better!** 45.59 is very low and demonstrates high predictive certainty.
* **Keystroke Savings (KS %)**: The percentage of touch taps saved by tapping word prediction pills instead of typing every character manually.
* **IME (InputMethodService)**: Android's official system API used to build soft mobile keyboards.

---

# 🖼️ SLIDE-BY-SLIDE PRESENTATION & DEFENSE GUIDE

---

### SLIDE 1: Title & Metadata Slide

#### 📌 Visual Layout
```text
LIKHIBI: A COMPUTATIONAL NATURAL LANGUAGE PROCESSING FRAMEWORK FOR NAGAMESE CREOLE
Domain: Natural Language Processing / Machine Learning
Name: Khiasuthong T | Reg No: 2306107010 | 7th Semester B.Tech CSE
```

#### 🗣️ Verbal Presentation Script
> *"Respected Project Coordinators Mr. Nzanthung Odyuo sir, Mr. Nokshangthemba sir, and evaluation committee members, good morning. I am Khiasuthong T, Registration Number 2306107010, 7th Semester B.Tech CSE. Today, I am presenting my Mini Project proposal titled **'Likhibi: A Computational Natural Language Processing Framework for Nagamese Creole'**. Nagamese is the primary spoken lingua franca across Nagaland with over 30 million daily users, yet it remains a Low-Resource Language in computer science. This project builds foundational computational NLP resources and an offline predictive text engine using a native Android keyboard as our demonstration platform."*

#### 🎯 Slide 1 Teacher Grilling Q&A
* **Q: What does 'Likhibi' mean?**  
  *A*: In Nagamese, 'Likhibi' means 'Please Write' or 'To Write'.
* **Q: Is this an AI/ML project or just an Android app?**  
  *A*: Strictly an AI/ML and NLP project, sir. The Android app is merely Tier 2 of our architecture—an on-device demonstration platform. Tier 1 is our Python NLP Research Pipeline where we extract features, preprocess text, build a 20,000-entry dictionary, and train statistical N-gram language models.
* **Q: Why is Nagamese called a Creole?**  
  *A*: A creole is a stable language formed from parent languages. Nagamese combines Assamese vocabulary roots + Naga tribal grammar structure + English/Hindi loanwords, written digitally in Roman script.

---

### SLIDE 2: Contents (Agenda)

#### 📌 Visual Layout
```text
CONTENTS:
1. Aim & Specific Mini Project Objectives
2. Motivation
3. Literature Survey & Identified Gaps
4. Problem Statement
5. Proposed Methodology: Architecture & Scope Split
6. Proposed Methodology: Data Collection & Preprocessing
7. Proposed Methodology: Feature Extraction & Model Selection
8. Proposed Methodology: Training, Evaluation & Deployment
9. Expected Outcomes
```

#### 🗣️ Verbal Presentation Script
> *"Here is the agenda for today's presentation, structured strictly according to the University ML/AI Project Review guidelines. I will cover our aim and objectives, establish our motivation, survey literature gaps, define the problem statement, and walk through our 4-part Proposed Methodology—from data collection, preprocessing, feature extraction, model selection, training, evaluation, to mobile deployment."*

#### 🎯 Slide 2 Teacher Grilling Q&A
* **Q: Why are 4 slides dedicated to Proposed Methodology?**  
  *A*: Methodology is the core of an ML project. Following coordinator guidelines, we broke it into: (1) Architecture & Scope Split, (2) Data & Preprocessing, (3) Features & Model Selection, and (4) Training, Evaluation & Deployment.
* **Q: Does this cover Mini Project or Major Project?**  
  *A*: Specifically our Mini Project Review-I submission. On Slide 6, we explicitly define the scope split: Mini Project delivers the corpus, 20k dictionary, N-gram model, Trie index, and Android keyboard APK. Neural Machine Translation (NMT) is scheduled for Major Project continuation.

---

### SLIDE 3: Aim & Objectives

#### 📌 Visual Layout
```text
1. AIM: "To design, curate, and implement a foundational NLP resource infrastructure
   for Nagamese Creole, train an offline contextual word-prediction model, and deploy
   a functional initial Android Input Method Editor (IME) keyboard application."

2. OBJECTIVES TABLE:
   • 01. Corpus Acquisition  | Dataset Building | 6.9k Monolingual & 6.9k Parallel Pairs
   • 02. Lexical Database   | Structured Data  | 20,000+ Entry Dictionary (nagamese_lexicon.json)
   • 03. Language Modeling  | NLP Algorithms   | Statistical N-Grams (Add-k Smoothing)
   • 04. Trie Indexing      | Data Structure   | Sub-millisecond Trie Prefix Tree Index
   • 05. On-Device Deploy   | Mobile App       | Android Keyboard APK (<5 MB RAM)
```

#### 🗣️ Verbal Presentation Script
> *"Slide 3 outlines our Aim and 5 concrete Mini Project Objectives. Our Aim is to build computational resources for Nagamese and deploy an offline predictive keyboard. To achieve this, we set 5 measurable objectives: 
> (1) Building a 6,965-line monolingual corpus and parallel dataset, 
> (2) Compiling a 20,000-entry validated digital dictionary, 
> (3) Training statistical N-gram language models, 
> (4) Constructing a sub-millisecond Trie prefix index, and 
> (5) Deploying the models inside an offline Android keyboard APK."*

#### 🎯 Slide 3 Teacher Grilling Q&A
* **Q: Difference between Aim and Objectives?**  
  *A*: Aim is the broad research vision. Objectives are the 5 specific technical deliverables completed to achieve that vision.
* **Q: How did you verify the 20,000 dictionary entries?**  
  *A*: Automated scanner `validate_lexicon.py` checked all 21,000 entries against source corpora and stem lists $\rightarrow$ **21,000 valid entries, 0 invalid entries** in `validation_report.json`.

---

### SLIDE 4: Motivation

#### 📌 Visual Layout
```text
         2M+ Speakers                           0 Existing NLP Tools

  • SOCIOLINGUISTIC REALITY: Nagamese is the inter-tribal lingua franca of Nagaland.
  • LOW-RESOURCE CRISIS: Mobile OS (Android/iOS) offer ZERO native support in Roman script.
  • MOBILE TYPING FRICTION: Aggressive auto-correct errors default to English/Hindi.
  • RESEARCH IMPACT: Establishes first digital datasets, dictionary, and keyboard for Nagamese.
```

#### 🗣️ Verbal Presentation Script
> *"Slide 4 presents our motivation. Over 2 million people speak Nagamese as the inter-tribal lingua franca connecting 16 major Naga tribes. However, smartphone operating systems offer zero native predictive text support in Roman script. When native speakers type, auto-correct aggressively replaces Nagamese words with English or Hindi words—changing 'jabo' to 'jumbo'. Likhibi solves this digital divide by providing native dictionaries and an offline predictive keyboard."*

#### 🎯 Slide 4 Teacher Grilling Q&A
* **Q: Why haven't Google or Apple built a Nagamese keyboard?**  
  *A*: Tech companies prioritize high-resource languages with massive web corpora. Nagamese lacked pre-existing digital datasets. Our project creates those missing foundational datasets.
* **Q: Why Roman script?**  
  *A*: Over 99% of digital messaging in Nagamese occurs in Roman script (English alphabets `A-Z`). Building our model in Roman script reflects actual user behavior.

---

### SLIDE 5: Literature Survey & Problem Statement

#### 📌 Visual Layout
```text
1. LITERATURE SURVEY & GAPS IDENTIFIED:
   • Nagamese Linguistics (Sreedhar 1974, Baishya 2013, Boruah 2018) -> GAP: Purely descriptive paper studies; ZERO digital corpora or code.
   • Low-Resource Indian NLP (Joshi et al. 2020) -> GAP: Northeastern creoles absent from Indic benchmarks (IndicGLUE, Samanantar).
   • Mobile Input Architecture (Fowler et al. 2015, Jurafsky 2023) -> GAP: Zero implementations exist for Nagamese.

2. PROBLEM STATEMENT:
   "Lack of native language models and digital lexicons for Nagamese Creole, causing severe typing friction, aggressive auto-correct errors, and digital exclusion for Nagamese speakers."
```

#### 🗣️ Verbal Presentation Script
> *"Slide 5 summarizes our Literature Survey across three pillars and defines our formal Problem Statement. Linguistics research by Sreedhar and Baishya documented Nagamese grammar on paper, but produced zero code artifacts. Indian NLP research by Joshi et al. highlighted regional data poverty, noting that benchmarks like IndicGLUE exclude Northeastern creoles. Mobile input research by Fowler and Jurafsky established Trie + N-gram architectures for typing, but zero models existed for Nagamese. Our Problem Statement formally defines the typing friction resulting from these gaps."*

#### 🎯 Slide 5 Teacher Grilling Q&A
* **Q: How does your work build on Sreedhar (1974)?**  
  *A*: Sreedhar documented phonetic sounds and grammatical suffixes on paper. We converted those suffix rules (plural `-khan`, genitive `-laga`) into computational pre-processing rules in Python (`morphology_generator.py`).

---

### SLIDE 6: Proposed Methodology – System Architecture & Scope Split

#### 📌 Visual Layout
```text
TWO-TIER MODULAR SYSTEM ARCHITECTURE:
  • Tier 1: Python NLP Research Pipeline (nlp_research/) -> Data preprocessing, Lexical DB (20k), N-Gram LM, Trie Indexer, JSON Asset Export.
  • Tier 2: Android Demonstration Platform (app/) -> Native Kotlin IME Keyboard App (<5 MB RAM).

MINI vs. MAJOR PROJECT SCOPE SPLIT:
  ✅ MINI PROJECT (THIS REVIEW): Monolingual (6.9k) + Parallel Corpus (6.9k), 20k Lexical DB, N-Gram Model, Trie Index, Android IME Keyboard APK Demo.
  🔜 MAJOR PROJECT CONTINUATION: Neural Machine Translation (NMT Nagamese <-> English), Final Polished Release, BLEU Evaluation.
```

#### 🗣️ Verbal Presentation Script
> *"Slide 6 presents our Two-Tier Architecture and Scope Split. Tier 1 is our Python NLP Pipeline handling data preprocessing, dictionary creation, N-gram training, and Trie indexing, serializing models into lightweight JSON assets (`trie_index.json` [0.77 MB] and `bigrams.json`). Tier 2 is our Android Demonstration Platform—a native Kotlin keyboard loading these JSON assets offline under 5 MB RAM. For this Mini Project, the corpus, 20k dictionary, N-gram model, Trie index, and Android keyboard APK are 100% complete. Neural Machine Translation (NMT) is planned as our Major Project continuation."*

#### 🎯 Slide 6 Teacher Grilling Q&A
* **Q: Why decouple into two tiers instead of doing everything in Android?**  
  *A*: Android Kotlin is designed for mobile UI rendering, not heavy NLP research. Training models on phones freezes devices and drains battery. Python handles heavy NLP training on PC and exports lightweight pre-calculated JSON files to the phone.

---

### SLIDE 7: Data Collection & Preprocessing

#### 📌 Visual Layout
```text
STAGE 1: DATA COLLECTION
  • Sources: 26 Scripture PDFs, Web Scrapers (xobdo.org, nagamesekhobor.com), Loanwords list.
  • Size: 6,965 Monolingual Sentences (185,945 tokens), 6,965 Parallel Verse Pairs, 20,000+ Lexicon Entries.

STAGE 2: DATA PREPROCESSING
  • Raw Input -> Cleaner (cleaner.py) -> Tokenizer (tokenizer.py) -> Morphology (morphology_generator.py) -> Anti-Synthetic Purge (builder.py) -> Validated Lexicon
  • Features: Sentence boundary markers (<s>, </s>), Noun cases (-khan, -laga, -ke, -pora, -te), Verb aspects (-se, -bo, -bole, -ina), Purge filter (blocks homolaga).
```

#### 🗣️ Verbal Presentation Script
> *"Slide 7 covers Data Collection and Preprocessing. In Stage 1, we extracted text from 26 scripture books, web glossaries, and loanword lists to produce 6,965 sentences and 6,965 parallel verse pairs. In Stage 2, raw text passes through 4 pre-processing stages: cleaning, custom regex tokenization with boundary markers `<s>` and `</s>`, morphological suffix rules for noun cases and verb aspects, and an anti-synthetic filter that purges invalid compound words. Validation confirmed 21,000 valid dictionary entries with zero invalid entries."*

#### 🎯 Slide 7 Teacher Grilling Q&A
* **Q: What are sentence boundary markers (`<s>`, `</s>`) and why use them?**  
  *A*: `<s>` = start of sentence, `</s>` = end of sentence. Calculating $P(\text{word} \mid \text{<s>})$ teaches the N-gram model which words naturally begin a sentence in Nagamese (such as `moi` or `tai`).

---

### SLIDE 8: Feature Extraction & Model Selection

#### 📌 Visual Layout
```text
STAGE 3: FEATURE ENGINEERING | Unigram/Bigram/Trigram counts, Trie character node paths, candidate scoring.
STAGE 4: DATASET SPLITTING  | 80% Training Set (5,571 sentences) / 20% Evaluation Test Set (1,394 sentences).
STAGE 5: MODEL SELECTION    | Statistical N-Gram LM with Add-k Smoothing (k=0.01) & Backoff + Trie Index.
                              Prefix 'ja' -> Branch 'j' -> Branch 'a' -> Candidates: ['jabo', 'jai', 'jani']
```

#### 🗣️ Verbal Presentation Script
> *"Slide 8 outlines Feature Extraction, Dataset Splitting, and Model Selection. We extract N-gram transition counts and Trie character paths as features. We use an 80/20 train/test split for perplexity evaluation. For Model Selection, we chose Statistical N-Grams with Add-k smoothing ($k=0.01$) and Trie Trees over Deep Learning because N-grams require vastly less training data, operate offline under 5 MB RAM, and deliver sub-2 millisecond search speeds on mobile phones."*

#### 🎯 Slide 8 Teacher Grilling Q&A
* **Q: Write/explain Bigram formula with Add-k smoothing.**  
  *A*: $P(w_i \mid w_{i-1}) = \frac{C(w_{i-1}, w_i) + k}{C(w_{i-1}) + k \cdot |V|}$, where $k=0.01$ and $|V|=3,267$. $k$ adds a tiny fraction so unseen word pairs receive a non-zero probability instead of crashing.
* **Q: What is Backoff smoothing?**  
  *A*: If a 3-word Trigram is unseen, the model backs off to check the 2-word Bigram probability. If unseen, it backs off to Unigram frequencies.

---

### SLIDE 9: Training, Evaluation & Deployment

#### 📌 Visual Layout
```text
STAGE 6: MODEL TRAINING    | Add-k smoothing (k=0.01). Exported Assets: trie_index.json (0.77 MB), bigrams.json.
STAGE 7: MODEL EVALUATION  | Perplexity (PP) = 45.59 (High Predictive Certainty). Keystroke Savings (KS %).
STAGE 8: DEPLOYMENT        | Native Android InputMethodService keyboard app loading assets offline (<10ms, <5MB RAM).
```

#### 🗣️ Verbal Presentation Script
> *"Slide 9 covers Training, Evaluation, and Deployment. Our training pipeline (`train_and_export.py`) fitted N-gram matrices and built our Trie index, exporting compact JSON files (`trie_index.json` at 0.77 MB). Evaluating on a 20% held-out test set, we achieved a Model Perplexity score of 45.59, demonstrating high predictive certainty. For Deployment, these JSON assets are loaded offline by our native Android keyboard APK, achieving sub-10 millisecond predictions under 5 MB RAM."*

#### 🎯 Slide 9 Teacher Grilling Q&A
* **Q: What is Perplexity ($PP=45.59$) and what does it mean?**  
  *A*: Perplexity measures model uncertainty. A score of 45.59 on a 3,267-vocabulary corpus means the model narrows down next-word choices to ~45 weighted candidates, allowing top 3 candidate slots to hit the intended word accurately.
* **Q: Why JSON assets over cloud APIs (like Gemini/OpenAI)?**  
  *A*: Cloud APIs add 300–1000ms network latency, incur API costs, and fail offline. Local JSON assets run in <10ms with zero network requirement.

---

### SLIDE 10: Expected Project Outcomes & Deliverables

#### 📌 Visual Layout
```text
DELIVERABLE                       │ ARTIFACT FILE
──────────────────────────────────┼──────────────────────────────────────
Validated Digital Dictionary      │ nagamese_lexicon.json (20,000+ entries)
Parallel Translation Corpus       │ bible_parallel_corpus.tsv (6,965 pairs)
Contextual Prediction Models      │ trie_index.json (0.77 MB), bigrams.json
Android IME Application           │ Offline keyboard APK (<5 MB RAM)
```

#### 🗣️ Verbal Presentation Script
> *"Slide 10 summarizes our Mini Project deliverables: (1) A 21,000-entry verified digital dictionary (`nagamese_lexicon.json`), (2) A 6,965-pair parallel translation corpus (`bible_parallel_corpus.tsv`), (3) Trained prediction model assets (`trie_index.json` and `bigrams.json`) with perplexity 45.59, and (4) An offline Android keyboard APK. Neural Machine Translation (NMT) and BLEU evaluation are scheduled as our Major Project continuation."*

#### 🎯 Slide 10 Teacher Grilling Q&A
* **Q: What is the ultimate contribution of your project?**  
  *A*: We moved Nagamese from a zero-resource language to a resource-equipped language in computer science. Any future researcher can now use our open datasets (`https://github.com/khiasu/likhibi.git`) to build translation tools, speech recognition, or educational software.

---

### SLIDES 11 & 12: Questions & Thank You

#### 🗣️ Verbal Closing Script
> *"Thank you very much, Mr. Nzanthung Odyuo sir, Mr. Nokshangthemba sir, and committee members. I am ready for your questions."*

---

# ⚡ THE "WHY NOT OTHER OPTIONS?" CHEAT SHEET

| Design Choice Made | Alternative Option | Engineering Rationale |
|---|---|---|
| **Statistical N-Grams + Trie Tree** | **Deep Learning / LSTMs / LLMs** | 1. **Data Efficiency**: N-grams excel on low-resource text (7k sentences vs millions needed for LLMs).<br>2. **Hardware Constraints**: Keyboard must run in **<5 MB RAM** and **<10ms**. LSTMs take >150 MB RAM and 100-300ms per word.<br>3. **Offline**: Runs locally without cloud APIs. |
| **JSON Asset Files** | **SQLite / Room Database** | Reading JSON into memory at app start gives instant **$O(1)$ RAM access**, whereas disk SQL queries add 15-30ms I/O latency per keypress. |
| **Custom Regex Tokenizer** | **Standard NLTK / SpaCy** | NLTK treats Romanized creole hyphens and boundary suffixes incorrectly. Custom regex isolates clean Romanized Nagamese tokens. |
| **Decoupled Two-Tier Architecture** | **Monolithic Android App Code** | Python handles heavy NLP training; Android handles soft keyboard UI. Models can be upgraded in Python without altering Android UI code. |

---

# 🚀 THE SURPRISE ADD-ON Q&A BANK (10 ADVANCED QUESTIONS)

* **Q1: Does your keyboard log or store private user keystrokes?**  
  *A*: No sir. The app operates 100% offline (`INTERNET` permission is omitted). It does not log, store, or transmit any typed text.
* **Q2: What happens when a user types an Out-Of-Vocabulary (OOV) word?**  
  *A*: Trie returns an empty branch. The engine displays the exact typed string in the primary candidate pill and provides high-frequency unigram fallbacks in secondary slots.
* **Q3: Why not fine-tune a small LLM (like Gemma-2B)?**  
  *A*: LLMs require >2 GB RAM and take 300ms+ to generate tokens. A mobile keyboard service must run under 5 MB RAM with <10ms response latency.
* **Q4: How does the keyboard handle numbers and punctuation?**  
  *A*: Numbers and punctuation are stripped during N-gram model training. On Android, tapping punctuation resets the active word prefix and triggers start-of-sentence (`<s>`) probabilities.
* **Q5: Can this framework scale to other Naga languages (Ao, Angami, Sumi, Lotha)?**  
  *A*: Yes. The pipeline is language-agnostic. Feeding a monolingual corpus and word list of Ao or Angami into `cleaner.py` and `builder.py` will generate model assets for that language automatically.
* **Q6: What is the computational complexity of your engine?**  
  *A*: Trie prefix search is $O(L)$ where $L$ is prefix length (<1ms). N-gram context lookup is $O(1)$ constant time in RAM dictionaries.
* **Q7: How is the candidate bar updated without UI lag?**  
  *A*: Lookups execute on lightweight background routines or in-memory Kotlin maps. Candidates are inflated into `ime_view.xml` within 10 milliseconds.
* **Q8: Difference between Tokenization, Stemming, and Lemmatization?**  
  *A*: Tokenization = splitting text into words. Stemming = naively chopping word ends. Lemmatization = mapping inflections (`manukhan`) to valid dictionary lemmas (`manu`).
* **Q9: How will you evaluate translation in the Major Project phase?**  
  *A*: We will train a seq2seq NMT model on `bible_parallel_corpus.tsv` (6,965 pairs) and measure BLEU scores against ground-truth translations.
* **Q10: How will you package the app for Play Store release?**  
  *A*: We will optimize Tier 2 bytecode via ProGuard/R8, embed compressed assets, build an AAB bundle, and publish an open-source release on GitHub and Google Play Store.

---

### 🏆 5 Presentation Day Success Rules
1. **Be Confident**: You built a 21,000-entry validated dictionary, 6.9k corpus, N-gram/Trie models, and an Android APK. That is a massive achievement.
2. **Emphasize 100% Validation**: *"All 21,000 entries were verified with zero invalid entries logged in `validation_report.json`."*
3. **Know Your Key Numbers**: **21,000 entries**, **6,965 sentences / 185,945 tokens**, **Perplexity = 45.59**, **RAM = <5 MB**, **Latency = <10 ms**.
4. **Address Coordinators Respectfully**: Refer to **Mr. Nzanthung Odyuo** and **Mr. Nokshangthemba** by name.
5. **Clarify Scope Split**: *"Mini Project delivers corpus, 20k lexicon, N-gram, Trie, and Keyboard APK today. NMT is scheduled for Major Project continuation."*
