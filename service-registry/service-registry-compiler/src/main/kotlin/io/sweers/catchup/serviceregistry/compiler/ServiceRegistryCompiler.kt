/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sweers.catchup.serviceregistry.compiler

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreElements.isAnnotationPresent
import com.google.auto.service.AutoService
import com.squareup.javapoet.*
import com.uber.crumb.compiler.api.ConsumerMetadata
import com.uber.crumb.compiler.api.CrumbConsumerExtension
import com.uber.crumb.compiler.api.CrumbContext
import com.uber.crumb.compiler.api.CrumbExtension.IncrementalExtensionType
import com.uber.crumb.compiler.api.CrumbExtension.IncrementalExtensionType.ISOLATING
import com.uber.crumb.compiler.api.CrumbProducerExtension
import com.uber.crumb.compiler.api.ProducerMetadata
import dagger.Module
import io.sweers.catchup.serviceregistry.annotations.Meta
import io.sweers.catchup.serviceregistry.annotations.ServiceModule
import io.sweers.catchup.serviceregistry.annotations.ServiceRegistry
import java.io.IOException
import java.util.Locale
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.ERROR

@AutoService(CrumbProducerExtension::class, CrumbConsumerExtension::class)
class ServiceRegistryCompiler : CrumbProducerExtension, CrumbConsumerExtension {

  companion object {
    const val METADATA_KEY = "ServiceRegistryCompiler"
  }

  override val key = METADATA_KEY

  override fun supportedProducerAnnotations() = setOf(ServiceModule::class.java)

  override fun supportedConsumerAnnotations() = setOf(ServiceRegistry::class.java)

  override fun isProducerApplicable(
    context: CrumbContext,
    type: TypeElement,
    annotations: Collection<AnnotationMirror>
  ): Boolean {
    return isAnnotationPresent(type, ServiceModule::class.java)
  }

  override fun isConsumerApplicable(
    context: CrumbContext,
    type: TypeElement,
    annotations: Collection<AnnotationMirror>
  ): Boolean {
    return isAnnotationPresent(type, ServiceRegistry::class.java)
  }

  override fun producerIncrementalType(
    processingEnvironment: ProcessingEnvironment
  ): IncrementalExtensionType {
    return ISOLATING
  }

  override fun produce(
    context: CrumbContext,
    type: TypeElement,
    annotations: Collection<AnnotationMirror>
  ): ProducerMetadata {
    // Must be a class
    if (type.kind !== ElementKind.CLASS) {
      context.processingEnv
          .messager
          .printMessage(
              ERROR,
              "@${ServiceModule::class.java.simpleName} is only applicable on classes!",
              type)
      return emptyMap<String, String>() to emptySet()
    }

    // Check has Dagger Module annotation
    if (type.getAnnotation(Module::class.java) == null) {
      context.processingEnv
          .messager
          .printMessage(
              ERROR,
              "Must be a dagger module!",
              type)
      return emptyMap<String, String>() to emptySet()
    }

    return mapOf(METADATA_KEY to type.qualifiedName.toString()) to setOf(type)
  }

  override fun consumerIncrementalType(
    processingEnvironment: ProcessingEnvironment
  ): IncrementalExtensionType {
    return ISOLATING
  }

  override fun consume(
    context: CrumbContext,
    type: TypeElement,
    annotations: Collection<AnnotationMirror>,
    metadata: Set<ConsumerMetadata>
  ) {
    if (type.kind != ElementKind.INTERFACE) {
      context.processingEnv
              .messager
              .printMessage(ERROR,
                      "@${ServiceRegistry::class.java.simpleName} can't be applied to $type: must be a " +
                              "Kotlin interface class",
                      type)
      return
    }

    val isConsumingMeta = type.isMeta

    // List of module TypeElements by type
    val modules = metadata
        .asSequence()
        .mapNotNull { it[METADATA_KEY] }
        .distinct()
        .map(context.processingEnv.elementUtils::getTypeElement)
        .filter { it.isMeta == isConsumingMeta }
        .map { it.qualifiedName }
        .toList()
        .toTypedArray()

    val moduleAnnotation = AnnotationSpec.builder(Module::class.java)
            .addMember(
                    "includes",
                    modules.joinToString(separator = ",\n", prefix = "{\n", postfix = "\n}") { "$it.class" }
            )
        .build()

    val className = "Resolved${type.simpleName.toString().capitalize(Locale.US)}"
    try {
      // Generate the file
      @Suppress("UnstableApiUsage")
      JavaFile.builder(
              MoreElements.getPackage(type).qualifiedName.toString(),
              TypeSpec.classBuilder(className)
                      .addModifiers(Modifier.PUBLIC)
                      .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                      .addAnnotation(moduleAnnotation)
                      .addSuperinterface(TypeName.get(type.asType()))
                      .addOriginatingElement(type)
                      .build()
      )
          .build()
          .writeTo(context.processingEnv.filer)
    } catch (e: IOException) {
      context.processingEnv
          .messager
          .printMessage(
              ERROR,
              "Failed to write generated registry! ${e.message}",
              type)
    }
  }
}

private val TypeElement.isMeta: Boolean
  get() = getAnnotation(Meta::class.java) != null
