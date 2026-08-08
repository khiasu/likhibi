"""
Nagamese Morphological Expansion Generator Module.

Applies standard Nagamese morphological inflection rules to expand base lemmas:
- Noun inflections: -khan (plural), -laga (possessive), -ke (accusative), -pora (ablative), -te (locative).
- Verb inflections: -se (past), -bo (future), -bole (infinitive), -i-na (participle), -thaki (continuous).

This tool scales the core lexicon towards the ~20,000 entry target.
"""

from typing import List, Dict, Set

# Standard Nagamese nominal suffixes
NOUN_SUFFIXES = [
    ("khan", "Plural suffix"),
    ("laga", "Possessive case suffix"),
    ("ke", "Accusative/Dative case suffix"),
    ("pora", "Ablative/Instrumental case suffix"),
    ("te", "Locative case suffix")
]

# Standard Nagamese verbal suffixes
VERB_SUFFIXES = [
    ("se", "Past tense suffix"),
    ("bo", "Future tense suffix"),
    ("bole", "Infinitive suffix"),
    ("ina", "Participle suffix"),
    ("thaki", "Continuous aspect suffix")
]

class NagameseMorphologyGenerator:
    """
    Generates valid morphological inflections for base Nagamese lemmas.
    """

    def expand_noun(self, lemma: str) -> List[Dict[str, str]]:
        """
        Generates inflected noun forms for a base noun lemma.
        """
        results = []
        for suffix, desc in NOUN_SUFFIXES:
            inflected = f"{lemma}{suffix}"
            results.append({
                "inflected_form": inflected,
                "base_lemma": lemma,
                "suffix": suffix,
                "grammatical_function": desc
            })
        return results

    def expand_verb(self, base_stem: str) -> List[Dict[str, str]]:
        """
        Generates inflected verb forms for a base verb stem.
        """
        results = []
        for suffix, desc in VERB_SUFFIXES:
            inflected = f"{base_stem}{suffix}"
            results.append({
                "inflected_form": inflected,
                "base_lemma": base_stem,
                "suffix": suffix,
                "grammatical_function": desc
            })
        return results

if __name__ == "__main__":
    generator = NagameseMorphologyGenerator()
    print("Sample Noun Expansion for 'manu':")
    for item in generator.expand_noun("manu"):
        print(f"  {item['inflected_form']} ({item['grammatical_function']})")

    print("\nSample Verb Expansion for 'ja':")
    for item in generator.expand_verb("ja"):
        print(f"  {item['inflected_form']} ({item['grammatical_function']})")
