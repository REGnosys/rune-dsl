package com.regnosys.rosetta.utils;

import com.google.common.collect.Comparators;
import com.regnosys.rosetta.RosettaEcoreUtil;
import com.regnosys.rosetta.rosetta.Import;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.RosettaRootElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;

public class ImportManagementService {

	@Inject
	RosettaEcoreUtil rosettaEcoreUtil;
	@Inject
	IQualifiedNameProvider qualifiedNameProvider;
	
	private Comparator<Import> importComparator = Comparator.comparing(Import::getImportedNamespace, Comparator.nullsLast(String::compareTo));

	public void cleanupImports(RosettaModel model) {
		EList<Import> imports = model.getImports();

		// remove all duplicate/unused imports
		List<Import> unusedImports = findUnused(model);
		imports.removeAll(unusedImports);

		List<Import> duplicateImports = findDuplicates(imports);
		imports.removeAll(duplicateImports);

		sortImports(imports);
	}

	public List<Import> findUnused(RosettaModel model) {
		List<QualifiedName> usedNames = new ArrayList<>();

		model.eAllContents().forEachRemaining(content -> {
			content.eCrossReferences().stream().filter(ref -> ref instanceof RosettaRootElement)
					.filter(ref -> rosettaEcoreUtil.isResolved((RosettaRootElement) ref)).forEach(ref -> {
						// Extract fully qualified name and add it to the list
						QualifiedName fullyQualifiedName = qualifiedNameProvider
								.getFullyQualifiedName((RosettaRootElement) ref);
						usedNames.add(fullyQualifiedName);
					});
		});

		List<Import> unusedImports = new ArrayList<>();
		for (Import ns : model.getImports()) {
			if (ns.getImportedNamespace() != null) {

				String[] segments = ns.getImportedNamespace().split("\\.");
				QualifiedName qn = QualifiedName.create(segments);
				boolean isWildcard = "*".equals(qn.getLastSegment());

				// Check if the import is used
				boolean isUsed;
				if (isWildcard) {
					isUsed = usedNames.stream().anyMatch(name -> name.skipLast(1).equals(qn.skipLast(1))
							&& name.getSegmentCount() == qn.getSegmentCount());
				} else {
					isUsed = usedNames.contains(qn);
				}

				if (!isUsed) {
					unusedImports.add(ns);
				}
			}
		}

		return unusedImports;

	}

	public List<Import> findDuplicates(List<Import> imports) {
		Set<String> seenNamespaces = new HashSet<String>();
		List<Import> duplicates = new ArrayList<Import>();
		// check duplicates
		for (Import imp : imports) {
			if (!(seenNamespaces.add(imp.getImportedNamespace()))) {
				duplicates.add(imp);
			}
		}
		return duplicates;
	}

	public String toString(List<Import> imports) {
		StringBuilder sortedImportsText = new StringBuilder();
		
		Import previousImport = null;
		for (Import imp : imports) {
			//if previous import comes from a different package, insert new line
			if (previousImport != null) {
				String previousFirstSegment = previousImport.getImportedNamespace().split("\\.")[0];
				String currentFirstSegment = imp.getImportedNamespace().split("\\.")[0];
				if(!previousFirstSegment.equals(currentFirstSegment)) {
					sortedImportsText.append("\n");
				}
			}
			sortedImportsText.append("import ").append(imp.getImportedNamespace());
			if (imp.getNamespaceAlias() != null) {
				sortedImportsText.append(" as ").append(imp.getNamespaceAlias());
			}
			sortedImportsText.append("\n");
			
			
			previousImport = imp;
		}

		return sortedImportsText.toString().strip();
	}

	public boolean isSorted(List<Import> imports) {
		return Comparators.isInOrder(imports, importComparator);
	}

	public void sortImports(EList<Import> imports) {
		ECollections.sort(imports, importComparator);
	}
}
