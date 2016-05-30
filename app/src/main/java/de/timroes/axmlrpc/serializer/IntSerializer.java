package de.timroes.axmlrpc.serializer;

import org.w3c.dom.Element;

import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLUtil;
import de.timroes.axmlrpc.xmlcreator.XmlElement;

/**
 *
 * @author timroes
 */
public class IntSerializer implements Serializer {

	public Object deserialize(Element content) throws XMLRPCException {
		return Integer.parseInt(XMLUtil.getOnlyTextContent(content.getChildNodes()));
	}

	public XmlElement serialize(Object object) {
		return XMLUtil.makeXmlTag(SerializerHandler.TYPE_INT,
				object.toString());
	}

}
