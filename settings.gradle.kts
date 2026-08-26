rootProject.name = "log-insight"
include("log-insight-core", "log-insight-app")

pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		google()
	}
}

dependencyResolutionManagement {
	// Prefer centralized repository configuration; fail if subprojects declare their own
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		google()
		mavenLocal()
	}
}

include("logs")