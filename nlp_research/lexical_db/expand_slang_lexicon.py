"""
Nagamese Slang, Colloquial & Regional Lexicon Expansion Generator.

Expands the Nagamese Lexical Database towards 10,000+ entries by incorporating:
1. Informal slang, everyday idioms, and conversational particles.
2. Regional tribal dialect loanwords & Nagaland cultural vocabulary.
3. Modern English/Hindi loanwords used in daily code-switching.
4. Valid inflected forms for verified Nagamese stems.
"""

import os
import sys
import json
from typing import Set, List, Dict, Any

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

from nlp_research.preprocessing.tokenizer import NagameseTokenizer
from nlp_research.preprocessing.cleaner import TextCleaner
from nlp_research.lexical_db.builder import LexicalDatabaseBuilder

# 1. Nagamese Informal Slang, Conversational Expressions & Colloquial Idioms
INFORMAL_SLANG_VOCAB = [
    "hery", "de", "na", "bhal", "man", "bro", "bos", "chokra", "chokri", "baigan",
    "bolek", "pakila", "dimaag", "jhula", "bekar", "faltu", "chalti", "ghusa",
    "marikene", "sochiko", "kela", "misa", "hosa", "loikene", "bhabikene", "shanti",
    "machan", "bhaiya", "dada", "didi", "bhaido", "mobi", "gaari", "paani", "bazar",
    "dukan", "khana", "dost", "logoter", "machang", "morom", "thik", "chalibo",
    "karone", "khobor", "kiba", "kuntu", "kot", "kile", "etiya", "eti", "itu",
    "otu", "tat", "ia", "solibo", "lokpabo", "ghuri", "ahibole", "jaibole",
    "thakibole", "khabole", "piyabole", "chara", "bepar", "matsa", "kelaa",
    "gila", "golai", "sobi", "ekdum", "misaa", "hosaa", "pagal", "boka",
    "chalu", "shana", "bindas", "faatu", "fatu", "bekaar", "misa-kotha",
    "hosa-kotha", "kotha-ko", "kam-kori", "ghuriko", "boikene", "uthikene"
]

# 2. Nagaland Regional Tribal Dialect Terms, Places & Cultural Lexicon
REGIONAL_CULTURAL_VOCAB = [
    "kohima", "dimapur", "mokokchung", "wokha", "tuensang", "mon", "zunheboto",
    "phek", "peren", "kiphire", "longleng", "niuland", "tseminyu", "chumoukedima",
    "shamator", "angami", "ao", "lotha", "sumi", "konyak", "phom", "chang",
    "sangtam", "yimkhiung", "kuki", "zeliang", "pochury", "rengma", "morung",
    "sekrenyi", "moatsu", "tokhu", "tuluni", "tsungremmong", "ahuna", "hornbill",
    "mithun", "dao", "mekhela", "spear", "kingchilli", "bhootjolokia", "axone",
    "anishi", "zutho", "ruhi", "raja", "gaonbura", "dobashi", "gb", "db",
    "panchayat", "naga", "nagaland", "nagalim", "basha", "suba", "khel"
]

# 3. High-Frequency Modern Loanwords (English & Hindi used in Nagamese Code-Switching)
MODERN_LOANWORDS = [
    "system", "project", "meeting", "result", "exam", "student", "teacher",
    "school", "college", "university", "office", "staff", "job", "salary",
    "money", "bank", "card", "pass", "fail", "online", "website", "group",
    "link", "share", "subscribe", "like", "comment", "story", "status",
    "photo", "video", "camera", "phone", "sim", "recharge", "data", "wifi",
    "bill", "order", "shop", "market", "price", "discount", "bus", "auto",
    "bike", "taxi", "petrol", "driver", "doctor", "medicine", "hospital",
    "police", "court", "case", "news", "paper", "notice", "team", "match",
    "score", "game", "player", "tournament", "win", "loss", "party",
    "program", "function", "stage", "mic", "speaker", "sound", "light",
    "room", "flat", "rent", "key", "lock", "water", "electricity", "power"
]

class SlangLexiconExpander:

    def __init__(self, glossary_path: str = "datasets/raw/vocabulary/regional_nagamese_glossary.txt"):
        self.glossary_path = glossary_path
        self.tokenizer = NagameseTokenizer()

    def run_expansion(self):
        print("Gathering slang, colloquial, and regional Nagamese terms...")
        
        all_new_tokens: Set[str] = set()
        for word in INFORMAL_SLANG_VOCAB + REGIONAL_CULTURAL_VOCAB + MODERN_LOANWORDS:
            tokens = self.tokenizer.tokenize(word)
            for t in tokens:
                if len(t) >= 2 and t.isalpha():
                    all_new_tokens.add(t)

        print(f"Extracted {len(all_new_tokens)} clean slang and regional tokens.")

        # Append to glossary file
        os.makedirs(os.path.dirname(self.glossary_path), exist_ok=True)
        with open(self.glossary_path, "a", encoding="utf-8") as f:
            f.write("\n# Nagamese Informal Slang, Regional Dialects & Modern Loanwords\n")
            for token in sorted(all_new_tokens):
                f.write(token + "\n")

        print(f"Appended slang terms to {self.glossary_path}")

        # Now run database builder with valid inflections to reach 10,000+ target entries
        print("\nRebuilding Lexical Database to target 10,000+ entries...")
        builder = LexicalDatabaseBuilder()
        builder.build_database(
            monolingual_txt_path="datasets/raw/vocabulary/monolingual_nagamese.txt",
            engine_kt_path="app/src/main/java/com/likhibi/nlp/engine/NagameseOfflineEngine.kt",
            glossary_path=self.glossary_path,
            output_json_path="datasets/processed/lexical_database/nagamese_lexicon.json"
        )

if __name__ == "__main__":
    expander = SlangLexiconExpander()
    expander.run_expansion()
