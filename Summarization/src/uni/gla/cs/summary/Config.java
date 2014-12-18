package uni.gla.cs.summary;


public class Config {
	public static String stopwordfile="src\\test\\resources\\data\\stopword\\stopwords.txt";
//	public static final String folderpath = "C:\\Users\\fajie\\Desktop\\output2";
//	public static final String subfolderpath = "C:\\Users\\fajie\\Desktop\\output2\\fruit";
	public static final String folderpath = "C:\\data\\DUC\\DUC-2004\\docs";
	public static final String subfolderpath = "C:\\data\\DUC\\DUC-2004\\docs\\d30001t";//the path is the subdirectory of folderpath,already clustered
	public static final String outputfile="C:\\Users\\fajie\\Desktop\\output3\\CentroidSummary.txt";
	public static int SenNum=50;//Senteces num for summary
	public static int SenNumGraph=100;//Senteces num for summary-based graph
	public static String indexPath="src\\test\\resources\\data\\index\\";//folder of index
	public static String regex="@@@@@@";
	public static final int senNumCentroid=10;
	public static final int senLength=6;
	public static final float redThreshold=0.65F;

}
