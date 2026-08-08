"""
Nagamese Web Scraper & Corpus Collector Module.

Scrapes public Nagamese news portals, articles, blogs, and digital text sources
to expand monolingual vocabulary coverage towards the ~20,000 entry goal.
"""

import os
import sys
import re
import urllib.request
import urllib.parse
from typing import List, Set

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

class NagameseWebScraper:
    """
    Scrapes and extracts Nagamese text from public news and web sources.
    """

    def __init__(self, output_txt_path: str = "datasets/raw/vocabulary/web_scraped_nagamese.txt"):
        self.output_txt_path = output_txt_path
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
        }

    def fetch_page_text(self, url: str) -> str:
        """
        Fetches raw HTML/text content from a given URL and strips HTML tags.
        """
        try:
            req = urllib.request.Request(url, headers=self.headers)
            with urllib.request.urlopen(req, timeout=10) as response:
                html = response.read().decode("utf-8", errors="ignore")
                
            # Strip script and style blocks
            cleaned_html = re.sub(r"<script.*?>.*?</script>", " ", html, flags=re.DOTALL | re.IGNORECASE)
            cleaned_html = re.sub(r"<style.*?>.*?</style>", " ", cleaned_html, flags=re.DOTALL | re.IGNORECASE)
            # Strip HTML tags
            text = re.sub(r"<.*?>", " ", cleaned_html)
            # Remove multiple whitespace
            text = re.sub(r"\s+", " ", text).strip()
            return text
        except Exception as e:
            print(f"Error fetching URL {url}: {e}")
            return ""

    def append_scraped_text(self, text_lines: List[str]):
        """
        Appends clean scraped lines to the web dataset text file.
        """
        if not text_lines:
            return

        os.makedirs(os.path.dirname(self.output_txt_path), exist_ok=True)
        with open(self.output_txt_path, "a", encoding="utf-8") as f:
            for line in text_lines:
                clean_line = line.strip()
                if len(clean_line) > 10:
                    f.write(clean_line + "\n")

        print(f"Appended {len(text_lines)} scraped text lines to {self.output_txt_path}")

if __name__ == "__main__":
    scraper = NagameseWebScraper()
    print("Nagamese Web Scraper initialized.")
