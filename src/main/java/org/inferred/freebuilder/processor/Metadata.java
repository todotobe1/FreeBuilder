/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inferred.freebuilder.processor;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.addAll;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.inferred.freebuilder.processor.util.ImpliedClass;
import org.inferred.freebuilder.processor.util.ImpliedClass.ImpliedNestedClass;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

/**
 * Metadata about a &#64;{@link org.inferred.freebuilder.FreeBuilder FreeBuilder} type.
 */
public class Metadata {

  /** Standard Java methods that may be underridden. */
  public enum StandardMethod {
    TO_STRING, HASH_CODE, EQUALS
  }

  private final Elements elements;
  private final TypeElement type;
  private final TypeElement builder;
  @Nullable private final BuilderFactory builderFactory;
  private final ImpliedClass generatedBuilder;
  private final ImpliedNestedClass valueType;
  private final ImpliedNestedClass partialType;
  private final ImpliedNestedClass propertyEnum;
  private final List<Property> properties;
  private final Set<StandardMethod> underriddenMethods;
  private final boolean builderSerializable;
  private final boolean gwtSerializable;

  private Metadata(Builder builder) {
    this.elements = builder.elements;
    this.type = builder.type;
    this.builder = builder.builder;
    this.builderFactory = builder.builderFactory;
    this.generatedBuilder = builder.generatedBuilder;
    this.valueType = builder.valueType;
    this.partialType = builder.partialType;
    this.propertyEnum = builder.propertyEnum;
    this.properties = ImmutableList.copyOf(builder.properties);
    this.underriddenMethods = ImmutableSet.copyOf(builder.underriddenMethods);
    this.builderSerializable = builder.builderSerializable;
    this.gwtSerializable = builder.gwtSerializable;
  }

  /** Returns the package the type is in. */
  public PackageElement getPackage() {
    return elements.getPackageOf(type);
  }

  /** Returns the type itself. */
  public TypeElement getType() {
    return type;
  }

  /** Returns the builder type that users will see. */
  public TypeElement getBuilder() {
    return builder;
  }

  /** Returns the builder factory mechanism the user has exposed, if any. */
  public Optional<BuilderFactory> getBuilderFactory() {
    return Optional.fromNullable(builderFactory);
  }

  /** Returns the builder class that should be generated. */
  public ImpliedClass getGeneratedBuilder() {
    return generatedBuilder;
  }

  /** Returns the value class that should be generated. */
  public ImpliedNestedClass getValueType() {
    return valueType;
  }

  /** Returns the partial value class that should be generated. */
  public ImpliedNestedClass getPartialType() {
    return partialType;
  }

  /** Returns the Property enum that may be generated. */
  public ImpliedNestedClass getPropertyEnum() {
    return propertyEnum;
  }

  /** Returns metadata about the properies of the type. */
  public List<Property> getProperties() {
    return properties;
  }

  public Set<StandardMethod> getUnderriddenMethods() {
    return underriddenMethods;
  }

  /** Returns whether the builder type should be serializable. */
  public boolean isBuilderSerializable() {
    return builderSerializable;
  }

  /** Returns whether the type (and hence the generated value type) is GWT serializable. */
  public boolean isGwtSerializable() {
    return gwtSerializable;
  }

  /** Metadata about a property of a {@link Metadata}. */
  public static class Property {
    private final TypeMirror type;
    private final TypeMirror boxedType;
    private final String name;
    private final String capitalizedName;
    private final String getterName;
    private final String allCapsName;
    private final PropertyCodeGenerator codeGenerator;
    private final boolean fullyCheckedCast;

    private Property(Builder builder) {
      this.type = builder.type;
      this.boxedType = builder.boxedType;
      this.name = builder.name;
      this.capitalizedName = builder.capitalizedName;
      this.allCapsName = builder.allCapsName;
      this.getterName = builder.getterName;
      this.codeGenerator = builder.codeGenerator;
      this.fullyCheckedCast = builder.fullyCheckedCast;
    }

    /** Returns the type of the property. */
    public TypeMirror getType() {
      return type;
    }

    /** Returns the boxed form of {@link #getType()}, or null if type is not primitive. */
    public TypeMirror getBoxedType() {
      return boxedType;
    }

    /** Returns the name of the property, e.g. myProperty. */
    public String getName() {
      return name;
    }

    /** Returns the capitalized name of the property, e.g. MyProperty. */
    public String getCapitalizedName() {
      return capitalizedName;
    }

    /** Returns the name of the property in all-caps with underscores, e.g. MY_PROPERTY. */
    public String getAllCapsName() {
      return allCapsName;
    }

    /** Returns the name of the getter for the property, e.g. getMyProperty, or isSomethingTrue. */
    public String getGetterName() {
      return getterName;
    }

