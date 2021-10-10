/*
 * Copyright (c) 2020 Toast Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.toasttab.protokt.codegen.impl

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import com.toasttab.protokt.codegen.impl.Annotator.Context
import com.toasttab.protokt.codegen.protoc.Method
import com.toasttab.protokt.codegen.protoc.ProtocolContext
import com.toasttab.protokt.codegen.protoc.Service
import com.toasttab.protokt.codegen.template.Services.Descriptor
import com.toasttab.protokt.codegen.template.Services.Method.MethodOptions
import com.toasttab.protokt.codegen.template.Services.MethodType
import com.toasttab.protokt.codegen.template.Services.Service.MethodInfo
import com.toasttab.protokt.codegen.template.Services.Service.ReflectInfo
import io.grpc.MethodDescriptor
import io.grpc.ServiceDescriptor
import com.toasttab.protokt.codegen.template.Services.Method as MethodTemplate
import com.toasttab.protokt.codegen.template.Services.Service as ServiceTemplate

internal object ServiceAnnotator {
    fun annotateService(s: Service, ctx: Context, generateService: Boolean): TypeSpec {
        ServiceTemplate.render(
            generateService = generateService,
            generateDescriptor = generateDescriptor(ctx.desc.context),
            name = s.name,
            qualifiedName = renderQualifiedName(s, ctx),
            grpcDescriptor = renderDescriptor(s),
            methods = renderMethods(s, ctx),
            reflectInfo =
            ReflectInfo(
                s.index,
                ctx.desc.context.fileDescriptorObjectName
            )
        )

        return TypeSpec.objectBuilder(s.name + "Grpc")
            .addProperty(
                PropertySpec.builder("SERVICE_NAME", String::class)
                    .addModifiers(KModifier.CONST)
                    .initializer("\"" + renderQualifiedName(s, ctx) + "\"")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("serviceDescriptor", ServiceDescriptor::class)
                    .delegate(
                        """
                            |lazy {
                            |  ServiceDescriptor.newBuilder(SERVICE_NAME)
                            |${serviceLines(s)}
                            |}
                        """.trimMargin()
                    )
                    .build()
            )
            .addProperties(
                s.methods.map {
                    PropertySpec.builder(
                        it.name.decapitalize()+ "Method",
                        MethodDescriptor::class
                            .asTypeName()
                            .parameterizedBy(
                                TypeVariableName(it.inputType.renderName(ctx.pkg)),
                                TypeVariableName(it.outputType.renderName(ctx.pkg))
                            )
                    )
                        .delegate(
                            """
                                |lazy {
                                |  MethodDescriptor.newBuilder<${it.inputType.renderName(ctx.pkg)}, ${it.outputType.renderName(ctx.pkg)}>()
                                |    .setType(MethodDescriptor.MethodType.${methodType(it)})
                                |    .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "${it.name}"))
                                |    .setRequestMarshaller(${it.options.protokt.requestMarshaller.ifEmpty { "com.toasttab.protokt.grpc.KtMarshaller" }}(${it.inputType.renderName(ctx.pkg)}))
                                |    .setResponseMarshaller(${it.options.protokt.responseMarshaller.ifEmpty { "com.toasttab.protokt.grpc.KtMarshaller" }}(${it.outputType.renderName(ctx.pkg)}))
                                |    .build()
                                |}
                            """.trimMargin()
                        )
                        .build()
                }
            )
            .build()
    }

    private fun serviceLines(s: Service) =
        s.methods.joinToString("\n") {
            "    .addMethod(${it.name.decapitalize()}Method)"
        } + "\n    .build()"

    private fun generateDescriptor(ctx: ProtocolContext) =
        !ctx.onlyGenerateGrpc && !ctx.lite

    private fun renderQualifiedName(s: Service, ctx: Context) =
        if (ctx.pkg.default) {
            s.name
        } else {
            "${ctx.desc.packageName}.${s.name}"
        }

    private fun renderDescriptor(s: Service) =
        Descriptor.render(
            methods = s.methods.map { it.name.decapitalize() }
        )

    private fun renderMethods(s: Service, ctx: Context) =
        s.methods.map { renderMethod(it, ctx) }

    private fun renderMethod(m: Method, ctx: Context) =
        m.inputType.renderName(ctx.pkg).let { `in` ->
            m.outputType.renderName(ctx.pkg).let { out ->
                MethodInfo(
                    m.name,
                    m.name.decapitalize(),
                    MethodTemplate.render(
                        name = m.name.capitalize(),
                        type = methodType(m),
                        `in` = `in`,
                        out = out,
                        options = methodOptions(m)
                    ),
                    `in`,
                    out
                )
            }
        }

    private fun methodType(m: Method) =
        MethodType.render(method = m)

    private fun methodOptions(m: Method) =
        MethodOptions(
            m.options.protokt.requestMarshaller.ifEmpty { null },
            m.options.protokt.responseMarshaller.ifEmpty { null }
        )
}
