package test.pojo.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.List;
import test.pojo.Child;
import test.pojo.Foo3;
import test.pojo.metafields.ReferenceWithMetaGrandChild;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperators.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class Foo3Validator implements Validator<Foo3> {

	private List<ComparisonResult> getComparisonResults(Foo3 o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("attr", (Integer) o.getAttr() != null ? 1 : 0, 1, 1), 
				checkCardinality("numberAttr", (Integer) o.getNumberAttrOverriddenAsInteger() != null ? 1 : 0, 1, 1), 
				checkCardinality("parent", (Child) o.getParent() != null ? 1 : 0, 1, 1), 
				checkCardinality("parentList", (ReferenceWithMetaGrandChild) o.getParentListOverriddenAsReferenceWithMetaGrandChild() != null ? 1 : 0, 1, 1), 
				checkCardinality("stringAttr", (FieldWithMetaString) o.getStringAttr() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Foo3 o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Foo3", ValidationResult.ValidationType.CARDINALITY, "Foo3", path, "", res.getError());
				}
				return success("Foo3", ValidationResult.ValidationType.CARDINALITY, "Foo3", path, "");
			})
			.collect(toList());
	}

}
