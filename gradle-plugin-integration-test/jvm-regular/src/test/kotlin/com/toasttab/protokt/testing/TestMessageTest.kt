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

package com.toasttab.protokt.testing

import kotlin.test.Test
import kotlin.test.assertEquals

class TestMessageTest {
    @Test
    fun serialization_round_trip() {
        val message = TestMessage { baz = "2007-12-03T10:15:30.00Z" }

        assertEquals(
            message,
            TestMessage.deserialize(message.serialize())
        )
    }
}
