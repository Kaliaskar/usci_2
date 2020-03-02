package kz.bsbnb.usci.eav.meta.xsd;

import kz.bsbnb.usci.eav.model.meta.*;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * @author Maksat Nussipzhan
 * @author Jandos Iskakov
 */

@Component
public class XsdGenerator {

    public void generate(OutputStream schema, List<MetaClass> metaClasses) {
        PrintStream ps = new PrintStream(schema);

        ps.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        ps.println("<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">");
        ps.println();
        ps.println("<xsd:element name=\"batch\" type=\"batch\"/>");
        ps.println("<xsd:element name=\"entities\" type=\"entities\"/>");
        ps.println();
        ps.println("<xsd:complexType name=\"batch\">");
        ps.println("<xsd:all>");
        ps.println("<xsd:element name=\"entities\" type=\"entities\"/>");
        ps.println("</xsd:all>");
        ps.println("</xsd:complexType>");
        ps.println();

        ps.println("<xsd:simpleType name=\"operation\">");
        ps.println("<xsd:restriction base=\"xsd:string\">");
        ps.println("<xsd:enumeration value=\"DELETE\"/>");
        ps.println("<xsd:enumeration value=\"OPEN\"/>");
        ps.println("<xsd:enumeration value=\"CLOSE\"/>");
        ps.println("<xsd:enumeration value=\"INSERT\"/>");
        ps.println("<xsd:enumeration value=\"NEW\"/>");
        ps.println("<xsd:enumeration value=\"CHECKED_REMOVE\"/>");
        ps.println("</xsd:restriction>");
        ps.println("</xsd:simpleType>");

        ps.println();
        ps.println("<xsd:complexType name=\"entities\">");
        ps.println("<xsd:choice minOccurs=\"1\" maxOccurs=\"unbounded\">");

        // в блок entities попадают только сущности которые можно высылать отдельно
        // например по продукту "кредиты", отдельно можно высылать только три мета класса: кредиты, заемщик(субьект), портфолио
        // то есть отдельно выслать адрес не получиться так как он в составе субьекта
        for (MetaClass metaClass : metaClasses) {
            ps.println("<xsd:element name=\"" + metaClass.getClassName() + "\">");
            ps.println("<xsd:annotation>");
            ps.println("<xsd:documentation>"+metaClass.getClassTitle()+"</xsd:documentation>");
            ps.println("</xsd:annotation>");
            ps.println("<xsd:complexType>");
            ps.println("<xsd:complexContent>");
            ps.println("<xsd:extension base=\"" + metaClass.getClassName() + "\">");
            ps.println("<xsd:attribute name=\"operation\" type=\"operation\" use=\"optional\"/>");
            ps.println("</xsd:extension>");
            ps.println("</xsd:complexContent>");
            ps.println("</xsd:complexType>");
            ps.println("</xsd:element>");
        }

        ps.println("</xsd:choice>");
        ps.println("</xsd:complexType>");

        // извлекаем комплексные атрибуты из разных уровней мета класса
        // ранее был механизи ссылочных атрибутов который позволял высылать только ключевые атрибуты по ним
        // он был отключен так как старые батчи сформированы с полным набором атрибутов
        Set<MetaClass> complexTypes = new HashSet<>(metaClasses);

        for (MetaClass metaClass : metaClasses)
            complexTypes.addAll(getComplexTypes(metaClass));

        Map<String, Boolean> setMemberTypes = new HashMap<>();

        for (MetaClass metaClass : complexTypes) {
            ps.println();
            ps.println("<xsd:complexType name=\"" + metaClass.getClassName() + "\">");
            ps.println("<xsd:annotation>");
            ps.println("<xsd:documentation>"+metaClass.getClassTitle()+"</xsd:documentation>");
            ps.println("</xsd:annotation>");
            ps.println("<xsd:all>");

            boolean hasMultipleKeySet = hasMultipleKeySet(metaClass);

            for (MetaAttribute metaAttribute : metaClass.getAttributes())
                printAttribute(metaClass, hasMultipleKeySet, metaAttribute, ps, setMemberTypes);

            ps.println("</xsd:all>");
            ps.println("</xsd:complexType>");
        }

        for (String setMemberType : setMemberTypes.keySet()) {
            ps.println();
            ps.println("<xsd:complexType name=\"" + setMemberType + "_set" + "\">");
            ps.println("<xsd:sequence>");

            boolean isComplex = setMemberTypes.get(setMemberType);
            String elementType = isComplex ? setMemberType : getSimpleType(MetaDataType.valueOf(setMemberType));
            printElement(ps, Element.create("item", elementType, "0", "unbounded", "false", null));

            ps.println("</xsd:sequence>");
            ps.println("</xsd:complexType>");
        }

        ps.println();

        printDateType(ps);

        ps.println();

        printDateTypeTime(ps);

        ps.println();

        printDoubleType(ps);

        ps.println();

        printNonEmptyString(ps);

        ps.println();

        ps.println("</xsd:schema>");

        ps.close();
    }

    // проверяет еслть ли несколько групп ключей в мета классе
    private boolean hasMultipleKeySet(MetaClass metaClass) {
        if (!metaClass.isSearchable())
            return false;

        return metaClass.getAttributes().stream()
                .filter(MetaAttribute::isKey)
                .map(MetaAttribute::getKeySet)
                .distinct().count() > 1;
    }

    /**
     * Собирает все комплексные атрибуты (включая ссылки) мета класса
     */
    private Set<MetaClass> getComplexTypes(MetaClass metaClass) {
        Set<MetaClass> metaClasses = new HashSet<>();

        Queue<MetaClass> queue = new LinkedList<>(Collections.singletonList(metaClass));

        while(queue.size() > 0) {
            MetaClass meta = queue.poll();

            if (metaClasses.contains(meta))
                continue;

            metaClasses.add(meta);

            for (MetaAttribute childAttr : meta.getAttributes()) {
                MetaType childMetaType = childAttr.getMetaType();

                if (!childMetaType.isComplex())
                    continue;

                MetaClass childMetaClass;
                if (childMetaType.isSet()) {
                    MetaSet childMetaSet = (MetaSet) childMetaType;
                    childMetaClass = (MetaClass) childMetaSet.getMetaType();
                } else {
                    childMetaClass = (MetaClass) childMetaType;
                }

                queue.add(childMetaClass);
            }
        }

        return metaClasses;
    }

    private void printDateType(PrintStream ps) {
        ps.println("<xsd:simpleType name=\"date\">");
        ps.println("<xsd:restriction base=\"xsd:string\">");
        ps.println("<xsd:pattern value=\"|[0-9][0-9].[0-9][0-9].[0-9][0-9][0-9][0-9]\"/>");
        ps.println("</xsd:restriction>");
        ps.println("</xsd:simpleType>");
    }

    private void printDateTypeTime(PrintStream ps) {
        ps.println("<xsd:simpleType name=\"date_time\">");
        ps.println("<xsd:restriction base=\"xsd:string\">");
        ps.println("<xsd:pattern value=\"|[0-9][0-9].[0-9][0-9].[0-9][0-9][0-9][0-9] [0-9][0-9]:[0-9][0-9]:[0-9][0-9]\"/>");
        ps.println("</xsd:restriction>");
        ps.println("</xsd:simpleType>");
    }

    private void printDoubleType(PrintStream ps) {
        ps.println("<xsd:simpleType name=\"doubleOrEmpty\">");
        ps.println("<xsd:union memberTypes=\"xsd:double\">");
        ps.println("<xsd:simpleType>");
        ps.println("<xsd:restriction base=\"xsd:string\">");
        ps.println("<xsd:length value=\"0\"/>");
        ps.println("</xsd:restriction>");
        ps.println("</xsd:simpleType>");
        ps.println("</xsd:union>");
        ps.println("</xsd:simpleType>");
    }

    private void printNonEmptyString(PrintStream ps) {
        ps.println("<xsd:simpleType name=\"nonEmptyString\">");
        ps.println("<xsd:restriction base=\"xsd:string\">");
        ps.println("<xsd:minLength value=\"1\"/>");
        ps.println("</xsd:restriction>");
        ps.println("</xsd:simpleType>");
    }

    private void printAttribute(MetaClass metaClass, boolean hasMultipleKeySet,
                                MetaAttribute metaAttribute, PrintStream ps,
                                Map<String, Boolean> setTypes) {
        Element element;
        String minOccurs;
        String nillable;

        if (metaClass.getClassName().equals("portfolio_flow_detail")) {
            minOccurs = "0";
            nillable = "true";
        } else {
            // группы ключей контролировать не возможно так как могут выслать только один из ключей
            minOccurs = metaAttribute.isKey() && !hasMultipleKeySet ? "1" : "0";
            nillable = metaAttribute.isKey() && !hasMultipleKeySet ? "false" : "true";
        }

        String maxOccurs = "1";

        MetaType metaType = metaAttribute.getMetaType();

        if (metaType.isSet()) {
            MetaSet metaSet = (MetaSet) metaType;

            if (metaType.isComplex()) {
                MetaClass childMetaSetClass = (MetaClass) metaSet.getMetaType();
                element = Element.create(
                        metaAttribute.getName(), childMetaSetClass.getClassName() + "_set", minOccurs, maxOccurs, nillable, metaAttribute.getTitle());
                setTypes.put(childMetaSetClass.getClassName(), true);
            } else {
                MetaDataType childMetaSetType = metaSet.getMetaDataType();
                element = Element.create(
                        metaAttribute.getName(), childMetaSetType.name() + "_set", minOccurs, maxOccurs, nillable, metaAttribute.getTitle());
                setTypes.put(childMetaSetType.name(), false);
            }
        } else if (metaType.isComplex()) {
            MetaClass childMetaClass = (MetaClass) metaType;

            element = Element.create(
                    metaAttribute.getName(), childMetaClass.getClassName(), minOccurs, maxOccurs, nillable, metaAttribute.getTitle());
        } else {
            MetaValue childMetaValue = (MetaValue) metaType;
            element = Element.create(
                    metaAttribute.getName(), getSimpleType(childMetaValue.getMetaDataType()), minOccurs, maxOccurs,
                    nillable, !metaClass.isDictionary() && metaAttribute.isKey(), metaAttribute.getTitle());
        }

        printElement(ps, element);
    }

    private String getSimpleType(MetaDataType typeCode) {
        switch (typeCode) {
            case INTEGER:
                return "xsd:int";
            case STRING:
                return "xsd:string";
            case DOUBLE:
                return "doubleOrEmpty";
            case BOOLEAN:
                return "xsd:boolean";
            case DATE:
                return "date";
            case DATE_TIME:
                return "date_time";
            default:
                throw new IllegalArgumentException();
        }
    }

    private static class Element {
        private final String name;
        private final String type;
        private final String minOccurs;
        private final String maxOccurs;
        private final String nillable;
        private final boolean isSimpleKey;
        private final String title;

        private Element(String name, String type, String minOccurs, String maxOccurs, String nillable, boolean isSimpleKey, String title) {
            this.name = name;
            this.type = type;
            this.minOccurs = minOccurs;
            this.maxOccurs = maxOccurs;
            this.nillable = nillable;
            this.isSimpleKey = isSimpleKey;
            this.title = title;
        }

        static Element create(String name, String type, String minOccurs, String maxOccurs, String nillable, String title) {
            return new Element(name, type, minOccurs, maxOccurs, nillable, false, title);
        }

        static Element create(String name, String type, String minOccurs, String maxOccurs, String nillable, boolean isSimpleKey, String title) {
            return new Element(name, type, minOccurs, maxOccurs, nillable, isSimpleKey, title);
        }
    }

    private void printElement(PrintStream ps, Element element) {
        if (element.isSimpleKey) {
            ps.printf("<xsd:element name=\"%s\" minOccurs=\"%s\" maxOccurs=\"%s\" nillable=\"%s\">\n",
                    element.name, element.minOccurs, element.maxOccurs, element.nillable);
            if(element.title != null) {
                ps.println("<xsd:annotation>");
                ps.println("<xsd:documentation>"+element.title+"</xsd:documentation>");
                ps.println("</xsd:annotation>");
            }
                ps.println("<xsd:complexType>");
                    ps.println("<xsd:simpleContent>");
                        if (element.type.equals("xsd:string")) {
                            ps.println("<xsd:extension base=\"nonEmptyString\">");
                            ps.println("<xsd:attribute type=\"operation\" name=\"operation\" use=\"optional\"/>");
                            ps.println("<xsd:attribute type=\"nonEmptyString\" name=\"data\" use=\"optional\"/>");
                            ps.println("</xsd:extension>");
                        } else {
                            ps.printf("<xsd:extension base=\"%s\">\n", element.type);
                            ps.println("<xsd:attribute type=\"operation\" name=\"operation\" use=\"optional\"/>");
                            ps.printf("<xsd:attribute type=\"%s\" name=\"data\" use=\"optional\"/>\n", element.type);
                            ps.println("</xsd:extension>");
                        }
                    ps.println("</xsd:simpleContent>");
                ps.println("</xsd:complexType>");
            ps.println("</xsd:element>");
        } else {
            if(element.title != null) {
                if (element.nillable.equals("false")) {
                    if (element.type.equals("xsd:string")) {
                        ps.printf("<xsd:element name=\"%s\" minOccurs=\"%s\" maxOccurs=\"%s\" nillable=\"%s\">\n",
                                element.name, element.minOccurs, element.maxOccurs, element.nillable);
                        ps.println("<xsd:annotation>");
                        ps.println("<xsd:documentation>"+element.title+"</xsd:documentation>");
                        ps.println("</xsd:annotation>");
                        ps.println("<xsd:simpleType>");
                        ps.printf("<xsd:restriction base=\"%s\">\n", element.type);
                        ps.println("<xsd:minLength value=\"1\" />");
                        ps.println("</xsd:restriction>");
                        ps.println("</xsd:simpleType>");
                    } else {
                        ps.printf("<xsd:element name=\"%s\" type=\"%s\" minOccurs=\"%s\" maxOccurs=\"%s\" nillable=\"%s\">\n",
                                element.name, element.type, element.minOccurs, element.maxOccurs, element.nillable);
                        ps.println("<xsd:annotation>");
                        ps.println("<xsd:documentation>"+element.title+"</xsd:documentation>");
                        ps.println("</xsd:annotation>");
                    }
                } else {
                    ps.printf("<xsd:element name=\"%s\" type=\"%s\" minOccurs=\"%s\" maxOccurs=\"%s\" nillable=\"%s\">\n",
                            element.name, element.type, element.minOccurs, element.maxOccurs, element.nillable);
                    ps.println("<xsd:annotation>");
                    ps.println("<xsd:documentation>"+element.title+"</xsd:documentation>");
                    ps.println("</xsd:annotation>");
                }
                ps.println("</xsd:element>");
            } else {
                ps.printf("<xsd:element name=\"%s\" type=\"%s\" minOccurs=\"%s\" maxOccurs=\"%s\" nillable=\"%s\"/>\n",
                        element.name, element.type, element.minOccurs, element.maxOccurs, element.nillable);
            }

        }
    }

}
