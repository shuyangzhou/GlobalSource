package com.liferay.netbeansproject;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class IvyReportParser {

	public static String ParseIvyReport(String modulePath) throws Exception {
		Path path = Paths.get(modulePath);

		String reportName = path.getFileName().toString();

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		File reportFile = new File("ivy-reports/" + reportName);

		if(!reportFile.exists()) {
			return "";
		}

		Document document = documentBuilder.parse(reportFile);

		XPathFactory xPathFactory = XPathFactory.newInstance();

		XPath xPath = xPathFactory.newXPath();

		XPathExpression xPathExpression =
			xPath.compile(
				"/ivy-report/dependencies/module/revision/artifacts/" +
					"artifact/@location");

		NodeList nodeList =
			(NodeList) xPathExpression.evaluate(
				document, XPathConstants.NODESET);

		StringBuilder sb = new StringBuilder();

		for(int i = 0; i < nodeList.getLength(); i++) {
			sb.append(nodeList.item(i).getNodeValue());
			sb.append(":");
		}

		return sb.toString();
	}

}