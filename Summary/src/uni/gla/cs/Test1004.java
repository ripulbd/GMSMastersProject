package uni.gla.cs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class Test1004 {

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws CorruptIndexException,
			LockObtainFailedException, IOException, ParseException {
		// Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		// Analyzer analyzer = new PorterStemAnalyzer();
		// Analyzer analyzer=new StopAnalyzer(Version.LUCENE_CURRENT);
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		// Analyzer analyzer=new WhitespaceAnalyzer(Version.LUCENE_CURRENT);
		// Analyzer analyzer=new KeywordAnalyzer();
		//

		String path = "src\\test\\resources\\data\\index";
		File indexpath = new File(path);
//		indexpath.delete();
		File indexfiles[] = indexpath.listFiles();
		for (File file : indexfiles) {
			file.delete();
		}
//		String filelocation0 = "src\\test\\resources\\data\\xijinping3.txt";
//		String filelocation1 = "src\\test\\resources\\data\\xijinping2.txt";
//		String filelocation2 = "src\\test\\resources\\data\\xijinping.txt";
		File dir = new File("src\\test\\resources\\data\\");
		File file[] = dir.listFiles();

		// Store the index in memory:
		// Directory directory = new RAMDirectory();
		// To store an index on disk, use this instead:
		Directory directory = FSDirectory.open(new File(path));
		IndexWriter iwriter = new IndexWriter(directory, analyzer, true,
				IndexWriter.MaxFieldLength.LIMITED);
		for (int i = 0; i < file.length; i++) {
			Document doc = new Document();
			if (!file[i].isDirectory()) {
				doc.add(new Field("fieldname", new FileReader(file[i])));
				iwriter.addDocument(doc);
			}
			
		}
		// Document doc0 = new Document();
		// Document doc1 = new Document();
		// Document doc2 = new Document();
		//
		// doc0.add(new Field("fieldname", new FileReader(filelocation0)));
		// doc1.add(new Field("fieldname", new FileReader(filelocation1)));
		// doc2.add(new Field("fieldname", new FileReader(filelocation2)));
		// iwriter.addDocument(doc0);
		// iwriter.addDocument(doc1);
		// iwriter.addDocument(doc2);

		iwriter.optimize();
		iwriter.close();

		// Now search the index:
		// IndexReader ireader = IndexReader.open(directory); // read-only=true
		// IndexSearcher isearcher = new IndexSearcher(ireader);
		Searcher isearcher = new IndexSearcher(directory);
		
		// Parse a simple query that searches for "text":
		QueryParser parser = new QueryParser(Version.LUCENE_CURRENT,
				"fieldname", analyzer);
		Query query = parser.parse("Angeles in to Occidental College");
		ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;
		// Iterate through the results:
		// TopDocs scoredoc= isearcher.search(query, 100);
		// System.out.println( scoredoc.totalHits);
		System.out.println(hits.length);

		for (int i = 0; i < hits.length; i++) {
			Document hitDoc = isearcher.doc(hits[i].doc);
			// System.out.println(hitDoc.get("fieldname"));
			System.out.println(hits[i].score);
			System.out.println(hits[i].toString());
		}
		isearcher.close();
		// ireader.close();
		directory.close();
		
	}
}