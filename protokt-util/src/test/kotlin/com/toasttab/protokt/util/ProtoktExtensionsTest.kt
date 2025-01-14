package com.toasttab.protokt.util

import com.google.common.truth.Truth.assertThat
import com.toasttab.protokt.ext.Protokt
import org.junit.jupiter.api.Test

class ProtoktExtensionsTest {
    @Test
    fun `protokt extensions class name is correct`() {
        assertThat(PROTOKT_EXTENSIONS_CLASS_NAME).isEqualTo(Protokt::class.qualifiedName)
    }
}
