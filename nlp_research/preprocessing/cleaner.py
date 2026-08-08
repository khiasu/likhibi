"""
Nagamese Text Cleaner & Normalizer Module.

Handles cleaning of raw extracted text, normalizing Unicode accents, cleaning punctuation,
standardizing spellings, and tagging loanwords (English/Hindi/Assamese).
"""

import re
import unicodedata
from typing import List, Set

# Common English loanwords used in Nagamese code-switching
COMMON_ENGLISH_LOANWORDS: Set[str] = {
    "school", "college", "office", "doctor", "mobile", "paper", "time", "book",
    "hospital", "police", "car", "bus", "train", "station", "system", "teacher",
    "class", "table", "chair", "pen", "pencil", "market", "shop", "number"
}

# Common Hindi/Assamese loanwords and verb inflections
COMMON_HINDI_ASSAMESE_LOANWORDS: Set[str] = {
    "javo", "jabo", "khabo", "kore", "kori", "dukan", "bazar", "pani", "manu",
    "kaam", "kam", "ghor", "din", "raat", "bhai", "bon", "baba", "ama"
}

class TextCleaner:
    """
    Cleans and normalizes Nagamese text.
    """

    def __init__(self, config=None):
        self.config = config or {}

    def normalize_unicode(self, text: str) -> str:
        """
        Normalizes unicode characters to NFKC standard representation.
        """
        return unicodedata.normalize("NFKC", text)

    def clean_text(self, text: str) -> str:
        """
        Cleans text noise, special quotes, bracketed notes, and irregular spaces.
        """
        if not text:
            return ""

        # Normalize unicode
        text = self.normalize_unicode(text)

        # Replace non-standard quote characters
        text = text.replace("“", '"').replace("”", '"').replace("‘", "'").replace("’", "'")

        # Remove footnote markers or verse bracket tags e.g. [1], (A)
        text = re.sub(r"\[\d+\]", "", text)
        text = re.sub(r"\(\w+\)", "", text)

        # Normalize multiple spaces/newlines
        text = re.sub(r"\s+", " ", text).strip()

        return text

    def get_etymology_tag(self, word: str) -> str:
        """
        Categorizes word etymology origin (English loanword, Hindi/Assamese borrowing, Native).
        """
        w_lower = word.lower()
        if w_lower in COMMON_ENGLISH_LOANWORDS:
            return "English Loanword"
        elif w_lower in COMMON_HINDI_ASSAMESE_LOANWORDS:
            return "Assamese / Hindi Borrowing"
        else:
            return "Nagamese Creole / Native"

if __name__ == "__main__":
    cleaner = TextCleaner()
    sample = "1 Isor laga Putro, Jisu Khrista  laga susamachar..."
    print("Sample Cleaned:", cleaner.clean_text(sample))
    print("Etymology for 'school':", cleaner.get_etymology_tag("school"))
    print("Etymology for 'javo':", cleaner.get_etymology_tag("javo"))
