"""
Nagamese PDF Text & Verse Extractor Module.

Ingests book-wise Nagamese New Testament PDFs from `datasets/raw/parallel/nagamese_nt_pdfs/`,
extracts page texts, cleans formatting artifacts, parses verse numbers & chapter headings,
and exports structured JSON and raw monolingual text.
"""

import os
import re
import json
import pypdf
from typing import Dict, List, Any

BOOK_MAPPING = {
    "nag_MRK.pdf": ("Mark", "MRK"),
    "nag_LUK.pdf": ("Luke", "LUK"),
    "nag_JHN.pdf": ("John", "JHN"),
    "nag_ACT.pdf": ("Acts", "ACT"),
    "nag_ROM.pdf": ("Romans", "ROM"),
    "nag_1CO.pdf": ("1 Corinthians", "1CO"),
    "nag_2CO.pdf": ("2 Corinthians", "2CO"),
    "nag_GAL.pdf": ("Galatians", "GAL"),
    "nag_EPH.pdf": ("Ephesians", "EPH"),
    "nag_PHP.pdf": ("Philippians", "PHP"),
    "nag_COL.pdf": ("Colossians", "COL"),
    "nag_1TH.pdf": ("1 Thessalonians", "1TH"),
    "nag_2TH.pdf": ("2 Thessalonians", "2TH"),
    "nag_1TI.pdf": ("1 Timothy", "1TI"),
    "nag_2TI.pdf": ("2 Timothy", "2TI"),
    "nag_TIT.pdf": ("Titus", "TIT"),
    "nag_PHM.pdf": ("Philemon", "PHM"),
    "nag_HEB.pdf": ("Hebrews", "HEB"),
    "nag_JAS.pdf": ("James", "JAS"),
    "nag_1PE.pdf": ("1 Peter", "1PE"),
    "nag_2PE.pdf": ("2 Peter", "2PE"),
    "nag_1JN.pdf": ("1 John", "1JN"),
    "nag_2JN.pdf": ("2 John", "2JN"),
    "nag_3JN.pdf": ("3 John", "3JN"),
    "nag_JUD.pdf": ("Jude", "JUD"),
    "nag_REV.pdf": ("Revelation", "REV")
}

class NagamesePdfExtractor:

    def __init__(self, pdf_dir: str):
        self.pdf_dir = pdf_dir

    def parse_book(self, pdf_name: str) -> List[Dict[str, Any]]:
        if pdf_name not in BOOK_MAPPING:
            return []

        book_title, book_code = BOOK_MAPPING[pdf_name]
        pdf_path = os.path.join(self.pdf_dir, pdf_name)
        
        reader = pypdf.PdfReader(pdf_path)
        lines = []
        for p in reader.pages:
            t = p.extract_text()
            if t:
                lines.extend(t.splitlines())

        v_regex = re.compile(r"^\s*(\d{1,3})\s+(.*)")
        verses = []
        curr_ch = 1
        prev_v = 0
        curr_v = None
        curr_txt = []

        for line in lines:
            s = str(line).strip()
            if not s:
                continue

            # Ignore top running header line e.g. 'Mark 1:1 i Mark 1:7'
            if " i " in s and (":" in s):
                continue
            # Ignore book title repetitions
            if s == f"{book_title} He Likha Bhal Khobor" or s == book_title:
                continue

            m = v_regex.match(s)
            if m:
                v_num = int(m.group(1))
                v_body = m.group(2).strip()

                if curr_v is not None:
                    if v_num <= prev_v:
                        curr_ch += 1

                    verse_text = " ".join(curr_txt).replace("\ufffd", "").strip()
                    verses.append({
                        "book": book_title,
                        "book_code": book_code,
                        "chapter": curr_ch,
                        "verse": curr_v,
                        "verse_id": f"{book_code}_{curr_ch}_{curr_v}",
                        "text": verse_text
                    })

                prev_v = v_num
                curr_v = v_num
                curr_txt = [v_body]
            else:
                if curr_v is not None:
                    curr_txt.append(s)

        if curr_v is not None and curr_txt:
            verse_text = " ".join(curr_txt).replace("\ufffd", "").strip()
            verses.append({
                "book": book_title,
                "book_code": book_code,
                "chapter": curr_ch,
                "verse": curr_v,
                "verse_id": f"{book_code}_{curr_ch}_{curr_v}",
                "text": verse_text
            })

        return verses

    def extract_all(self, output_json_path: str, output_txt_path: str) -> Dict[str, Any]:
        all_verses = []
        monolingual_lines = []

        for pdf_name in sorted(BOOK_MAPPING.keys()):
            pdf_path = os.path.join(self.pdf_dir, pdf_name)
            if os.path.exists(pdf_path):
                b_verses = self.parse_book(pdf_name)
                all_verses.extend(b_verses)
                for v in b_verses:
                    monolingual_lines.append(v["text"])
                print(f"Extracted {len(b_verses)} verses from {pdf_name}")

        os.makedirs(os.path.dirname(output_json_path), exist_ok=True)
        with open(output_json_path, "w", encoding="utf-8") as f:
            json.dump(all_verses, f, ensure_ascii=False, indent=2)

        os.makedirs(os.path.dirname(output_txt_path), exist_ok=True)
        with open(output_txt_path, "w", encoding="utf-8") as f:
            f.write("\n".join(monolingual_lines))

        print(f"\nSUCCESS! Extracted {len(all_verses)} verses across 26 books.")
        return {
            "total_verses": len(all_verses),
            "json_path": output_json_path,
            "txt_path": output_txt_path
        }

if __name__ == "__main__":
    pdf_directory = "datasets/raw/parallel/nagamese_nt_pdfs"
    json_out = "datasets/raw/parallel/nagamese_parsed_verses.json"
    txt_out = "datasets/raw/vocabulary/monolingual_nagamese.txt"

    extractor = NagamesePdfExtractor(pdf_directory)
    extractor.extract_all(json_out, txt_out)
