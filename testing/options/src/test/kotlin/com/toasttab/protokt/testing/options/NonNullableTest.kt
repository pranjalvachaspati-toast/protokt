/*
 * Copyright (c) 2019 Toast Inc.
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

package com.toasttab.protokt.testing.options

import com.google.common.truth.Truth.assertThat
import com.toasttab.protokt.testing.rt.propertyIsMarkedNullable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NonNullableTest {
    @Test
    fun `test declared nullability`() {
        assertThat(
            NonNullModel::class.propertyIsMarkedNullable("nonNullStringValue")
        ).isFalse()

        assertThat(
            NonNullModel::class.propertyIsMarkedNullable("nonNullOneof")
        ).isFalse()
    }

    @Test
    fun `detailed error when attempting to deserialize null field`() {
        val thrown = assertThrows<IllegalArgumentException> {
            NonNullModel.deserialize(
                NonNullModelMirror {
                    nonNullStringValue = null
                    nonNullOneof = NonNullModelMirror.NonNullOneof.Message("asdf")
                }.serialize()
            )
        }

        assertThat(thrown).hasMessageThat().apply {
            contains("nonNullStringValue")
            contains("was null")
            contains("(protokt.property).non_null")
        }
    }

    @Test
    fun `detailed error when attempting to deserialize null oneof`() {
        val thrown = assertThrows<IllegalArgumentException> {
            NonNullModel.deserialize(
                NonNullModelMirror {
                    nonNullStringValue = "asdf"
                    nonNullOneof = null
                }.serialize()
            )
        }

        assertThat(thrown).hasMessageThat().apply {
            contains("nonNullOneof")
            contains("was null")
            contains("(protokt.oneof).non_null")
        }
    }
}
