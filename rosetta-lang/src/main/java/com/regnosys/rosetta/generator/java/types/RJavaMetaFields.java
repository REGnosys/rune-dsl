package com.regnosys.rosetta.generator.java.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.regnosys.rosetta.generator.java.util.ModelGeneratorUtil;
import com.rosetta.model.metafields.MetaFields;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;
import com.rosetta.util.types.JavaTypeDeclaration;

import static com.rosetta.model.lib.SerializedNameConstants.*;

public class RJavaMetaFields extends JavaPojoInterface  {
	private final JavaTypeUtil javaTypeUtil;

	public RJavaMetaFields(JavaTypeUtil javaTypeUtil) {
		this.javaTypeUtil = javaTypeUtil;
	}

	@Override
	public boolean isSubtypeOf(JavaType other) {
		if (javaTypeUtil.ROSETTA_MODEL_OBJECT.isSubtypeOf(other)) {
			return true;
		}
		if (javaTypeUtil.GLOBAL_KEY_FIELDS.isSubtypeOf(other)) {
			return true;
		}
		if (javaTypeUtil.META_DATA_FIELDS.isSubtypeOf(other)) {
			return true;
		}
		return false;
	}

	@Override
	public String getSimpleName() {
		return MetaFields.class.getSimpleName();
	}

	@Override
	public List<? extends JavaTypeDeclaration<?>> getInterfaceDeclarations() {
		return List.of(javaTypeUtil.ROSETTA_MODEL_OBJECT, javaTypeUtil.GLOBAL_KEY_FIELDS, javaTypeUtil.META_DATA_FIELDS);
	}

	@Override
	public String getJavadoc() {
		return ModelGeneratorUtil.javadoc(null, Collections.emptyList(), "1");
	}

	@Override
	public String getRosettaName() {
		return getSimpleName();
	}

	@Override
	public String getVersion() {
		return "0.0.0";
	}

	@Override
	public Collection<JavaPojoProperty> getOwnProperties() {
		return List.of(
					new JavaPojoProperty("scheme", "scheme", SCHEME, "scheme", javaTypeUtil.STRING, null, null, false),
					new JavaPojoProperty("address", "reference", SCOPED_REFERENCE, "address", javaTypeUtil.STRING, null, null, false),
					new JavaPojoProperty("globalKey", List.of(), KEY, "globalKey", javaTypeUtil.STRING, null, null, false),
					new JavaPojoProperty("externalKey", List.of("id", "key"), EXTERNAL_KEY, "externalKey", javaTypeUtil.STRING, null, null, false),
					new JavaPojoProperty("scopedKey", "location", SCOPED_KEY, "scopedKey", javaTypeUtil.STRING, null, null, false)
				);
	}

	@Override
	public Collection<JavaPojoProperty> getAllProperties() {
		return getOwnProperties();
	}

	@Override
	public JavaPojoInterface getSuperPojo() {
		return null;
	}

	@Override
	public DottedPath getPackageName() {
		return DottedPath.splitOnDots(MetaFields.class.getPackageName());
	}

	@Override
	public List<JavaClass<?>> getInterfaces() {
		return List.of(javaTypeUtil.ROSETTA_MODEL_OBJECT, javaTypeUtil.GLOBAL_KEY_FIELDS, javaTypeUtil.META_DATA_FIELDS);
	}

}
