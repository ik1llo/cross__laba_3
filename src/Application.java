package src;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Application {
    static Set<String> ethnicities = new HashSet<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("1) parse and show XML document");
            System.out.println("2) validate XML by XSD");
            System.out.println("3) show existing ethnicities");
            System.out.println("4) show the top names with ethnicity and gender specified");
            System.out.println("5) read and display with DOM parser");
            System.out.println("6) exit");
            System.out.print("[choice]: ");

            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    parse_n_show_XML_doc();
                    break;
                case 2:
                    validate_XML_by_XSD();
                    break;
                case 3:
                    show_ethnicities();
                    break;
                case 4:
                    show_top_names();
                    break;
                case 5:
                    read_and_display_DOM();
                    break;
                case 6:
                    System.exit(0);
                    break;
                default:
                    System.out.println("wrong choice number... Try again:");
            }
        } while (choice != 6);

        scanner.close();
    }

    static void parse_n_show_XML_doc() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser sax_parser = factory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {
                boolean status = false;

                @Override
                public void startElement(String uri, String local_name, String q_name, Attributes attributes) throws SAXException {
                    System.out.println("start element: " + q_name);
                    status = true;
                }

                @Override
                public void endElement(String uri, String local_name, String q_name) throws SAXException {
                    System.out.println("end element: " + q_name);
                    status = false;
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (status) { System.out.println("text: " + new String(ch, start, length)); }
                }
            };

            sax_parser.parse(new File("Popular_Baby_Names_NY.xml"), handler);
        } catch (ParserConfigurationException | SAXException | IOException e) { e.printStackTrace(); }
    }

    static void validate_XML_by_XSD() {
        try {
            File xsd_file = new File("Popular_Baby_Names_NY.xml");
            SchemaFactory schema_factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schema_factory.newSchema(xsd_file);

            System.out.println("XML document corresponds to XSD");
        } catch (SAXException e) { System.out.println("XML document does not corresponds to XSD"); }
    }

    static void show_ethnicities() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser sax_parser_ethnicity = factory.newSAXParser();

            DefaultHandler handler_ethnicity = new DefaultHandler() {
                boolean in_ethnicity = false;

                @Override
                public void startElement(String uri, String local_name, String q_name, Attributes attributes) throws SAXException {
                    if (q_name.equalsIgnoreCase("ethcty")) { in_ethnicity = true; }
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (in_ethnicity) {
                        String ethnicity = new String(ch, start, length).trim();
                        if (!ethnicity.isEmpty()) { ethnicities.add(ethnicity); }
                    }
                }

                @Override
                public void endElement(String uri, String local_name, String q_name) throws SAXException {
                    if (q_name.equalsIgnoreCase("ethcty")) { in_ethnicity = false; }
                }
            };

            sax_parser_ethnicity.parse(new File("Popular_Baby_Names_NY.xml"), handler_ethnicity);

            System.out.println("Ethnicity groups:");
            for (String ethnicity : ethnicities) { System.out.println(ethnicity); }
        } catch (ParserConfigurationException | SAXException | IOException e) { e.printStackTrace(); }
    }


    static void show_top_names() {
        try {
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter ethnicity: ");
            String ethnicity = scanner.nextLine();

            System.out.print("Enter gender (MALE or FEMALE): ");
            String gender = scanner.nextLine();

            List<BabyName> baby_names = new ArrayList<>();

            File input_fle = new File("Popular_Baby_Names_NY.xml");
            DocumentBuilderFactory db_factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder d_builder = db_factory.newDocumentBuilder();
            Document doc = d_builder.parse(input_fle);

            doc.getDocumentElement().normalize();

            NodeList n_list = doc.getElementsByTagName("row");
            for (int temp = 0; temp < n_list.getLength(); temp++) {
                Node n_node = n_list.item(temp);

                if (n_node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) n_node;
                    String ethnicity_node = element.getElementsByTagName("ethcty").item(0).getTextContent();
                    String gender_node = element.getElementsByTagName("gndr").item(0).getTextContent();

                    if (ethnicity_node.equalsIgnoreCase(ethnicity) && gender_node.equalsIgnoreCase(gender)) {
                        String name = element.getElementsByTagName("nm").item(0).getTextContent();

                        int count = Integer.parseInt(element.getElementsByTagName("cnt").item(0).getTextContent());
                        int rating = Integer.parseInt(element.getElementsByTagName("rnk").item(0).getTextContent());

                        baby_names.add(new BabyName(name, gender, count, rating, ethnicity_node));
                    }
                }
            }

            List<BabyName> merged_names = new ArrayList<>();
            for (BabyName name : baby_names) {
                boolean found = false;
                for (BabyName merged : merged_names) {
                    if (merged.getName().equalsIgnoreCase(name.getName())) {
                        merged.setCount(merged.getCount() + name.getCount());
                        found = true;
                        break;
                    }
                }

                if (!found) { merged_names.add(name); }
            }

            Collections.sort(merged_names);

            System.out.println("Top 10 popular names of " + ethnicity + " of " + gender + ":");
            for (int i = 0; i < Math.min(10, merged_names.size()); i++) {
                BabyName baby_name = merged_names.get(i);
                System.out.println("name: " + baby_name.getName() + ", gender: " + baby_name.getGender() + ", amount: " + baby_name.getCount() + ", rating: " + baby_name.getRating());
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder doc_builder = docFactory.newDocumentBuilder();
            Document new_doc = doc_builder.newDocument();

            Element root_element = new_doc.createElement("TopNames");
            new_doc.appendChild(root_element);

            for (int i = 0; i < Math.min(10, merged_names.size()); i++) {
                BabyName baby_name = merged_names.get(i);
                Element name_element = new_doc.createElement("Name");
                name_element.setAttribute("Name", baby_name.getName());
                name_element.setAttribute("Gender", baby_name.getGender());
                name_element.setAttribute("Amount", String.valueOf(baby_name.getCount()));
                name_element.setAttribute("Rating", String.valueOf(baby_name.getRating()));
                root_element.appendChild(name_element);
            }

            TransformerFactory transformer_factory = TransformerFactory.newInstance();
            Transformer transformer = transformer_factory.newTransformer();
            DOMSource source = new DOMSource(new_doc);
            StreamResult result = new StreamResult(new File("top_names.xml"));
        
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);

            System.out.println("Top 10 popular names of " + ethnicity + " of " + gender + " saved to top_names.xml");
            } catch (ParserConfigurationException | SAXException | IOException | NumberFormatException | TransformerException e) { e.printStackTrace(); }
    }

    static class BabyName implements Comparable<BabyName> {
        private String name;
        private String gender;
        private String ethnicity;

        private int count;
        private int rating;

        public BabyName(String name, String gender, int count, int rating, String ethnicity) {
            this.name = name;
            this.gender = gender;
            this.ethnicity = ethnicity;

            this.rating = rating;
            this.count = count;
        }

        public String getName() { return name; }

        public String getGender() { return gender; }

        public String getEthnicity() { return ethnicity; }

        public int getCount() { return count; }

        public void setCount(int count) { this.count = count; }

        public int getRating() { return rating; }

        @Override
        public int compareTo(BabyName o) { return Integer.compare(o.rating, this.rating); }
    }

    static void read_and_display_DOM() {
        try {
            File input_file = new File("top_names.xml");
            DocumentBuilderFactory db_factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder d_builder = db_factory.newDocumentBuilder();
            Document doc = d_builder.parse(input_file);
            doc.getDocumentElement().normalize();
    
            System.out.println("root element: " + doc.getDocumentElement().getNodeName());
            NodeList node_list = doc.getElementsByTagName("*");
            for (int temp = 0; temp < node_list.getLength(); temp++) {
                Node node = node_list.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    System.out.print("element: " + node.getNodeName());
                    if (node.hasAttributes()) {
                        NamedNodeMap node_map = node.getAttributes();
                        for (int i = 0; i < node_map.getLength(); i++) {
                            Node attr = node_map.item(i);
                            System.out.print(" [" + attr.getNodeName() + "=\"" + attr.getNodeValue() + "\"]");
                        }
                    }
                    System.out.println();
                    if (node.hasChildNodes()) {
                        System.out.println("text: " + node.getTextContent().trim());
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) { e.printStackTrace(); }
    }
}
