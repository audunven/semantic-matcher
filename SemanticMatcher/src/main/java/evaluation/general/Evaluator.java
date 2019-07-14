package evaluation.general;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Chart;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCatAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLegend;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumRef;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScaling;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTStrRef;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.STAxPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.STBarDir;
import org.openxmlformats.schemas.drawingml.x2006.chart.STLegendPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.STOrientation;
import org.openxmlformats.schemas.drawingml.x2006.chart.STTickLblPos;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.eval.DiffEvaluator;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import utilities.StringUtilities;



/**
 * @author audunvennesland
 * 9. apr. 2017 
 */
public class Evaluator {

	/**
	 * Evaluates a single alignment against a reference alignment and prints precision, recall, f-measure, true positives (TP), false positives (FP) and false negatives (FN)
	 * @param inputAlignmentFileName
	 * @param referenceAlignmentFileName
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 */
	public static void evaluateSingleAlignment (String inputAlignmentFileName, String referenceAlignmentFileName) throws AlignmentException, URISyntaxException {

		AlignmentParser refAlignParser = new AlignmentParser(0);
		AlignmentParser evalAlignParser = new AlignmentParser(1);

		Alignment referenceAlignment = refAlignParser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentFileName)));
		BasicAlignment inputAlignment = (BasicAlignment) evalAlignParser.parse(new URI(StringUtilities.convertToFileURL(inputAlignmentFileName)));

		Properties p = new Properties();
		PRecEvaluator eval = new PRecEvaluator(referenceAlignment, inputAlignment);

		eval.eval(p);

		System.out.println("------------------------------");
		System.out.println("Evaluator scores for " + inputAlignmentFileName);
		System.out.println("------------------------------");
		System.out.println("F-measure: " + eval.getResults().getProperty("fmeasure").toString());
		System.out.println("Precision: " + eval.getResults().getProperty("precision").toString());
		System.out.println("Recall: " + eval.getResults().getProperty("recall").toString());

		System.out.println("True positives (TP): " + eval.getResults().getProperty("true positive").toString());

		int fp = eval.getFound() - eval.getCorrect();
		System.out.println("False positives (FP): " + fp);
		int fn = eval.getExpected() - eval.getCorrect();
		System.out.println("False negatives (FN): " + fn);
		System.out.println("\n");

	}
	

	/**
	 * Evaluates a single alignment against a reference alignment and prints precision, recall, f-measure, true positives (TP), false positives (FP) and false negatives (FN)
	 * @param inputAlignmentFileName
	 * @param referenceAlignmentFileName
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 */
	public static void evaluateSingleAlignment (URIAlignment inputAlignment, String referenceAlignmentFileName) throws AlignmentException, URISyntaxException {

		AlignmentParser refAlignParser = new AlignmentParser(0);

		Alignment referenceAlignment = refAlignParser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentFileName)));

		Properties p = new Properties();
		PRecEvaluator eval = new PRecEvaluator(referenceAlignment, inputAlignment);

		eval.eval(p);

		System.out.println("------------------------------");
		System.out.println("Evaluator scores for " + inputAlignment.getType());
		System.out.println("------------------------------");
		System.out.println("F-measure: " + eval.getResults().getProperty("fmeasure").toString());
		System.out.println("Precision: " + eval.getResults().getProperty("precision").toString());
		System.out.println("Recall: " + eval.getResults().getProperty("recall").toString());

		System.out.println("True positives (TP): " + eval.getResults().getProperty("true positive").toString());

		int fp = eval.getFound() - eval.getCorrect();
		System.out.println("False positives (FP): " + fp);
		int fn = eval.getExpected() - eval.getCorrect();
		System.out.println("False negatives (FN): " + fn);
		System.out.println("\n");

	}
	
	public static void evaluateSingleAlignmentDiff (URIAlignment inputAlignment, String referenceAlignmentFileName) throws AlignmentException, URISyntaxException, IOException {
		
		AlignmentParser refAlignParser = new AlignmentParser(0);

		Alignment referenceAlignment = refAlignParser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentFileName)));

		Properties p = new Properties();
		
		PRecEvaluator eval = new PRecEvaluator(referenceAlignment, inputAlignment);
		eval.eval(p);

		System.out.println("------------------------------");
		System.out.println("Evaluator scores for " + inputAlignment.getType());
		System.out.println("------------------------------");
		System.out.println("F-measure: " + eval.getResults().getProperty("fmeasure").toString());
		System.out.println("Precision: " + eval.getResults().getProperty("precision").toString());
		
		
		DiffEvaluator evalDiff = new DiffEvaluator(referenceAlignment, inputAlignment);
		//eval.eval(p);
		evalDiff.diff();
		
		System.out.println("Number of true positives: " + evalDiff.getTruePositive().size() + ", Number of false positives: " + evalDiff.getFalsePositive().size() + ", Number of false negatives: " + evalDiff.getFalseNegative().size());
		
		
		File outputFile = new File("./files/test.txt");
		
		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputFile)), true); 
		
		
		eval.write(writer);
		
		
	}
	
	
	/**
	 * Evaluates a single alignment against a reference alignment and prints precision, recall, f-measure, true positives (TP), false positives (FP) and false negatives (FN)
	 * @param inputAlignmentFileName
	 * @param referenceAlignmentFileName
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 * @throws IOException 
	 */
	public static void evaluateSingleAlignment (String alignmentName, Alignment inputAlignment, String referenceAlignmentFileName, String output) throws AlignmentException, URISyntaxException, IOException {

		AlignmentParser refAlignParser = new AlignmentParser(0);

		Alignment referenceAlignment = refAlignParser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentFileName)));
		
		File outputFile = new File(output);
		
		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputFile)), true); 

		Properties p = new Properties();
		PRecEvaluator eval = new PRecEvaluator(referenceAlignment, inputAlignment);

		eval.eval(p);
		int fp = eval.getFound() - eval.getCorrect();
		int fn = eval.getExpected() - eval.getCorrect();

		//print to file
		writer.println("-----------------------------------------------------------------------");
		writer.println("Evaluator scores for " + alignmentName);
		writer.println("-----------------------------------------------------------------------");
		writer.println("Number of relations: " + inputAlignment.nbCells());
		writer.println("-----------------------------------------------------------------------");
		writer.println("F-measure: " + eval.getResults().getProperty("fmeasure").toString());
		writer.println("Precision: " + eval.getResults().getProperty("precision").toString());
		writer.println("Recall: " + eval.getResults().getProperty("recall").toString());
		writer.println("True positives (TP): " + eval.getResults().getProperty("true positive").toString());		
		writer.println("False positives (FP): " + fp);		
		writer.println("False negatives (FN): " + fn);
		writer.println("\n");
		writer.flush();
		writer.close();
		
		//print to console
		System.out.println("-----------------------------------------------------------------------");
		System.out.println("Evaluator scores for " + alignmentName);
		System.out.println("-----------------------------------------------------------------------");
		System.out.println("Number of relations: " + inputAlignment.nbCells());
		System.out.println("-----------------------------------------------------------------------");
		System.out.println("F-measure: " + eval.getResults().getProperty("fmeasure").toString());
		System.out.println("Precision: " + eval.getResults().getProperty("precision").toString());
		System.out.println("Recall: " + eval.getResults().getProperty("recall").toString());
		System.out.println("True positives (TP): " + eval.getResults().getProperty("true positive").toString());
		System.out.println("False positives (FP): " + fp);
		System.out.println("False negatives (FN): " + fn);
		System.out.println("\n");

	}
	
	/**
	 * Evaluates a single alignment against a reference alignment and prints precision, recall, f-measure, true positives (TP), false positives (FP) and false negatives (FN) to both console and file
	 * @param inputAlignmentFileName
	 * @param referenceAlignmentFileName
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 * @throws IOException 
	 */
	public static void evaluateSingleAlignment (String alignmentName, URIAlignment inputAlignment, String referenceAlignmentFileName, String output) throws AlignmentException, URISyntaxException, IOException {

		AlignmentParser refAlignParser = new AlignmentParser(0);

		Alignment referenceAlignment = refAlignParser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentFileName)));
		
		File outputFile = new File(output);
		
		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputFile)), true); 

		Properties p = new Properties();
		PRecEvaluator eval = new PRecEvaluator(referenceAlignment, inputAlignment);

		eval.eval(p);
		int fp = eval.getFound() - eval.getCorrect();
		int fn = eval.getExpected() - eval.getCorrect();

		//print to file
		writer.println("-----------------------------------------------------------------------");
		writer.println("Evaluator scores for " + alignmentName);
		writer.println("-----------------------------------------------------------------------");
		writer.println("Number of relations: " + inputAlignment.nbCells());
		writer.println("-----------------------------------------------------------------------");
		writer.println("F-measure: " + eval.getResults().getProperty("fmeasure").toString());
		writer.println("Precision: " + eval.getResults().getProperty("precision").toString());
		writer.println("Recall: " + eval.getResults().getProperty("recall").toString());
		writer.println("True positives (TP): " + eval.getResults().getProperty("true positive").toString());		
		writer.println("False positives (FP): " + fp);		
		writer.println("False negatives (FN): " + fn);
		writer.println("\n");
		writer.flush();
		writer.close();
		
		//print to console
		System.out.println("-----------------------------------------------------------------------");
		System.out.println("Evaluator scores for " + alignmentName);
		System.out.println("-----------------------------------------------------------------------");
		System.out.println("Number of relations: " + inputAlignment.nbCells());
		System.out.println("-----------------------------------------------------------------------");
		System.out.println("F-measure: " + eval.getResults().getProperty("fmeasure").toString());
		System.out.println("Precision: " + eval.getResults().getProperty("precision").toString());
		System.out.println("Recall: " + eval.getResults().getProperty("recall").toString());
		System.out.println("True positives (TP): " + eval.getResults().getProperty("true positive").toString());
		System.out.println("False positives (FP): " + fp);
		System.out.println("False negatives (FN): " + fn);
		System.out.println("\n");

	}
	
	/**
	 * Evaluates a single alignment against a reference alignment and prints precision, recall, f-measure, true positives (TP), false positives (FP) and false negatives (FN) to console only.
	 * @param inputAlignmentFileName
	 * @param referenceAlignmentFileName
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 * @throws IOException 
	 */
	public static void evaluateSingleAlignment (String alignmentName, URIAlignment inputAlignment, String referenceAlignmentFileName) throws AlignmentException, URISyntaxException, IOException {

		AlignmentParser refAlignParser = new AlignmentParser(0);

		Alignment referenceAlignment = refAlignParser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentFileName)));

		Properties p = new Properties();
		PRecEvaluator eval = new PRecEvaluator(referenceAlignment, inputAlignment);

		eval.eval(p);
		int fp = eval.getFound() - eval.getCorrect();
		int fn = eval.getExpected() - eval.getCorrect();
		
		//print to console
		System.out.println("-----------------------------------------------------------------------");
		System.out.println("Evaluator scores for " + alignmentName);
		System.out.println("-----------------------------------------------------------------------");
		System.out.println("Number of relations: " + inputAlignment.nbCells());
		System.out.println("-----------------------------------------------------------------------");
		System.out.println("F-measure: " + eval.getResults().getProperty("fmeasure").toString());
		System.out.println("Precision: " + eval.getResults().getProperty("precision").toString());
		System.out.println("Recall: " + eval.getResults().getProperty("recall").toString());
		System.out.println("True positives (TP): " + eval.getResults().getProperty("true positive").toString());
		System.out.println("False positives (FP): " + fp);
		System.out.println("False negatives (FN): " + fn);
		System.out.println("\n");

	}
	
	/**
	 * Evaluates a single alignment against a reference alignment and prints precision, recall, f-measure, true positives (TP), false positives (FP) and false negatives (FN)
	 * @param inputAlignmentFileName
	 * @param referenceAlignmentFileName
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 */
	public static void evaluateSingleAlignment (BasicAlignment inputAlignment, String referenceAlignmentFileName) throws AlignmentException, URISyntaxException {

		AlignmentParser refAlignParser = new AlignmentParser(0);
		AlignmentParser evalAlignParser = new AlignmentParser(1);

		Alignment referenceAlignment = refAlignParser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentFileName)));
		//BasicAlignment inputAlignment = (BasicAlignment) evalAlignParser.parse(new URI(StringUtilities.convertToFileURL(inputAlignmentFileName)));

		Properties p = new Properties();
		PRecEvaluator eval = new PRecEvaluator(referenceAlignment, inputAlignment);

		eval.eval(p);

		System.out.println("------------------------------");
		System.out.println("Evaluator scores for " + inputAlignment.getType());
		System.out.println("------------------------------");
		System.out.println("F-measure: " + eval.getResults().getProperty("fmeasure").toString());
		System.out.println("Precision: " + eval.getResults().getProperty("precision").toString());
		System.out.println("Recall: " + eval.getResults().getProperty("recall").toString());

		System.out.println("True positives (TP): " + eval.getResults().getProperty("true positive").toString());

		int fp = eval.getFound() - eval.getCorrect();
		System.out.println("False positives (FP): " + fp);
		int fn = eval.getExpected() - eval.getCorrect();
		System.out.println("False negatives (FN): " + fn);
		System.out.println("\n");

	}

	/**
	 * Evaluates all alignments in a folder against a reference alignment prints for each alignment: precision, recall, f-measure, true positives (TP), false positives (FP) and false negatives (FN)
	 * @param folderName The folder holding all alignments
	 * @param referenceAlignmentFileName
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 */
	public static void evaluateAlignmentFolder (String folderName, String referenceAlignmentFileName) throws AlignmentException, URISyntaxException {

		AlignmentParser aparser = new AlignmentParser(0);
		Alignment referenceAlignment = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentFileName)));

		Properties p = new Properties();

		File folder = new File(folderName);
		File[] filesInDir = folder.listFiles();
		Alignment evaluatedAlignment = null;
		PRecEvaluator eval = null;

		for (int i = 0; i < filesInDir.length; i++) {

			String URI = StringUtilities.convertToFileURL(folderName) + "/" + StringUtilities.stripPath(filesInDir[i].toString());
			System.out.println("Evaluating file " + URI);
			evaluatedAlignment = aparser.parse(new URI(URI));

			eval = new PRecEvaluator(referenceAlignment, evaluatedAlignment);

			eval.eval(p);

			System.out.println("Number of relations in alignment: " + evaluatedAlignment.nbCells());

			System.out.println("------------------------------");
			System.out.println("Evaluator scores for " + StringUtilities.stripPath(filesInDir[i].toString()));
			System.out.println("------------------------------");
			System.out.println("F-measure: " + eval.getResults().getProperty("fmeasure").toString());
			System.out.println("Precision: " + eval.getResults().getProperty("precision").toString());
			System.out.println("Recall: " + eval.getResults().getProperty("recall").toString());

			System.out.println("True positives (TP): " + eval.getResults().getProperty("true positive").toString());

			int fp = eval.getFound() - eval.getCorrect();
			System.out.println("False positives (FP): " + fp);
			int fn = eval.getExpected() - eval.getCorrect();
			System.out.println("False negatives (FN): " + fn);
			System.out.println("\n");
		}
	}



	/**
	 * Produces a Map of key: matcher (i.e. alignment produced by a particular matcher) and value: F-measure score from evaluation of against the alignment for that particular matcher
	 * @param folderName The folder holding the alignments to be evaluated
	 * @param referenceAlignmentFileName
	 * @return A Map<String, Double) holding the matcher (alignment) and F-measure score for that particular matcher (alignment)
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 */
	public static Map<String, EvaluationScore> evaluateAlignmentFolderMap (String folderName, String referenceAlignmentFileName) throws AlignmentException, URISyntaxException {

		Map<String, EvaluationScore> evalFolderMap = new HashMap<String, EvaluationScore>();

		double precision = 0;
		double recall = 0;		
		double fMeasure = 0;

		AlignmentParser aparser = new AlignmentParser(0);
		Alignment referenceAlignment = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentFileName)));

		Properties p = new Properties();
		File folder = new File(folderName);
		File[] filesInDir = folder.listFiles();

		Alignment evaluatedAlignment = null;
		PRecEvaluator eval = null;



		for (int i = 0; i < filesInDir.length; i++) {

			EvaluationScore evalScore = new EvaluationScore();

			String URI = StringUtilities.convertToFileURL(folderName) + "/" + StringUtilities.stripPath(filesInDir[i].toString());

			evaluatedAlignment = aparser.parse(new URI(URI));

			eval = new PRecEvaluator(referenceAlignment, evaluatedAlignment);

			eval.eval(p);

			precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
			recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
			fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

			evalScore.setPrecision(precision);
			evalScore.setRecall(recall);
			evalScore.setfMeasure(fMeasure);

			evalFolderMap.put(URI.substring(URI.lastIndexOf("/") +1), evalScore);

		}

		return evalFolderMap;

	}

	/**
	 * Runs a complete evaluation producing F-measure scores for individual matchers and combination strategies. The F-measure scores are printed to console.
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 * @throws FileNotFoundException 
	 */
	public static void runCompleteEvaluation (String alignmentsFolder, String referenceAlignment, String outputPath, String datasetName) throws AlignmentException, URISyntaxException, FileNotFoundException {

		File allIndividualAlignments = new File(alignmentsFolder);

		File[] folders = allIndividualAlignments.listFiles();
		System.err.println("Size of folders: " + folders.length);

		XSSFWorkbook workbook = new XSSFWorkbook();		

		XSSFSheet spreadsheet = null;


		//get a map<matcherName, fMeasureValue>
		Map<String, EvaluationScore> evalMap = evaluateAlignmentFolderMap(alignmentsFolder, referenceAlignment);

		spreadsheet = workbook.createSheet(datasetName);


		Cell cell = null;

		//Create a new font and alter it.
		XSSFFont font = workbook.createFont();
		font.setFontHeightInPoints((short) 30);
		font.setItalic(true);
		font.setBold(true);

		//Set font into style
		CellStyle style = workbook.createCellStyle();
		style.setFont(font);

		int rowNum = 0;

		Row headerRow = spreadsheet.createRow(0);

		//style=row.getRowStyle();
		headerRow.createCell(0).setCellValue("Matcher");
		headerRow.createCell(1).setCellValue("Precision");
		headerRow.createCell(2).setCellValue("Recall");
		headerRow.createCell(3).setCellValue("F-measure");

		EvaluationScore es = new EvaluationScore();
		double precision = 0;
		double recall = 0;
		double fMeasure = 0;

		for (Entry<String, EvaluationScore> e : evalMap.entrySet()) {

			es = e.getValue();
			precision = es.getPrecision();
			recall = es.getRecall();
			fMeasure = es.getfMeasure();

			int cellnum = 0;

			Row evalRow = spreadsheet.createRow(rowNum++);
			cell = evalRow.createCell(cellnum++);
			cell.setCellValue(e.getKey());
			cell = evalRow.createCell(cellnum++);
			cell.setCellValue(precision);
			cell = evalRow.createCell(cellnum++);
			cell.setCellValue(recall);
			cell = evalRow.createCell(cellnum++);
			cell.setCellValue(fMeasure);

		}

		try {
			FileOutputStream outputStream = 
					new FileOutputStream(new File(outputPath));
			workbook.write(outputStream);
			outputStream.close();
			System.out.println("Excel written successfully..");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Evaluates a single matcher at various thresholds
	 * @param alignmentsFolder the folder holding all alignments produced by the matcher
	 * @param referenceAlignment the reference alignment to which the matcher results are measured against
	 * @param outputPath an output path for the produced Excel
	 * @param datasetName
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 * @throws IOException 
	 */
	public static void evaluateSingleMatcherThresholds (Map<String, EvaluationScore> evaluationMap, String output) throws AlignmentException, URISyntaxException, IOException {
		
		System.out.println("Creating charts...");
		
		Workbook wb = new XSSFWorkbook();
		
		
		Sheet sheet = wb.createSheet("Sheet1");

		Row row;
		Cell cell;
		
		row = sheet.createRow(0);
		row.createCell(0);

		int counter = 0;
		
		//add the confidence thresholds in the first row
		for (Entry<String, EvaluationScore> e : evaluationMap.entrySet()) {
			counter++;
			row.createCell(counter).setCellValue(e.getKey());

		}
		

		//in the second row add all precision values
		row = sheet.createRow(1);
		cell = row.createCell(0);
		cell.setCellValue("Precision");
		
		int precisionCounter = 0;
		for (Entry<String, EvaluationScore> e : evaluationMap.entrySet()) {
			precisionCounter++;
			cell = row.createCell(precisionCounter);
			cell.setCellValue(e.getValue().getPrecision());
		}
		
		//in the third row add all recall values
		row = sheet.createRow(2);
		cell = row.createCell(0);
		cell.setCellValue("Recall");
		int recallCounter = 0;
		
		for (Entry<String, EvaluationScore> e : evaluationMap.entrySet()) {
			recallCounter++;
			cell = row.createCell(recallCounter);
			cell.setCellValue(e.getValue().getRecall());
		}
		
		//in the third row add all F-measure values
		row = sheet.createRow(3);
		cell = row.createCell(0);
		cell.setCellValue("F-measure");
		int fMeasureCounter = 0;

		for (Entry<String, EvaluationScore> e : evaluationMap.entrySet()) {
			fMeasureCounter++;
			cell = row.createCell(fMeasureCounter);
			cell.setCellValue(e.getValue().getfMeasure());
		}

		Drawing drawing = sheet.createDrawingPatriarch();
		ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 5, 8, 20);

		Chart chart = drawing.createChart(anchor);

		CTChart ctChart = ((XSSFChart)chart).getCTChart();
		CTPlotArea ctPlotArea = ctChart.getPlotArea();
		CTBarChart ctBarChart = ctPlotArea.addNewBarChart();
		CTBoolean ctBoolean = ctBarChart.addNewVaryColors();
		ctBoolean.setVal(true);
		ctBarChart.addNewBarDir().setVal(STBarDir.COL);

		for (int r = 2; r < 6; r++) {
			CTBarSer ctBarSer = ctBarChart.addNewSer();
			CTSerTx ctSerTx = ctBarSer.addNewTx();
			CTStrRef ctStrRef = ctSerTx.addNewStrRef();
			ctStrRef.setF("Sheet1!$A$" + r);
			ctBarSer.addNewIdx().setVal(r-2);  
			CTAxDataSource cttAxDataSource = ctBarSer.addNewCat();
			ctStrRef = cttAxDataSource.addNewStrRef();
			ctStrRef.setF("Sheet1!$B$1:$J$1"); 
			CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
			CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
			ctNumRef.setF("Sheet1!$B$" + r + ":$J$" + r);

			//at least the border lines in Libreoffice Calc ;-)
			ctBarSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});   

		} 

		//telling the BarChart that it has axes and giving them Ids
		ctBarChart.addNewAxId().setVal(123456);
		ctBarChart.addNewAxId().setVal(123457);

		//cat axis
		CTCatAx ctCatAx = ctPlotArea.addNewCatAx(); 
		ctCatAx.addNewAxId().setVal(123456); //id of the cat axis
		CTScaling ctScaling = ctCatAx.addNewScaling();
		ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
		ctCatAx.addNewDelete().setVal(false);
		ctCatAx.addNewAxPos().setVal(STAxPos.B);
		ctCatAx.addNewCrossAx().setVal(123457); //id of the val axis
		ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

		//val axis
		CTValAx ctValAx = ctPlotArea.addNewValAx(); 
		ctValAx.addNewAxId().setVal(123457); //id of the val axis
		ctScaling = ctValAx.addNewScaling();
		ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
		ctValAx.addNewDelete().setVal(false);
		ctValAx.addNewAxPos().setVal(STAxPos.L);
		ctValAx.addNewCrossAx().setVal(123456); //id of the cat axis
		ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

		//legend
		CTLegend ctLegend = ctChart.addNewLegend();
		ctLegend.addNewLegendPos().setVal(STLegendPos.B);
		ctLegend.addNewOverlay().setVal(false);

		//System.out.println(ctChart);

		FileOutputStream fileOut = new FileOutputStream(output+".xlsx");
		wb.write(fileOut);
		fileOut.close();
		

	}


	public static void main(String[] args) throws AlignmentException, URISyntaxException, IOException {
		
		/* TRYING DIFF EVALUATOR */
		File singleAlignment = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ALIGNMENTS/PROFILEWEIGHT/MERGED_SIGMOID/PROFILEWEIGHT_MERGED_SIGMOIDATMONTO-AIRM.rdf");
		AlignmentParser parser = new AlignmentParser();
		URIAlignment alignmentFile = (URIAlignment) parser.parse(singleAlignment.toURI().toString());
		String refalign = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQ-SUB.rdf";
		evaluateSingleAlignmentDiff(alignmentFile, refalign);

//		/* RUN COMPLETE EVALUATION */
//		String alignmentFolder = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ALIGNMENTS/MISMATCHDETECTION/ALIGNMENTS";
//		String refalign = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQUIVALENCE.rdf";
//		String outputEvaluationFile = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ALIGNMENTS/MISMATCHDETECTION/EVALUATION-INITIALALIGNMENTS.xslx";
//		String datasetName = "ATM-MISMATCH-EQUIVALENCE";
//		
//		runCompleteEvaluation(alignmentFolder, refalign, outputEvaluationFile, datasetName);


		/* EVALUATE SINGLE ALIGNMENT */
//					File singleAlignment = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ALIGNMENTS/HARMONY_WEIGHTED/SUBSUMPTION/ComputedHarmonyAlignment_WEIGHTED.rdf");
//					AlignmentParser parser = new AlignmentParser();
//					URIAlignment alignmentFile = (URIAlignment) parser.parse(singleAlignment.toURI().toString());
//					
//					String refalign = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-SUBSUMPTION.rdf";
//					String output = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ALIGNMENTS/HARMONY_WEIGHTED/SUBSUMPTION/eval_HARMONY_WEIGHTED.txt";
//					evaluateSingleAlignment(alignmentFile, refalign, output);
					//String outputEvaluationFile = "./files/_PHD_EVALUATION/ATMONTO-AIRM/EVALUATION/ATMONTO-AIRM_STROMA";
					//String datasetName = "ATMONTO-AIRM";

		/* EVALUATE A FOLDER OF ALIGNMENT FILES AND PRODUCE EXCEL */
		//			String onto1 = "303";
		//			String onto2 = "304";
		//			String type = "SUB";
		//			String alignmentFolder = "./files/_PHD_EVALUATION/OAEI2011/ALL_ALIGNMENTS/"+onto1+onto2+"/"+type;
		//			String refalign = "./files/_PHD_EVALUATION/OAEI2011/REFALIGN/"+onto1+onto2+"/"+onto1+"-"+onto2+"-"+type+".rdf";
		//			String outputEvaluationFile = "./files/_PHD_EVALUATION/OAEI2011/ALL_ALIGNMENTS/"+onto1+onto2+"/EVALUATION/OAEI2011_"+onto1+onto2+"_ALL_"+type+"_06.xslx";
		//			String datasetName = "OAEI2011-"+onto1+onto2+type;
		
	
		
		/*String alignmentFolder = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ALIGNMENTS/COMPETITION/EVALUATION_COMPETITION/SUBSUMPTION_EQUIVALENCE";
		String refalign = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQ-SUB.rdf";
		String outputEvaluationFile = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ALIGNMENTS/COMPETITION/EVALUATION_COMPETITION/SUBSUMPTION/BIBFRAME-SCHEMAORG-EVALUATION-SUBSUMPTION_EQUIVALENCE.xslx";
		String datasetName = "BIBFRAME-SCHEMAORG-SUBSUMPTION_EQUIVALENCE";
		runCompleteEvaluation(alignmentFolder, refalign, outputEvaluationFile, datasetName);*/
		
//		evaluateAlignmentFolder(alignmentFolder,refalign);
		
//		evaluateSingleAlignment(singleAlignment, refalign);


	}

}






