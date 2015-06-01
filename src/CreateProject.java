
import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CreateProject {
	public static void createConfiguration(Element projectElement) {
		Element configurationElement = document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		createData(configurationElement);

		createLibraries(configurationElement);
	}

	public static void createData(Element configurationElement) {
		Element dataElement = document.createElement("data");

		dataElement.setAttribute("xmlns", "http://www.netbeans.org/ns/j2se-project/3");

		configurationElement.appendChild(dataElement);

		Element nameElement = document.createElement("name");

		nameElement.appendChild(document.createTextNode(_projectName));

		dataElement.appendChild(nameElement);

		Element sourceRootsElement = document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		for(String module:_modules) {
			if(!(module.endsWith("-test")||module.endsWith("test-internal")))
			createRoots(sourceRootsElement,"src."+module+".dir", module);
		}

		Element testRootsElement = document.createElement("test-roots");

		dataElement.appendChild(testRootsElement);

		for(String test:_tests) {
			createRoots(testRootsElement, "test."+test+".dir", test);
		}

	}

	public static void createLibraries(Element configurationElement) {
		Element librariesElement = document.createElement("libraries");

		librariesElement.setAttribute("xmlns", "http://www.netbeans.org/ns/ant-project-libraries/1");

		configurationElement.appendChild(librariesElement);

		Element definitionsElement = document.createElement("definitions");

		definitionsElement.appendChild(document.createTextNode("./lib/nblibraries.properties"));

		librariesElement.appendChild(definitionsElement);
	}

	public static void createProjectElement() {
		Element projectElement = document.createElement("project");

		projectElement.setAttribute("xmlns", "http://www.netbeans.org/ns/project/1");

		document.appendChild(projectElement);

		Element typeElement = document.createElement("type");

		typeElement.appendChild(document.createTextNode("org.netbeans.modules.java.j2seproject"));

		projectElement.appendChild(typeElement);

		createConfiguration(projectElement);
	}

	public static void createRoots(Element sourceRootsElement, String module, String moduleName) {
		Element rootElement = document.createElement("root");

		rootElement.setAttribute("id", module);

		rootElement.setAttribute("name", moduleName);

		sourceRootsElement.appendChild(rootElement);
	}
	public static void main(String[] args) throws Exception {
		parseArgument(args);

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		document = documentBuilder.newDocument();

		createProjectElement();

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		DOMSource source = new DOMSource(document);

		StreamResult streamResult;

		streamResult = new StreamResult(new File("portal/nbproject/project.xml"));

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, streamResult);
	}

	public static void parseArgument(String[] args) {
		try {
			_projectName = args[0];

			_modules = args[1].split(",");

			_tests = args[2].split(",");
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.out.println(
				"Insufficient number of inputs, please use the following " +
				"order of inputs: Project Name, module list");

			System.exit(1);
		}
	}

	private static Document document;
	private static String _projectName;
	private static String[] _modules;
	private static String[] _tests;
}