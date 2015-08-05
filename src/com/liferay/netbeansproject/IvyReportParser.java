package com.liferay.netbeansproject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class IvyReportParser {

	public static void main(String[] args) throws Exception {
		if(args.length != 2) {
			throw new IllegalArgumentException();
		}

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		Document document = documentBuilder.parse(args[0]);

		XPathFactory xPathFactory = XPathFactory.newInstance();

		XPath xPath = xPathFactory.newXPath();


		XPathExpression xPathExpression =
			xPath.compile(
				"/ivy-report/dependencies/module/revision/artifacts/" +
					"artifact/@location");

		NodeList nodeList =
			(NodeList) xPathExpression.evaluate(
				document, XPathConstants.NODESET);

		for(int i = 0; i < nodeList.getLength(); i++) {
			try (
				PrintWriter printWriter =
					new PrintWriter(
						new BufferedWriter(new FileWriter(args[1],true)))) {

				printWriter.print(nodeList.item(i).getNodeValue() + ":");
			}
		}
	}

}