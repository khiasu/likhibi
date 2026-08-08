$docxPath = "f:\likhibi-main\docs\Nagamese_NLP_Presentation_Script.docx"
$pdfPath = "f:\likhibi-main\docs\Nagamese_NLP_Presentation_Script.pdf"
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$doc = $word.Documents.Open($docxPath)
$wdFormatPDF = 17
$doc.SaveAs($pdfPath, $wdFormatPDF)
$doc.Close()
$word.Quit()
Write-Host "PDF Generated Successfully!"
