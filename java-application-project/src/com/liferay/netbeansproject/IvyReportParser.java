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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class IvyReportParser {

	public static String parseIvyReport(String modulePath) throws Exception {
		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		Path path = Paths.get(modulePath);

		File reportFile = new File("ivy-reports/" + path.getFileName());

		if(!reportFile.exists()) {
			return "";
		}

		XPathFactory xPathFactory = XPathFactory.newInstance();

		XPath xPath = xPathFactory.newXPath();

		XPathExpression xPathExpression =
			xPath.compile(
				"/ivy-report/dependencies/module/revision/artifacts/" +
					"artifact/@location");

		NodeList nodeList =
			(NodeList)xPathExpression.evaluate(
				documentBuilder.parse(reportFile), XPathConstants.NODESET);

		StringBuilder sb = new StringBuilder();

		for(int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);

			sb.append(node.getNodeValue());
			sb.append(":");
		}

		return sb.toString();
	}

}