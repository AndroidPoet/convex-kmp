import io.github.androidpoet.convex.Configuration

val isSnapshot = System.getenv("SNAPSHOT")?.toBoolean() ?: false
val publishVersion = if (isSnapshot) "${Configuration.VERSION}-SNAPSHOT" else Configuration.VERSION

afterEvaluate {
    extensions.findByType<com.vanniktech.maven.publish.MavenPublishBaseExtension>()?.apply {
        publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()
    }
}
