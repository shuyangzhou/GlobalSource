package com.liferay.netbeansproject;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
public class IvyReportParser {

	public static String parseIvyReport(String projectName) throws Exception {
		StringBuilder sb = new StringBuilder();

		parseConf(projectName + "-default.xml", sb);
		parseConf(projectName + "-internal.xml", sb);
		parseConf(projectName + "-provided.xml", sb);
		parseConf(projectName + "-test.xml", sb);

		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}

		return sb.toString();
	}

	private static String parseConf(String fileName, StringBuilder sb)
		throws Exception {

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		File reportFile = new File("ivy-reports/" + fileName);

		if (!reportFile.exists()) {
			return "";
		}

		XPathFactory xPathFactory = XPathFactory.newInstance();

		XPath xPath = xPathFactory.newXPath();

		XPathExpression xPathExpression = xPath.compile(
			"/ivy-report/dependencies/module/revision/artifacts/" +
				"artifact/@location");

		NodeList nodeList = (NodeList)xPathExpression.evaluate(
			documentBuilder.parse(reportFile), XPathConstants.NODESET);

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);

			sb.append(node.getNodeValue());
			sb.append(":");
		}

		return sb.toString();
	}

}