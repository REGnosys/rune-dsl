package com.regnosys.rosetta.tools.modelimport;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.regnosys.rosetta.rosetta.RosettaNamed;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.xmlet.xsdparser.core.XsdParser;
import org.xmlet.xsdparser.xsdelements.XsdAbstractElement;
import org.xmlet.xsdparser.xsdelements.XsdComplexType;
import org.xmlet.xsdparser.xsdelements.XsdElement;
import org.xmlet.xsdparser.xsdelements.XsdSchema;

import com.google.common.collect.Streams;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.RosettaRootElement;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.types.RDataType;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;
import com.rosetta.util.serialisation.TypeXMLConfiguration;

public class XsdImport {
	public final String ENUM = "enum";
	public final String TYPE = "type";

	private final RosettaModelFactory rosettaModelFactory;
	private final XsdElementImport xsdElementImport;
	private final XsdTypeImport xsdTypeImport;
	private final XsdEnumImport xsdEnumImport;
	private final XsdTypeAliasImport xsdTypeAliasImport;
	private final RosettaXsdMapping xsdMapping;

	@Inject
	public XsdImport(RosettaModelFactory rosettaModelFactory, XsdElementImport xsdElementImport, XsdTypeImport xsdTypeImport, XsdEnumImport xsdEnumImport, XsdTypeAliasImport xsdTypeAliasImport, RosettaXsdMapping xsdMapping) {
		this.rosettaModelFactory = rosettaModelFactory;
		this.xsdElementImport = xsdElementImport;
		this.xsdTypeImport = xsdTypeImport;
		this.xsdEnumImport = xsdEnumImport;
		this.xsdTypeAliasImport = xsdTypeAliasImport;
		this.xsdMapping = xsdMapping;
	}

	public ResourceSet generateRosetta(XsdParser parsedInstance, GenerationProperties properties) {
		List<XsdAbstractElement> xsdElements = parsedInstance.getResultXsdSchemas().flatMap(XsdSchema::getXsdElements).toList();
		
		// Initialization
		xsdMapping.initializeBuiltins(rosettaModelFactory.getResourceSet());
		
		// First register all rosetta types and attributes, which makes it possible to support
		// forward references and self-references.
		List<? extends RosettaRootElement> enums = xsdEnumImport.registerTypes(xsdElements, xsdMapping, properties);
		List<? extends RosettaRootElement> aliases = xsdTypeAliasImport.registerTypes(xsdElements, xsdMapping, properties);
		List<? extends Data> elements = xsdElementImport.registerTypes(xsdElements, xsdMapping, properties);
		List<? extends Data> types = xsdTypeImport.registerTypes(xsdElements, xsdMapping, properties)
				.stream().flatMap(Collection::stream).toList();

        // Post process to circumvent name conflicts:
        Set<String> elementNames = elements.stream().map(RosettaNamed::getName).collect(Collectors.toSet());
        Set<String> typeNames = types.stream().map(RosettaNamed::getName).collect(Collectors.toSet());
        types.stream().filter(t -> elementNames.contains(t.getName())).forEach(t -> {
            String newName = t.getName() + "Type";
            if (!typeNames.contains(newName)) {
                t.setName(t.getName() + "Type");
            } else {
                elements.stream().filter(e -> e.getName().equals(t.getName())).findAny().ifPresent(elem -> elem.setName(t.getName() + "Element"));
            }
        });

		// Then write these types to the appropriate resources.
		if (!enums.isEmpty()) {
			RosettaModel enumModel = rosettaModelFactory.createRosettaModel(ENUM, properties);
			enumModel.getElements().addAll(enums);
		}
		
		if (!aliases.isEmpty() || !elements.isEmpty() || !types.isEmpty()) {
			RosettaModel typeModel = rosettaModelFactory.createRosettaModel(TYPE, properties);
			typeModel.getElements().addAll(aliases);
			typeModel.getElements().addAll(elements);
			typeModel.getElements().addAll(types);
		}
		
		// Then fill in the contents of these types.
		xsdEnumImport.completeTypes(xsdElements, xsdMapping);
		xsdTypeAliasImport.completeTypes(xsdElements, xsdMapping);
		xsdElementImport.completeTypes(xsdElements, xsdMapping);
		xsdTypeImport.completeTypes(xsdElements, xsdMapping);

		return rosettaModelFactory.getResourceSet();
	}
	
	public RosettaXMLConfiguration generateXMLConfiguration(XsdParser parsedInstance, GenerationProperties properties) {
		Map<String, List<XsdAbstractElement>> targetNamespaceToXsdElementsMap = 
				parsedInstance.getResultXsdSchemas()
					.collect(Collectors.toMap(
                            XsdSchema::getTargetNamespace,
                            XsdSchema::getXsdElements,
                            Streams::concat))
					.entrySet()
					.stream()
					.collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().toList()));
				
		Map<ModelSymbolId, TypeXMLConfiguration> result = new HashMap<>();
		targetNamespaceToXsdElementsMap.forEach((targetNamespace, xsdElements) -> {
			xsdElements.forEach(abstractElem -> {
				if (abstractElem instanceof XsdElement xsdElem) {
                    xsdElementImport.getXMLConfiguration(xsdElem, xsdMapping, targetNamespace)
						.ifPresent(elemXMLConfig -> {
							Data type = xsdMapping.getRosettaTypeFromElement(xsdElem);
							result.put(new RDataType(type).getSymbolId(), elemXMLConfig);
						});
				} else if (abstractElem instanceof XsdComplexType xsdType) {
                    xsdTypeImport.getXMLConfiguration(xsdType, xsdMapping, targetNamespace)
						.ifPresent(typeXMLConfig -> {
							Data type = xsdMapping.getRosettaTypeFromComplex(xsdType);
							result.put(new RDataType(type).getSymbolId(), typeXMLConfig);
						});
				}
			});
		});
		return new RosettaXMLConfiguration(result);
	}

	public void saveResources(String outputPath) throws IOException {
		rosettaModelFactory.saveResources(outputPath);
	}
}
