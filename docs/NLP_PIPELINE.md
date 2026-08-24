# LIKHIBI — Natural Language Processing (NLP) Pipeline

## 1. End-to-End Pipeline Overview

```
[Raw PDFs / Scraped Text]
          │
          ▼
 [nlp_research/preprocessing/pdf_extractor.py]
          │
          ▼
 [Monolingual Corpus: 185k tokens] ──► [Parallel Corpus Aligner: 6,965 pairs]
          │                                           │
          ▼                                           ▼
 [Lexicon Builder: 21k words]                [Neural Machine Translation (NMT)]
          │
          ├──────────────────────────┐
          ▼                          ▼
 [Character-Level Trie]     [Statistical Trigram/Bigram/Unigram Models]
          │                          │
          └───────────┬──────────────┘
                      ▼
         [assets/models/ payloads]
                      │
                      ▼
           [Android IME Keyboard]
```

---

## 2. Statistical Language Modeling Equations

### Maximum Likelihood Estimation with Add-$k$ Smoothing ($k = 0.01$)
$$P_{\text{add-}k}(w_i \mid w_{i-1}) = \frac{C(w_{i-1}, w_i) + k}{C(w_{i-1}) + k \cdot |V|}$$

### Perplexity Calculation
$$\text{PP}(W) = \exp\left( -\frac{1}{N} \sum_{i=1}^N \ln P(w_i \mid w_{i-1}) \right)$$
* **Current Evaluated Perplexity**: **45.59** on held-out test split.

---

## 3. Contextual Dual-Ranked Scoring Formula

When a user types prefix $P$ under context $(w_{i-2}, w_{i-1})$:
$$\text{Score}(W) = \text{TrieFreq}(W) + \beta_{\text{trigram}} \cdot P(W \mid w_{i-2}, w_{i-1}) + \beta_{\text{bigram}} \cdot P(W \mid w_{i-1}) + \gamma \cdot \text{UserFreq}(W)$$
* Where $\gamma = 10,000$ (highest priority for personalized user words)
* Where $\beta_{\text{trigram}} = 5,000$ (high priority for 2-word context match)
