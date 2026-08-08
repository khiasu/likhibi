import sys
import os
import win32com.client

def convert_docx_to_pdf(docx_path, pdf_path):
    word = win32com.client.Dispatch("Word.Application")
    word.Visible = False
    doc = word.Documents.Open(os.path.abspath(docx_path))
    doc.SaveAs(os.path.abspath(pdf_path), FileFormat=17) # 17 = wdFormatPDF
    doc.Close()
    word.Quit()
    print(f"Successfully generated PDF presentation script at: {pdf_path}")

if __name__ == "__main__":
    convert_docx_to_pdf("docs/Nagamese_NLP_Presentation_Script.docx", "docs/Nagamese_NLP_Presentation_Script.pdf")
