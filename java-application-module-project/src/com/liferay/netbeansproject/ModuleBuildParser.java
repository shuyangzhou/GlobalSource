package com.liferay.netbeansproject;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
public class ModuleBuildParser {

	public static String parseBuildFile(String modulePath) throws Exception {
		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		File buildFile = new File(modulePath + "/build.xml");

		if (!buildFile.exists()) {
			return "";
		}

		XPathFactory xPathFactory = XPathFactory.newInstance();

		XPath xPath = xPathFactory.newXPath();

		XPathExpression xPathExpression = xPath.compile(
			"/project/property[@name=\"import.shared\"]/@value");

		String importShared = xPathExpression.evaluate(
			documentBuilder.parse(buildFile));

		String[] importSharedSplit = importShared.split(",");

		StringBuilder sb = new StringBuilder();

		for (String module : importSharedSplit) {
			String[] moduleSplit = module.split("/");
			sb.append(moduleSplit[moduleSplit.length-1]);
			sb.append(":");
		}

		sb.setLength(sb.length() - 1);

		return sb.toString();
	}

}