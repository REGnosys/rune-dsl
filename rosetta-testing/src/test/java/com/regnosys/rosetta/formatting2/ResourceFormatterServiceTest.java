package com.regnosys.rosetta.formatting2;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.serializer.ISerializer;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.io.Resources;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class ResourceFormatterServiceTest {
	@Inject
	ResourceFormatterService formatterService;
	@Inject
	Provider<ResourceSet> resourceSetProvider;
	@Inject
	ISerializer serializer;

	private void testFormatting(Collection<String> inputUrls, Collection<String> expectedUrls)
			throws IOException, URISyntaxException {
		ResourceSet resourceSet = resourceSetProvider.get();
		List<Resource> resources = new ArrayList<>();
		List<String> expected = new ArrayList<>();

		for (String url : inputUrls) {
			Resource resource = resourceSet.getResource(URI.createURI(Resources.getResource(url).toString()), true);
			resources.add(resource);
		}

		for (String url : expectedUrls) {
			expected.add(Files.readString(Path.of(Resources.getResource(url).toURI())));
		}

		formatterService.formatCollection(resources);

		List<String> result = resources.stream().map(resource -> serializer.serialize(resource.getContents().get(0)))
				.collect(Collectors.toList());

		Assertions.assertIterableEquals(expected, result);
	}

	@Test
	void formatSingleDocument() throws IOException, URISyntaxException {
		testFormatting(List.of("formatting-test/input/typeAlias.rosetta"),
				List.of("formatting-test/expected/typeAlias.rosetta"));
	}

	@Test
	void formatMultipleDocuments() throws IOException, URISyntaxException {
		testFormatting(
				List.of("formatting-test/input/typeAlias.rosetta",
						"formatting-test/input/typeAliasWithDocumentation.rosetta"),
				List.of("formatting-test/expected/typeAlias.rosetta",
						"formatting-test/expected/typeAliasWithDocumentation.rosetta"));
	}
}
