"""
Lexical Database Schema Definition for Nagamese.

Defines the target structure for the ~20,000 Nagamese validated entries, including:
- Native Nagamese lemmas and creole words (~17,000 entries)
- English & Hindi borrowed loanwords (~2,000-3,000 entries) frequently used in code-switching (e.g. "Moi school javo")
"""

from dataclasses import dataclass, field
from typing import List, Optional

@dataclass
class LexicalItemSchema:
    """
    Python data class representation of a structured Nagamese dictionary entry.
    
    :param etymology_origin: Language origin tag e.g. "English Loanword", "Hindi Loanword", "Assamese Creole", "Indigenous Naga".
    """
    id: str
    lemma: str
    phonetic_ipa: Optional[str] = None
    pos_category: Optional[str] = None
    english_definition: Optional[str] = None
    etymology_origin: Optional[str] = None
    orthographic_variants: List[str] = field(default_factory=list)
    usage_examples: List[str] = field(default_factory=list)
    frequency_count: int = 0
    is_validated: bool = False
