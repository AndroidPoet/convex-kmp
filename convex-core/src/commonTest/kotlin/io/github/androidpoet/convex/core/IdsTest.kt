package io.github.androidpoet.convex.core

import io.github.androidpoet.convex.core.types.DeploymentUrl
import io.github.androidpoet.convex.core.types.DocumentId
import io.github.androidpoet.convex.core.types.FunctionReference
import io.github.androidpoet.convex.core.types.StorageId
import kotlin.test.Test
import kotlin.test.assertEquals

class IdsTest {

    @Test
    fun documentId_wrapsString() {
        val id = DocumentId("abc123")
        assertEquals("abc123", id.value)
        assertEquals("abc123", id.toString())
    }

    @Test
    fun storageId_wrapsString() {
        val id = StorageId("storage_xyz")
        assertEquals("storage_xyz", id.value)
        assertEquals("storage_xyz", id.toString())
    }

    @Test
    fun functionReference_fromModuleAndExport() {
        val ref = FunctionReference.of("messages", "list")
        assertEquals("messages:list", ref.value)
    }

    @Test
    fun functionReference_preservesNestedPaths() {
        val ref = FunctionReference.of("foo/bar", "myFunc")
        assertEquals("foo/bar:myFunc", ref.value)
    }

    @Test
    fun deploymentUrl_computesSiteUrl() {
        val url = DeploymentUrl("https://my-app-123.convex.cloud")
        assertEquals("https://my-app-123.convex.site", url.siteUrl)
    }
}
