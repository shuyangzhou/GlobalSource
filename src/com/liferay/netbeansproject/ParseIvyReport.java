package com.liferay.netbeansproject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ParseIvyReport {

	public static void main(String[] args) throws Exception {
		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		Document document = documentBuilder.parse(args[0]);

		Element ivyReportElement = document.getDocumentElement();

		if(ivyReportElement.getChildNodes().getLength() > 3) {
			Node dependenciesNode = ivyReportElement.getChildNodes().item(3);

			NodeList dependenciesNodeList = dependenciesNode.getChildNodes();

			for(int i = 0; i < dependenciesNodeList.getLength(); i++) {
				Node moduleNode = dependenciesNodeList.item(i);
				if(moduleNode.getNodeName().equals("module")) {
					Node revisionNode = moduleNode.getChildNodes().item(1);

					for(
						int j = 0; j < revisionNode.getChildNodes().getLength();
						j++) {

						Node artifactsNode =
							revisionNode.getChildNodes().item(j);

						if(artifactsNode.getNodeName().equals("artifacts")) {
							Node artifactNode =
								artifactsNode.getChildNodes().item(1);

							Node locationNode =
								artifactNode.getAttributes()
									.getNamedItem("location");

							String value = locationNode.getNodeValue();

							try (
								PrintWriter printWriter = new PrintWriter(
									new BufferedWriter(
										new FileWriter(
										"portal/ivy-dependencies.list",
											true)))) {

								printWriter.print(value + ":");
							}
						}
					}
				}
			}
		}
	}

}