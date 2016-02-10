package org.xzc.itext;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

public class LearnPdf {
	@Test
	public void createPdf1() throws DocumentException, IOException {
		BaseFont bf = BaseFont.createFont( "STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED );
		Font font = new Font( bf, 12, Font.BOLD );
		font.setColor( new BaseColor( 255, 0, 0 ) );
	 
		Font chapterFont = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLDITALIC);
		
		Document document = new Document();
		PdfWriter pw = PdfWriter.getInstance( document, new FileOutputStream( "1.pdf" ) );
		document.open();
		
		 Chunk chunk = new Chunk("This is the title", chapterFont);
	        Chapter chapter = new Chapter(new Paragraph(chunk), 1);
	        chapter.setNumberDepth( 0 );
	        document.add( chapter );
		//document.add( new Chapter( "C1", 1 ) );
		//默认的字体不支持中文!
		document.add( new Paragraph( "hello", font ) );
		document.close();
	}
}