    /**
     * Returns the code generator to use for this property, or null if no generator has been picked
     * (i.e. when passed to {@link PropertyCodeGenerator.Factory#create}.
     */
    public PropertyCodeGenerator getCodeGenerator() {
      return codeGenerator;
    }

    /**
     * Returns true if a cast to this property type is guaranteed to be fully checked at runtime.
     * This is true for any type that is non-generic, raw, or parameterized with unbounded
     * wildcards, such as {@code Integer}, {@code List} or {@code Map<?, ?>}.
     */
    public boolean isFullyCheckedCast() {
      return fullyCheckedCast;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(
          allCapsName,
          boxedType.toString(),
          capitalizedName,
          codeGenerator,
          getterName,
          name,
          type.toString(),
          fullyCheckedCast);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Property)) {
        return false;
      }
      Property other = (Property) obj;
      return equal(allCapsName, other.allCapsName)
          && equal(boxedType.toString(), other.boxedType.toString())
          && equal(capitalizedName, other.capitalizedName)
          && equal(codeGenerator, other.codeGenerator)
          && equal(getterName, other.getterName)
          && equal(name, other.name)
          && equal(type.toString(), other.type.toString())
          && equal(fullyCheckedCast, other.fullyCheckedCast);
    }

    @Override
    public String toString() {
      return "Property{type=" + type + ", boxedType=" + boxedType + ", name=" + name
          + ", capitalizedName=" + capitalizedName + ", getterName=" + getterName + ", allCapsName="
          + allCapsName + ", codeGenerator=" + codeGenerator + ", fullyCheckedCast="
          + fullyCheckedCast + "}";
    }

    /** Builder for {@link Property}. */
    public static class Builder {
      private TypeMirror type;
      private TypeMirror boxedType;
      private String name;
      private String capitalizedName;
      private String getterName;
      private String allCapsName;
      private PropertyCodeGenerator codeGenerator;
      private Boolean fullyCheckedCast;

      /** Sets the type of the property. */
      public Builder setType(TypeMirror type) {
        this.type = type;
        return this;
      }

      /** Sets the boxed type of the property (null if the original type is not primitive). */
      public Builder setBoxedType(TypeMirror type) {
        this.boxedType = type;
        return this;
      }

      /** Sets the name of the property. */
      public Builder setName(String name) {
        this.name = name;
        return this;
      }

      /** Sets the capitalized name of the property. */
      public Builder setCapitalizedName(String capitalizedName) {
        this.capitalizedName = capitalizedName;
        return this;
      }

      /** Sets the all-caps name of the property. */
      public Builder setAllCapsName(String allCapsName) {
        this.allCapsName = allCapsName;
        return this;
      }

      /** Sets the name of the getter for the property. */
      public Builder setGetterName(String getterName) {
        this.getterName = getterName;
        return this;
      }

      /** Sets the code generator to use for this property. */
      public Builder setCodeGenerator(PropertyCodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
        return this;
      }

      /**
       * Sets whether a cast to this property type is guaranteed to be fully checked by the
       * compiler.
       */
      public Builder setFullyCheckedCast(Boolean fullyCheckedCast) {
        this.fullyCheckedCast = checkNotNull(fullyCheckedCast);
        return this;
      }

      /** Returns a newly-built {@link Property} based on the content of the {@code Builder}. */
      public Property build() {
        checkState(type != null, "type not set");
        checkState(name != null, "name not set");
        checkState(capitalizedName != null, "capitalized not set");
        checkState(allCapsName != null, "allCapsName not set");
        checkState(getterName != null, "getter name not set");
        checkState(fullyCheckedCast != null, "fullyCheckedCast not set");
        // codeGenerator may be null
        return new Property(this);
      }
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        type.toString(),
        builder.toString(),
        builderFactory,
        generatedBuilder.toString(),
        valueType.toString(),
        partialType.toString(),
        propertyEnum.toString(),
        properties,
        underriddenMethods,
        builderSerializable,
        gwtSerializable);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Metadata)) {
      return false;
    }
    Metadata other = (Metadata) obj;
    return equal(type.toString(), other.type.toString())
        && equal(builder.toString(), other.builder.toString())
        && equal(builderFactory, other.builderFactory)
        && equal(generatedBuilder.toString(), other.generatedBuilder.toString())
        && equal(valueType.toString(), other.valueType.toString())
        && equal(partialType.toString(), other.partialType.toString())
        && equal(propertyEnum.toString(), other.propertyEnum.toString())
        && equal(properties, other.properties)
        && equal(underriddenMethods, other.underriddenMethods)
        && equal(builderSerializable, other.builderSerializable)
        && equal(gwtSerializable, other.gwtSerializable);
  }

  @Override
  public String toString() {
    return "Metadata{type=" + type + ", builder=" + builder
        + ", generatedBuilder=" + generatedBuilder + ", valueType=" + valueType + ", partialType="
        + partialType + ", propertyEnum=" + propertyEnum + ", properties=" + properties
        + ", underriddenMethods=" + underriddenMethods + ", builderSerializable="
        + builderSerializable + ", gwtSerializable=" + gwtSerializable + "}";
  }

  /** Builder for {@link Metadata}. */
  public static class Builder {

    private final Elements elements;
    private TypeElement type;
    private TypeElement builder;
    private BuilderFactory builderFactory;
    private ImpliedClass generatedBuilder;
    public ImpliedNestedClass valueType;
    public ImpliedNestedClass partialType;
    public ImpliedNestedClass propertyEnum;
    private final List<Property> properties = new ArrayList<Property>();
    private final Set<StandardMethod> underriddenMethods = EnumSet.noneOf(StandardMethod.class);
    private Boolean builderSerializable;
    private Boolean gwtSerializable;

    public Builder(Elements elements) {
      this.elements = checkNotNull(elements);
    }

    /** Sets the type the metadata object being built is referring to. */
    public Builder setType(TypeElement type) {
      this.type = checkNotNull(type);
      return this;
    }

    /** Sets the builder class that users will see. */
    public Builder setBuilder(TypeElement builder) {
      this.builder = checkNotNull(builder);
      return this;
    }

    /** Sets the builder factory mechanism the user has exposed. */
    public Builder setBuilderFactory(BuilderFactory builderFactory) {
      this.builderFactory = checkNotNull(builderFactory);
      return this;
    }

    /** Sets the builder factory mechanism the user has exposed, if any. */
    public Builder setBuilderFactory(Optional<BuilderFactory> builderFactory) {
      this.builderFactory = builderFactory.orNull();
      return this;
    }

    /** Sets the builder class that should be generated. */
    public Builder setGeneratedBuilder(ImpliedClass generatedBuilder) {
      this.generatedBuilder = checkNotNull(generatedBuilder);
      return this;
    }

    /** Sets the value type that should be generated. */
    public Builder setValueType(ImpliedNestedClass valueType) {
      this.valueType = valueType;
      return this;
    }

    /** Sets the partial type that should be generated. */
    public Builder setPartialType(ImpliedNestedClass partialType) {
      this.partialType = partialType;
      return this;
    }

    /** Sets the property enum that may be generated.  */
    public Builder setPropertyEnum(ImpliedNestedClass propertyEnum) {
      this.propertyEnum = propertyEnum;
      return this;
    }

    /** Adds metadata about a property of the type. */
    public Builder addProperty(Property property) {
      this.properties.add(property);
      return this;
    }

    /** Adds metadata about a set of properties of the type. */
    public Builder addAllProperties(Iterable<Property> properties) {
      addAll(this.properties, properties);
      return this;
    }

    /** Add an underridden standard method. */
    public Builder addUnderriddenMethod(StandardMethod standardMethod) {
      this.underriddenMethods.add(standardMethod);
      return this;
    }

    /** Add a set of underridden standard methods. */
    public Builder addAllUnderriddenMethods(Iterable<StandardMethod> standardMethods) {
      addAll(this.underriddenMethods, standardMethods);
      return this;
    }

    /** Sets whether the generated builder should be serializable. */
    public Builder setBuilderSerializable(boolean builderSerializable) {
      this.builderSerializable = builderSerializable;
      return this;
    }

    /** Sets whether the type (and hence the generated value type) is GWT serializable. */
    public Builder setGwtSerializable(boolean gwtSerializable) {
      this.gwtSerializable = gwtSerializable;
      return this;
    }

    /**
     * Returns a newly-built {@link Metadata} based on the content of the {@code Builder}.
     */
    public Metadata build() {
      checkState(builder != null, "builder not set");
      checkState(generatedBuilder != null, "generatedBuilder not set");
      checkState(type != null, "type not set");
      checkState(valueType != null, "valueType not set");
      checkState(valueType.getEnclosingElement().equals(generatedBuilder),
          "valueType not a nested class of generatedBuilder");
      checkState(partialType != null, "partialType not set");
      checkState(partialType.getEnclosingElement().equals(generatedBuilder),
          "partialType not a nested class of generatedBuilder");
      checkState(propertyEnum != null, "propertyEnum not set");
      checkState(propertyEnum.getEnclosingElement().equals(generatedBuilder),
          "propertyEnum not a nested class of generatedBuilder");
      checkState(builderSerializable != null, "builderSerializable not set");
      checkState(gwtSerializable != null, "gwtSerializable not set");
      return new Metadata(this);
    }
  }
}
